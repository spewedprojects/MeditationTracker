package com.gratus.meditationtrakcer;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

// YearyActivity.java - Bar Chart Integration
import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.renderer.BarChartRenderer;
import com.github.mikephil.charting.utils.ViewPortHandler;
import android.util.TypedValue;

import java.util.ArrayList;
import java.util.Locale;

import com.gratus.meditationtrakcer.utils.RoundedBarChartRenderer;

public class YearlyActivity extends BaseActivity {
    private String selectedYearStartDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yearly);

        // Initialize the toolbar and menu button
        setupToolbar(R.id.toolbar2, R.id.menubutton);

        // Set default or passed year start date
        selectedYearStartDate = getIntent().getStringExtra("startDate");
        if (selectedYearStartDate == null) {
            selectedYearStartDate = getFirstDayOfCurrentYear();
        }

        // Initialize and update chart
        updateYearlySummary();

        // Set up navigation buttons for previous/next year
        setupNavigationButtons();
    }

    private void updateYearlySummary() {
        MeditationLogDatabaseHelper dbHelper = new MeditationLogDatabaseHelper(this);
        ArrayList<BarEntry> yearlyEntries = dbHelper.getYearlyMeditationDataForDateRange(selectedYearStartDate);
        float totalHours = dbHelper.getTotalYearlyMeditationHoursForDateRange(selectedYearStartDate, getNextYearStartDate(selectedYearStartDate));

        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnPrimarySurface, tv, true);
        int barColor = tv.data;

        // Update chart and total hours
        BarDataSet yearlyDataSet = new BarDataSet(yearlyEntries, "");
        yearlyDataSet.setColor(barColor); // Consistent color
        yearlyDataSet.setValueTextColor(Color.parseColor("#969696"));
        yearlyDataSet.setValueTextSize(14f); // Consistent text size

        BarData yearlyData = new BarData(yearlyDataSet);
        yearlyData.setBarWidth(0.8f); // Consistent bar width

        BarChart yearlyBarChart = findViewById(R.id.yearlyBarChart);
        // 1) Assign the custom RoundedBarChartRenderer BEFORE invalidating
        yearlyBarChart.setRenderer(
                new RoundedBarChartRenderer(
                        yearlyBarChart,
                        yearlyBarChart.getAnimator(),
                        yearlyBarChart.getViewPortHandler()
                )
        );

        yearlyBarChart.setData(yearlyData);

        // Configure X-Axis
        ArrayList<String> yearLabels = new ArrayList<>();
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (String month : months) yearLabels.add(month);

        XAxis xAxis = yearlyBarChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(yearLabels));
        xAxis.setTextColor(Color.parseColor("#969696"));
        xAxis.setTextSize(13f); // Same text size as in WeeklyActivity
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f); // Same granularity

        // Configure Y-Axis
        YAxis leftAxis = yearlyBarChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setTextColor(Color.parseColor("#969696"));
        leftAxis.setTextSize(13f);
        leftAxis.setAxisMinimum(0f); // Same axis minimum
        yearlyBarChart.getAxisRight().setEnabled(false);

        // Removed Description Label
        yearlyBarChart.getDescription().setEnabled(false);

        // Refresh chart
        yearlyBarChart.invalidate();

        // Update total hours
        TextView yearTotalTextView = findViewById(R.id.year_total);
        yearTotalTextView.setText(String.format("Total: %.2f hours", totalHours));

        // Update displayed year
        TextView yearLabelTextView = findViewById(R.id.displayed_year);
        yearLabelTextView.setText(getYear(selectedYearStartDate)); // Display only the year
    }

    private void setupNavigationButtons() {
        Button prevYearButton = findViewById(R.id.previous_yearButton);
        Button nextYearButton = findViewById(R.id.next_yearButton);

        prevYearButton.setOnClickListener(v -> {
            selectedYearStartDate = getAdjustedYearStartDate(selectedYearStartDate, -1);
            updateYearlySummary();
        });

        nextYearButton.setOnClickListener(v -> {
            selectedYearStartDate = getAdjustedYearStartDate(selectedYearStartDate, 1);
            updateYearlySummary();
        });
    }

    private String getFirstDayOfCurrentYear() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_YEAR, 1);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    private String getAdjustedYearStartDate(String currentStartDate, int yearsOffset) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(sdf.parse(currentStartDate));
            calendar.add(Calendar.YEAR, yearsOffset);
            calendar.set(Calendar.DAY_OF_YEAR, 1); // Set to the first day of the adjusted year
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sdf.format(calendar.getTime());
    }

    private String getNextYearStartDate(String startDate) {
        return getAdjustedYearStartDate(startDate, 1);
    }

    private String getYear(String startDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault()); // Year format
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(sdf.parse(startDate)); // Parse the input date
            return yearFormat.format(calendar.getTime()); // Extract the year
        } catch (Exception e) {
            e.printStackTrace();
            return "Year"; // Default fallback in case of errors
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(YearlyActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
