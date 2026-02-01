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
    private boolean isYAxisVisible = true; // Default to true

    public MeditationChartManager(Context context, BarChart chart, Typeface customFont) {
        this.context = context;
        this.chart = chart;
        this.customFont = customFont;
        this.barColor = resolveThemeColor();
    }

    /**
     * Set whether the Left Y-Axis should be visible.  (31/01/26)
     */
    public void setYAxisEnabled(boolean visible) {
        this.isYAxisVisible = visible;
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

        // Note: If you want "0" to appear as a number on top of empty bars,
        // remove this specific line. Keeping it hides the number "0".
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
        // CRITICAL: Always keep enabled so the scale (0 to Max) is respected.
        // If set to false, the chart auto-scales to the data range (floating bars).
        leftAxis.setEnabled(true);
        leftAxis.setAxisMinimum(0f); // Forces the bars to sit on the floor (0)

        if (isYAxisVisible) {
            // Normal mode: Show labels
            leftAxis.setDrawLabels(true);
            leftAxis.setDrawGridLines(false); // Preference: clean look
            leftAxis.setDrawAxisLine(true);
            leftAxis.setTextColor(Color.parseColor("#969696"));
            leftAxis.setTypeface(customFont);
            leftAxis.setTextSize(13f);
        } else {
            // "Hidden" mode: Hide visuals but keep the scale logic
            leftAxis.setDrawLabels(false);
            leftAxis.setDrawAxisLine(false);
            leftAxis.setDrawGridLines(false);
            leftAxis.setDrawZeroLine(true); // Optional: Draws a line at 0 for grounding
            leftAxis.setZeroLineColor(Color.parseColor("#969696"));
        }
        chart.getAxisRight().setEnabled(false);

        // 6. General Chart Cleanup
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false); // Often cleaner without legend for single datasets
        //chart.setTouchEnabled(false); // Optional: Disable touch for static report cards if desired

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