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

// MonthlyActivity.java - Bar Chart Integration
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

public class MonthlyActivity extends BaseActivity {

    private String selectedMonthStartDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly);

        // Initialize the toolbar and menu button
        setupToolbar(R.id.toolbar2, R.id.menubutton);

        // Set default or passed month start date
        selectedMonthStartDate = getIntent().getStringExtra("startDate");
        if (selectedMonthStartDate == null) {
            selectedMonthStartDate = getFirstDayOfCurrentMonth();
        }

        // Initialize and update chart
        updateMonthlySummary();

        // Set up navigation buttons for previous/next month
        setupNavigationButtons();
    }

    private void updateMonthlySummary() {
        MeditationLogDatabaseHelper dbHelper = new MeditationLogDatabaseHelper(this);
        ArrayList<BarEntry> monthlyEntries = dbHelper.getMonthlyMeditationDataForDateRange(selectedMonthStartDate);
        float totalHours = dbHelper.getTotalMonthlyMeditationHoursForDateRange(selectedMonthStartDate, getNextMonthStartDate(selectedMonthStartDate));

        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnPrimarySurface, tv, true);
        int barColor = tv.data;

        // Update chart and total hours
        BarDataSet monthlyDataSet = new BarDataSet(monthlyEntries, "");
        monthlyDataSet.setColor(barColor); // Same color as in WeeklyActivity
        monthlyDataSet.setValueTextColor(Color.parseColor("#969696"));
        monthlyDataSet.setValueTextSize(14f); // Same text size as in WeeklyActivity

        BarData monthlyData = new BarData(monthlyDataSet);
        monthlyData.setBarWidth(0.8f); // Same bar width as in WeeklyActivity

        BarChart monthlyBarChart = findViewById(R.id.monthlyBarChart);
        // 1) Assign the custom RoundedBarChartRenderer BEFORE invalidating
        monthlyBarChart.setRenderer(
                new RoundedBarChartRenderer(
                        monthlyBarChart,
                        monthlyBarChart.getAnimator(),
                        monthlyBarChart.getViewPortHandler()
                )
        );
        monthlyBarChart.setData(monthlyData);

        // Configure X-Axis
        ArrayList<String> monthLabels = new ArrayList<>();
        for (int i = 1; i <= 5; i++) monthLabels.add("Week " + i);

        XAxis xAxis = monthlyBarChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(monthLabels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.parseColor("#969696"));
        xAxis.setTextSize(13f); // Same text size as in WeeklyActivity
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f); // Same granularity as in WeeklyActivity

        // Configure Y-Axis
        YAxis leftAxis = monthlyBarChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMinimum(0f); // Same axis minimum as in WeeklyActivity
        leftAxis.setTextColor(Color.parseColor("#969696"));
        leftAxis.setTextSize(13f);
        monthlyBarChart.getAxisRight().setEnabled(false);

        // Removed Description Label
        monthlyBarChart.getDescription().setEnabled(false);

        // Refresh chart
        monthlyBarChart.invalidate();

        // Update total hours
        TextView monthTotalTextView = findViewById(R.id.month_total);
        monthTotalTextView.setText(String.format("Total: %.2f hours", totalHours));

        // Update displayed month and year
        TextView monthLabelTextView = findViewById(R.id.displayed_month);
        monthLabelTextView.setText(getMonthYear(selectedMonthStartDate));
        TextView yearLabelTextView = findViewById(R.id.displayed_monthYear);
        yearLabelTextView.setText(getYear(selectedMonthStartDate)); // Set the year
    }

    private void setupNavigationButtons() {
        Button prevMonthButton = findViewById(R.id.previous_monthButton);
        Button nextMonthButton = findViewById(R.id.next_monthButton);

        prevMonthButton.setOnClickListener(v -> {
            selectedMonthStartDate = getAdjustedMonthStartDate(selectedMonthStartDate, -1);
            updateMonthlySummary();
        });

        nextMonthButton.setOnClickListener(v -> {
            selectedMonthStartDate = getAdjustedMonthStartDate(selectedMonthStartDate, 1);
            updateMonthlySummary();
        });
    }

    private String getFirstDayOfCurrentMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    private String getAdjustedMonthStartDate(String currentStartDate, int monthsOffset) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(sdf.parse(currentStartDate));
            calendar.add(Calendar.MONTH, monthsOffset);
            calendar.set(Calendar.DAY_OF_MONTH, 1); // Set to the first day of the adjusted month
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sdf.format(calendar.getTime());
    }

    private String getNextMonthStartDate(String startDate) {
        return getAdjustedMonthStartDate(startDate, 1);
    }

    private String getMonthYear(String startDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(sdf.parse(startDate));
            return displayFormat.format(calendar.getTime());
        } catch (Exception e) {
            e.printStackTrace();
            return "Month Year";
        }
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
        Intent intent = new Intent(MonthlyActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
