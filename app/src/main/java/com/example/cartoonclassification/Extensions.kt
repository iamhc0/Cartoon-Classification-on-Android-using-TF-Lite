package com.example.cartoonclassification

import android.graphics.Color
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

/**
 * Created by Hamza Chaudhary
 * Sr. Software Engineer Android
 * Created on 12 Jan,2023 14:45
 * Copyright (c) All rights reserved.
 */




fun BarChart.setChart(
    arrayList: ArrayList<BarEntry>?, xAxisValues: ArrayList<String>?
) {
    setDrawBarShadow(false)
    setFitBars(true)
    setDrawValueAboveBar(true)
    setMaxVisibleValueCount(25)
    setPinchZoom(true)
    setDrawGridBackground(true)
    val barDataSet = BarDataSet(arrayList, "Class")
    barDataSet.setColors(
        *intArrayOf(
            Color.parseColor("#03A9F4"),
            Color.parseColor("#FF9800"),
            Color.parseColor("#76FF03"),
            Color.parseColor("#E91E63"),
            Color.parseColor("#2962FF")
        )
    )
    //barDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
    val barData = BarData(barDataSet)
    barData.barWidth = 0.9f
    barData.setValueTextSize(0f)
    setBackgroundColor(Color.TRANSPARENT) //set whatever color you prefer
    setDrawGridBackground(false)
    animateY(2000)

    //Legend l =  getLegend(); // Customize the ledgends
    //l.setTextSize(10f);
    //l.setFormSize(10f);
//To set components of x axis
    val xAxis = xAxis
    xAxis.textSize = 13f
    xAxis.position = XAxis.XAxisPosition.TOP_INSIDE
    xAxis.valueFormatter = IndexAxisValueFormatter(xAxisValues)
    xAxis.setDrawGridLines(false)
    data = barData
}
