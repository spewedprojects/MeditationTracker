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

// WeeklyActivity.java - Bar Chart Integration
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

//import com.gratus.meditationtrakcer.utils.RoundedBarChartRenderer;


public class WeeklyActivity extends BaseActivity {

    private String selectedWeekStartDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weekly);

        // Initialize the toolbar and menu button
        setupToolbar(R.id.toolbar2, R.id.menubutton);

        // Set default or passed week start date
        selectedWeekStartDate = getIntent().getStringExtra("startDate");
        if (selectedWeekStartDate == null) {
            selectedWeekStartDate = getMondayOfCurrentWeek();
        }

        // Initialize and update chart
        updateWeeklySummary();

        // Set up navigation buttons for previous/next week
        setupNavigationButtons();
    }


    private void updateWeeklySummary() {
        MeditationLogDatabaseHelper dbHelper = new MeditationLogDatabaseHelper(this);
        ArrayList<BarEntry> weeklyEntries = dbHelper.getWeeklyMeditationDataForDateRange(selectedWeekStartDate);
        float totalHours = dbHelper.getTotalWeeklyMeditationHoursForDateRange(selectedWeekStartDate, getNextWeekStartDate(selectedWeekStartDate));

        // Update chart and total hours
        BarDataSet weeklyDataSet = new BarDataSet(weeklyEntries, "Weekly Progress");
        weeklyDataSet.setColor(Color.parseColor("#9CCC65")); // Same color as in onCreate
        weeklyDataSet.setValueTextColor(Color.parseColor("#969696"));
        weeklyDataSet.setValueTextSize(14f); // Same text size as in onCreate

        BarData weeklyData = new BarData(weeklyDataSet);
        weeklyData.setBarWidth(0.8f); // Same bar width as in onCreate

        BarChart weeklyBarChart = findViewById(R.id.weeklyBarChart);
        weeklyBarChart.setData(weeklyData);

        // Configure X-Axis
        ArrayList<String> weekLabels = new ArrayList<>();
        weekLabels.add("Mon"); weekLabels.add("Tue"); weekLabels.add("Wed"); weekLabels.add("Thu"); weekLabels.add("Fri"); weekLabels.add("Sat"); weekLabels.add("Sun");

        XAxis xAxis = weeklyBarChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(weekLabels));
        xAxis.setTextColor(Color.parseColor("#969696"));
        xAxis.setTextSize(13f); // Same text size as in WeeklyActivity
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f); // Same granularity as in onCreate

        // Configure Y-Axis
        YAxis leftAxis = weeklyBarChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setTextColor(Color.parseColor("#969696"));
        leftAxis.setTextSize(13f);
        leftAxis.setAxisMinimum(0f); // Same axis minimum as in onCreate
        weeklyBarChart.getAxisRight().setEnabled(false);

        // Removed Description Label
        weeklyBarChart.getDescription().setEnabled(false);

        // Refresh chart
        weeklyBarChart.invalidate();

        // Update total hours
        TextView weekTotalTextView = findViewById(R.id.week_total);
        weekTotalTextView.setText(String.format("Total: %.2f hours", totalHours));

        // Update week label and date range
        TextView weekLabelTextView = findViewById(R.id.displayed_week);
        TextView dateRangeTextView = findViewById(R.id.displayed_weekDates);
        weekLabelTextView.setText(getWeekNumber(selectedWeekStartDate));
        dateRangeTextView.setText(getDateRange(selectedWeekStartDate));
    }


    private String getWeekNumber(String startDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(sdf.parse(startDate));

            int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);
            return "Week #" + weekOfYear;
        } catch (Exception e) {
            e.printStackTrace();
            return "Week #";
        }
    }

    private String getDateRange(String startDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(sdf.parse(startDate));
            String start = displayFormat.format(calendar.getTime());

            calendar.add(Calendar.DAY_OF_YEAR, 6);
            String end = displayFormat.format(calendar.getTime());

            return start + " - " + end;
        } catch (Exception e) {
            e.printStackTrace();
            return "Date to date";
        }
    }


    private void setupNavigationButtons() {
        Button prevWeekButton = findViewById(R.id.previous_weekButton);
        Button nextWeekButton = findViewById(R.id.next_weekButton);

        prevWeekButton.setOnClickListener(v -> {
            selectedWeekStartDate = getAdjustedWeekStartDate(selectedWeekStartDate, -7);
            updateWeeklySummary();
        });

        nextWeekButton.setOnClickListener(v -> {
            selectedWeekStartDate = getAdjustedWeekStartDate(selectedWeekStartDate, 7);
            updateWeeklySummary();
        });
    }

    private String getMondayOfCurrentWeek() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    private String getAdjustedWeekStartDate(String currentStartDate, int daysOffset) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(sdf.parse(currentStartDate));
            calendar.add(Calendar.DAY_OF_YEAR, daysOffset);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sdf.format(calendar.getTime());
    }

    private String getNextWeekStartDate(String startDate) {
        return getAdjustedWeekStartDate(startDate, 7);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(WeeklyActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
