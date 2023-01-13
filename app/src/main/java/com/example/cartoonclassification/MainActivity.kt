package com.example.cartoonclassification

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import com.example.cartoonclassification.databinding.ActivityMainBinding
import com.github.mikephil.charting.data.BarEntry
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null
    private var tflite: Interpreter? = null
    private var inputImageBuffer: TensorImage? = null
    private var imageSizeX = 0
    private var imageSizeY = 0
    private var outputProbabilityBuffer: TensorBuffer? = null
    private var probabilityProcessor: TensorProcessor? = null
    private var bitmap: Bitmap? = null
    private var labels: List<String>? = null
    private var imageuri: Uri? = null


    companion object {
        const val IMAGE_MEAN = 0.0f
        const val IMAGE_STD = 1.0f
        const val PROBABILITY_MEAN = 0.0f
        const val PROBABILITY_STD = 255.0f
    }

    // TODO Insect

       /* private val ModelName = "Insect.tflite"
        private val LabelName = "InsectLabels.txt"*/

    // TODO Cartoon

   /* private val ModelName = "cartoon_model.tflite"
    private val LabelName = "cartoon_labels.txt"*/

    // TODO Garbage

      private val ModelName = "GarbageModel.tflite"
      private val LabelName = "GarbageLabels.txt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        binding?.init()
    }

    private fun ActivityMainBinding.init() {

        try {
            tflite = Interpreter(loadmodelfile(this@MainActivity))
        } catch (e: Exception) {
            e.printStackTrace()
        }


        image.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), 12)
        }

        classify.setOnClickListener {
            val imageTensorIndex = 0
            val imageShape =
                tflite!!.getInputTensor(imageTensorIndex).shape() // {1, height, width, 3}
            imageSizeY = imageShape[1]
            imageSizeX = imageShape[2]
            val imageDataType = tflite!!.getInputTensor(imageTensorIndex).dataType()
            val probabilityTensorIndex = 0
            val probabilityShape =
                tflite!!.getOutputTensor(probabilityTensorIndex).shape() // {1, NUM_CLASSES}
            val probabilityDataType = tflite!!.getOutputTensor(probabilityTensorIndex).dataType()
            inputImageBuffer = TensorImage(imageDataType)
            outputProbabilityBuffer =
                TensorBuffer.createFixedSize(probabilityShape, probabilityDataType)
            probabilityProcessor = TensorProcessor.Builder().add(postprocessNormalizeOp).build()
            inputImageBuffer = bitmap?.loadImage()

            inputImageBuffer?.run {
                outputProbabilityBuffer?.let {
                    tflite?.run(buffer, it.buffer.rewind())
                    probabilityProcessor?.showResult()
                }

            }

        }
    }

    private fun Bitmap.loadImage(): TensorImage {
        // Loads bitmap into a TensorImage.
        inputImageBuffer?.load(this)

        // Creates processor for the TensorImage.
        val cropSize = Math.max(width, height)
        // TODO(b/143564309): Fuse ops inside ImageProcessor.
        val imageProcessor = ImageProcessor.Builder().add(ResizeWithCropOrPadOp(cropSize, cropSize))
            .add(ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR)).add(
                preprocessNormalizeOp
            ).build()
        return imageProcessor.process(inputImageBuffer)
    }

    @Throws(IOException::class)
    private fun loadmodelfile(activity: Activity): MappedByteBuffer {
        val fileDescriptor = activity.assets.openFd(ModelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startoffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startoffset, declaredLength)
    }

    private val preprocessNormalizeOp: TensorOperator
        get() = NormalizeOp(IMAGE_MEAN, IMAGE_STD)
    private val postprocessNormalizeOp: TensorOperator
        get() = NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD)


    private fun TensorProcessor.showResult() {
        try {
            labels = FileUtil.loadLabels(this@MainActivity, LabelName)
        } catch (e: Exception) {
            e.printStackTrace()
        }


        labels?.let {
            val labeledProbability = TensorLabel(
                it, process(outputProbabilityBuffer)
            ).mapWithFloatValue

            val result =
                labeledProbability.toList().sortedByDescending { (_, value) -> value }.toMap()

            val label: Array<String>
            val label_probability: FloatArray
            if (result.size > 5) {
                label = result.keys.toTypedArray()
                label_probability = result.values.take(5).toFloatArray()
            } else {
                label = labeledProbability.keys.toTypedArray()
                label_probability = labeledProbability.values.toFloatArray()
            }


            // Previous code is used - i did not add this one line
            val maxValueInMap = Collections.max(labeledProbability.values)


            for (value in 1..5) {
                // Previous code is used - i did not add this one line
                //if (entry.getValue()==maxValueInMap) {


                binding?.chart?.xAxis?.setDrawGridLines(false)
                binding?.chart?.axisLeft?.setDrawGridLines(false)
                // PREPARING THE ARRAY LIST OF BAR ENTRIES
                val barEntries = ArrayList<BarEntry>()
                for (i in label_probability.indices) {
                    barEntries.add(BarEntry(i.toFloat(), label_probability[i] * 100))
                }

                // TO ADD THE VALUES IN X-AXIS
                val xAxisName = ArrayList<String>()
                for (i in label.indices) {
                    xAxisName.add(label[i])
                }
                binding?.chart?.setChart(barEntries, xAxisName)
                binding?.predictions?.text = "Predictions:"


                //           }
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 12 && resultCode == RESULT_OK && data != null) {
            imageuri = data.data
            try {
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageuri)
                binding?.image?.setImageBitmap(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


}