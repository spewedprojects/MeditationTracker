package com.gratus.meditationtrakcer.utils;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.TypedValue;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.buffer.BarBuffer;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.renderer.BarChartRenderer;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.ViewPortHandler;

/**
 * A renderer that draws each bar with 12 dp–rounded top corners,
 * keeping the bottom corners square so bars sit flush on the X–axis.
 *
 * We completely sidestep any inherited buffer‐initialization issues
 * by manually calling the same steps MPAndroidChart’s default renderer uses.
 */
public class RoundedBarChartRenderer extends BarChartRenderer {

    // 12 dp in pixels, computed once in the constructor
    private final float radiusPx;

    public RoundedBarChartRenderer(
            BarChart chart,
            ChartAnimator animator,
            ViewPortHandler viewPortHandler) {
        super(chart, animator, viewPortHandler);

        // Convert 12 dp → pixels, using the chart’s DisplayMetrics
        radiusPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                5f,
                chart.getResources().getDisplayMetrics()
        );
    }

    @Override
    public void drawData(Canvas c) {
        BarData barData = mChart.getBarData();
        // Loop through all BarDataSets in this chart
        for (int i = 0; i < barData.getDataSetCount(); i++) {
            IBarDataSet dataSet = barData.getDataSetByIndex(i);
            if (dataSet.isVisible()) {
                drawRoundedDataSet(c, dataSet, i);
            }
        }
    }

    /**
     * Manually initialize each BarBuffer, convert to pixels,
     * and then draw each bar with a rounded top (12 dp) + square bottom.
     */
    protected void drawRoundedDataSet(Canvas c, IBarDataSet dataSet, int index) {
        // 1) Grab the BarBuffer for this dataset index
        BarBuffer buffer = mBarBuffers[index];

        // 2) Exactly the same steps MPAndroidChart's default BarChartRenderer does:
        buffer.setPhases(
                mAnimator.getPhaseX(),
                mAnimator.getPhaseY()
        );
        buffer.setDataSet(index);
        buffer.setInverted(
                mChart.isInverted(dataSet.getAxisDependency())
        );
        buffer.setBarWidth(
                mChart.getBarData().getBarWidth()
        );

        // 3) Feed the dataset into the buffer → this allocates/fills buffer.buffer[]
        buffer.feed(dataSet);

        // 4) Transform all the raw bar‐coordinates into pixel‐space
        mChart.getTransformer(dataSet.getAxisDependency())
                .pointValuesToPixel(buffer.buffer);

        // 5) Draw each bar from the float[]: [left, top, right, bottom] for every bar
        mRenderPaint.setColor(dataSet.getColor());

        for (int j = 0; j < buffer.buffer.length; j += 4) {
            float left   = buffer.buffer[j];
            float top    = buffer.buffer[j + 1];
            float right  = buffer.buffer[j + 2];
            float bottom = buffer.buffer[j + 3];

            // Skip bars that are fully off‐screen
            if (!mViewPortHandler.isInBoundsTop(bottom))  continue;
            if (!mViewPortHandler.isInBoundsBottom(top))  break;

            // Build the full bar‐rectangle
            RectF barRect = new RectF(left, top, right, bottom);

            // 6a) Draw a fully‐rounded rectangle (radiusPx on all corners)
            //___c.drawRoundRect(barRect, radiusPx, radiusPx, mRenderPaint);

            // 6b) Overwrite the bottom‐half of that same rectangle,
            //      effectively squaring off the bottom corners.
            //
            //    ┌──────────────┐
            //    │ ⋅⋅⋅⋅⋅⋅⋅⋅⋅⋅⋅⋅ │ Top corners rounded
            //    │ ⋅⋅⋅⋅⋅⋅⋅⋅⋅⋅⋅⋅ │
            //    └──────────────┘   <-- Drawn with drawRoundRect
            //    ┌──────────────┐
            //    │              │
            //    │              │  <-- This covers from (top + radiusPx) → bottom
            //    │              │      so that bottom corners are square.
            //    └──────────────┘
            //
            //___c.drawRect(left,top + radiusPx, right, bottom, mRenderPaint);
            Path barPath = new Path();
            barPath.addRoundRect(barRect,
                    new float[]{radiusPx, radiusPx, radiusPx, radiusPx, 0f, 0f, 0f, 0f}, // top corners rounded
                    Path.Direction.CW);
            c.drawPath(barPath, mRenderPaint);

        }
    }

    @Override
    public void drawHighlighted(Canvas c, Highlight[] indices) {
        BarData barData = mChart.getBarData();

        for (Highlight high : indices) {
            IBarDataSet set = barData.getDataSetByIndex(high.getDataSetIndex());

            if (set == null || !set.isHighlightEnabled())
                continue;

            BarEntry entry = set.getEntryForXValue(high.getX(), high.getY());
            if (!isInBoundsX(entry, set)) continue;

            Transformer trans = mChart.getTransformer(set.getAxisDependency());
            mHighlightPaint.setColor(set.getHighLightColor());  // or custom theme color
            mHighlightPaint.setAlpha(100);  // semi-transparent

            final float barWidthHalf = barData.getBarWidth() / 2f;
            final float x = entry.getX();

            mBarRect.set(x - barWidthHalf, 0f, x + barWidthHalf, entry.getY());
            trans.rectToPixelPhase(mBarRect, mAnimator.getPhaseY());

            // Round top highlight only
            Path roundedHighlight = new Path();
            roundedHighlight.addRoundRect(
                    mBarRect,
                    new float[]{radiusPx, radiusPx, radiusPx, radiusPx, 0f, 0f, 0f, 0f},
                    Path.Direction.CW
            );

            c.drawPath(roundedHighlight, mHighlightPaint);
        }
    }

}
