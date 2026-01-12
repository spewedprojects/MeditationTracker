package com.gratus.meditationtrakcer.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.listener.OnChartGestureListener;

import java.util.ArrayList;
import java.util.List;

public class MeditationChartManager {

    private final Context context;
    private final BarChart chart;
    private final Typeface customFont;
    private final int barColor;

    public MeditationChartManager(Context context, BarChart chart, Typeface customFont) {
        this.context = context;
        this.chart = chart;
        this.customFont = customFont;
        this.barColor = resolveThemeColor();
    }

    /**
     * Updates the chart with new data, labels, and specific bar width.
     */
    public void displayChart(ArrayList<BarEntry> entries, List<String> labels, float barWidth) {
        // 1. Setup Data Set
        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(barColor);
        dataSet.setValueTextColor(Color.parseColor("#969696"));
        dataSet.setValueTextSize(14f);
        dataSet.setValueTypeface(customFont);
        dataSet.setValueFormatter(new HideZeroValueFormatter());

        // 2. Setup Bar Data
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(barWidth);

        // 3. Set Custom Renderer (Rounded Corners)
        chart.setRenderer(new RoundedBarChartRenderer(
                chart,
                chart.getAnimator(),
                chart.getViewPortHandler()
        ));

        chart.setData(barData);

        // Adds ~10-15 units of extra padding at the bottom so labels aren't cut off
        chart.setExtraBottomOffset(7f);
        // This happens due to usage of custom fonts

        // 4. Configure X-Axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setTextColor(Color.parseColor("#969696"));
        xAxis.setTextSize(12f);
        xAxis.setTypeface(customFont);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        // 5. Configure Y-Axis
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setTextColor(Color.parseColor("#969696"));
        leftAxis.setTypeface(customFont);
        leftAxis.setTextSize(13f);
        leftAxis.setAxisMinimum(0f);
        chart.getAxisRight().setEnabled(false);

        // 6. General Chart Cleanup
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false); // Often cleaner without legend for single datasets

        // 7. Refresh
        chart.invalidate();
    }

    /**
     * Optional: Helper to attach drill-down listeners easily
     */
    public void setDrillDownListener(OnChartGestureListener listener) {
        chart.setOnChartGestureListener(listener);
    }

    private int resolveThemeColor() {
        TypedValue tv = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnPrimarySurface, tv, true);
        return tv.data;
    }
}