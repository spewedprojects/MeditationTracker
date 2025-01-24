package com.gratus.meditationtrakcer.utils;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Paint;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.renderer.BarChartRenderer;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.utils.ViewPortHandler;

public class RoundedBarChartRenderer extends BarChartRenderer {

    public RoundedBarChartRenderer(BarChart chart, ChartAnimator animator, ViewPortHandler viewPortHandler) {
        super(chart, animator, viewPortHandler);
        mRenderPaint.setStyle(Paint.Style.FILL);
        mRenderPaint.setColor(0xFF3F51B5);  // Default bar color (blue)
    }

    // Implement your custom rendering logic here
    @Override
    protected void drawDataSet(Canvas c, IBarDataSet dataSet, int index) {
        float barWidth = 15f;  // Increased bar width for better visibility
        for (int i = 0; i < dataSet.getEntryCount(); i++) {
            BarEntry entry = dataSet.getEntryForIndex(i);
            float left = entry.getX() - barWidth / 2;
            float right = entry.getX() + barWidth / 2;
            float bottom = mViewPortHandler.contentBottom();  // Corrected bottom coordinate
            // float top = mViewPortHandler.contentTop() + (bottom - mViewPortHandler.contentTop()) * (1 - entry.getY() / ((BarChart) mChart).getAxisLeft().getAxisMaximum());
            float top = bottom - (entry.getY() / ((BarChart) mChart).getAxisLeft().getAxisMaximum()) * (bottom - mViewPortHandler.contentTop());

            RectF barRect = new RectF(left, top, right, bottom);
            c.drawRoundRect(barRect, 6, 6, mRenderPaint);
        }
    }
}
