package com.gratus.meditationtrakcer;


import android.content.SharedPreferences;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;

// WeeklyActivity.java - Bar Chart Integration
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import android.util.TypedValue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.gratus.meditationtrakcer.utils.HideZeroValueFormatter;
import com.gratus.meditationtrakcer.utils.RoundedBarChartRenderer;


public class SummaryActivity extends BaseActivity {

    // ── remembering which tab was last viewed ─────────────
    private static final String PREFS_SUMMARY   = "summary_prefs";
    private static final String KEY_LAST_VIEW   = "last_view";   // "W","M","Y"

    // ── lazy-init flags (for future memory optimisation) ──
    private boolean weekLoaded   = false;
    private boolean monthLoaded  = false;
    private boolean yearLoaded   = false;

    // swipe detector
    private GestureDetector gestureDetector;
    private static final int SWIPE_THRESHOLD   = 90;   // px
    private static final int SWIPE_VELOCITY    = 60;   // px/s

    private String selectedWeekStartDate;
    private String selectedMonthStartDate;
    private String selectedYearStartDate;

    // ── mode toggle ───────────────────────────────
    private MaterialButtonToggleGroup viewGroup;
    private MaterialButton btnWeekly, btnMonthly, btnYearly;
    private CardView weekCard, monthCard, yearCard;   // promote to fields

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        // Set default or passed week start date
        selectedWeekStartDate = getIntent().getStringExtra("startDate");
        if (selectedWeekStartDate == null) {
            selectedWeekStartDate = getMondayOfCurrentWeek();
        }

        // Set default or passed month start date
        selectedMonthStartDate = getIntent().getStringExtra("startDate");
        if (selectedMonthStartDate == null) {
            selectedMonthStartDate = getFirstDayOfCurrentMonth();
        }

        // Set default or passed year start date
        selectedYearStartDate = getIntent().getStringExtra("startDate");
        if (selectedYearStartDate == null) {
            selectedYearStartDate = getFirstDayOfCurrentYear();
        }

        // Initialize the toolbar and menu button
        setupToolbar(R.id.toolbar2, R.id.menubutton);

        // 1) grab view handles  (already done in your file) ……………………………………
        viewGroup  = findViewById(R.id.WMY_group);
        weekCard  = findViewById(R.id.cardView_week);
        monthCard = findViewById(R.id.cardView_month);
        yearCard  = findViewById(R.id.cardView_year);
        btnWeekly   = findViewById(R.id.W_Button);
        btnMonthly  = findViewById(R.id.M_Button);
        btnYearly   = findViewById(R.id.Y_Button);

        viewGroup.setSingleSelection(true);

        // 2) restore last view from SharedPreferences ………………………………………
        SharedPreferences sp = getSharedPreferences(PREFS_SUMMARY, MODE_PRIVATE);
        String last = sp.getString(KEY_LAST_VIEW, "W");
        int startId = last.equals("M") ? R.id.M_Button
                : last.equals("Y") ? R.id.Y_Button
                : R.id.W_Button;
        viewGroup.check(startId);      // will also trigger listener below
        applyTab(startId);
        refreshButtonTransparency(startId);        // <<< add here

        // 3) toggle listener → shows card, saves pref, lazy-loads chart …………………
        viewGroup.addOnButtonCheckedListener((g, id, isChecked) -> {
            if (!isChecked) return;

            weekCard.setVisibility(id == R.id.W_Button ? View.VISIBLE : View.GONE);
            monthCard.setVisibility(id == R.id.M_Button ? View.VISIBLE : View.GONE);
            yearCard.setVisibility(id == R.id.Y_Button ? View.VISIBLE : View.GONE);

            String mode = (id == R.id.M_Button) ? "M"
                    : (id == R.id.Y_Button) ? "Y"
                    : "W";
            sp.edit().putString(KEY_LAST_VIEW, mode).apply();

            switch (mode) {
                case "W": maybeLoadWeek();  break;
                case "M": maybeLoadMonth(); break;
                case "Y": maybeLoadYear();  break;
            }
            refreshButtonTransparency(id);        // <<< add here
        });

        // 4) first load whichever card is visible (lazy) ………………………………………
        if (startId == R.id.W_Button)  maybeLoadWeek();
        if (startId == R.id.M_Button)  maybeLoadMonth();
        if (startId == R.id.Y_Button)  maybeLoadYear();

        // 5) hook prev / next buttons (see helper below) ………………………………………
        setupNavigationButtons();

        // 6) enable swipe across the whole NestedScrollView ………………………………
        NestedScrollView scroll = findViewById(R.id.main_scroll); // give the NSV an id
        gestureDetector = new GestureDetector(this, new SwipeListener());
        scroll.setOnTouchListener((v,e) -> gestureDetector.onTouchEvent(e));

    }

    // put near your other helpers
    private void applyTab(int id) {
        // 1. make the right card visible
        weekCard .setVisibility(id == R.id.W_Button ? View.VISIBLE : View.GONE);
        monthCard.setVisibility(id == R.id.M_Button ? View.VISIBLE : View.GONE);
        yearCard .setVisibility(id == R.id.Y_Button ? View.VISIBLE : View.GONE);

        // 2. load its data once
        if (id == R.id.W_Button) maybeLoadWeek();
        else if (id == R.id.M_Button) maybeLoadMonth();
        else maybeLoadYear();
        refreshButtonTransparency(id);        // <<< add here
    }


    /** 100 % opaque for the checked button, 30 % opaque (≈ 70 % transparent)
     *  for the others.  */
    private void refreshButtonTransparency(int checkedId) {
        btnWeekly .setAlpha(checkedId == R.id.W_Button ? 1f : 0.3f);
        btnMonthly.setAlpha(checkedId == R.id.M_Button ? 1f : 0.3f);
        btnYearly .setAlpha(checkedId == R.id.Y_Button ? 1f : 0.3f);
    }

    private void setupNavigationButtons() {
        // Week
        findViewById(R.id.previous_weekButton).setOnClickListener(v -> {
            selectedWeekStartDate = getAdjustedWeekStartDate(selectedWeekStartDate,-7);
            updateWeeklySummary();
        });
        findViewById(R.id.next_weekButton).setOnClickListener(v -> {
            selectedWeekStartDate = getAdjustedWeekStartDate(selectedWeekStartDate, 7);
            updateWeeklySummary();
        });

        // Month
        findViewById(R.id.previous_monthButton).setOnClickListener(v -> {
            selectedMonthStartDate = getAdjustedMonthStartDate(selectedMonthStartDate,-1);
            updateMonthlySummary();
        });
        findViewById(R.id.next_monthButton).setOnClickListener(v -> {
            selectedMonthStartDate = getAdjustedMonthStartDate(selectedMonthStartDate, 1);
            updateMonthlySummary();
        });

        // Year
        findViewById(R.id.previous_yearButton).setOnClickListener(v -> {
            selectedYearStartDate = getAdjustedYearStartDate(selectedYearStartDate,-1);
            updateYearlySummary();
        });
        findViewById(R.id.next_yearButton).setOnClickListener(v -> {
            selectedYearStartDate = getAdjustedYearStartDate(selectedYearStartDate, 1);
            updateYearlySummary();
        });
    }

    // ----- WEEKLY VIEW -----

    private void updateWeeklySummary() {
        MeditationLogDatabaseHelper dbHelper = new MeditationLogDatabaseHelper(this);
        ArrayList<BarEntry> weeklyEntries = dbHelper.getWeeklyMeditationDataForDateRange(selectedWeekStartDate);
        float totalHours = dbHelper.getTotalWeeklyMeditationHoursForDateRange(selectedWeekStartDate, getNextWeekStartDate(selectedWeekStartDate));

        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnPrimarySurface, tv, true);
        // Use "com.google.android.material.R.attr..." to extract from the material components library rather than making your own resource. //
        int barColor = tv.data;

        // Update chart and total hours
        BarDataSet weeklyDataSet = new BarDataSet(weeklyEntries, "");
        weeklyDataSet.setColor(barColor); // <= uses theme
        weeklyDataSet.setValueTextColor(Color.parseColor("#969696"));
        weeklyDataSet.setValueTextSize(14f); // Same text size as in onCreate
        weeklyDataSet.setValueFormatter(new HideZeroValueFormatter());

        BarData weeklyData = new BarData(weeklyDataSet);
        weeklyData.setBarWidth(0.5f); // Same bar width as in onCreate

        BarChart weeklyBarChart = findViewById(R.id.weeklyBarChart);

        // 1) Assign the custom RoundedBarChartRenderer BEFORE invalidating
        weeklyBarChart.setRenderer(
                new RoundedBarChartRenderer(
                        weeklyBarChart,
                        weeklyBarChart.getAnimator(),
                        weeklyBarChart.getViewPortHandler()
                )
        );

        weeklyBarChart.setData(weeklyData);

        // Configure X-Axis
        ArrayList<String> weekLabels = new ArrayList<>();
        //weekLabels.add("Mon"); weekLabels.add("Tue"); weekLabels.add("Wed"); weekLabels.add("Thu"); weekLabels.add("Fri"); weekLabels.add("Sat"); weekLabels.add("Sun");
        // ------ START OF CHANGES -------
        try {
            // Parse the selectedWeekStartDate string into a LocalDate object
            java.time.format.DateTimeFormatter inputFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());
            LocalDate currentWeekStart = LocalDate.parse(selectedWeekStartDate, inputFormatter);

            // Define the desired output format for the labels (dd-E)
            java.time.format.DateTimeFormatter outputFormatter = java.time.format.DateTimeFormatter.ofPattern("dd-E", Locale.getDefault());

            // Iterate through the 7 days of the week, starting from Monday
            LocalDate mondayOfWeek = currentWeekStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            for (int i = 0; i < 7; i++) {
                LocalDate day = mondayOfWeek.plusDays(i);
                weekLabels.add(day.format(outputFormatter));
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to generic labels in case of error
            weekLabels.addAll(Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"));
        }
        // ----- END OF CHANGES -----

        XAxis xAxis = weeklyBarChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(weekLabels));
        xAxis.setTextColor(Color.parseColor("#969696"));
        xAxis.setTextSize(12f); // Same text size as in WeeklyActivity
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
    private String getMondayOfCurrentWeek() {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
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
        monthlyDataSet.setValueFormatter(new HideZeroValueFormatter());

        BarData monthlyData = new BarData(monthlyDataSet);
        monthlyData.setBarWidth(0.45f); // Same bar width as in WeeklyActivity

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


    // ----- MONTHLY VIEW -----
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


    // ----- YEARLY VIEW -----
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
        yearlyDataSet.setValueFormatter(new HideZeroValueFormatter());

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

    // Get year common for both Month and year views
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

    private void maybeLoadWeek()  { if (!weekLoaded)  { updateWeeklySummary();  weekLoaded  = true; } }
    private void maybeLoadMonth() { if (!monthLoaded) { updateMonthlySummary(); monthLoaded = true; } }
    private void maybeLoadYear()  { if (!yearLoaded)  { updateYearlySummary();  yearLoaded  = true; } }

    // Swipe gestures
    private class SwipeListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            // You must return true here to ensure the detector tracks the gesture
            return true;
        }
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
            // guard against null events
            if (e1 == null || e2 == null) return false;

            float dx = e2.getX() - e1.getX();
            float dy = e2.getY() - e1.getY();
            if (Math.abs(dx) > Math.abs(dy)
                    && Math.abs(dx) > SWIPE_THRESHOLD
                    && Math.abs(vx) > SWIPE_VELOCITY) {
                if (dx < 0) gotoNext();
                else       gotoPrev();
                return true;
            }
            return false;
        }
    }

    private void slideAnim(boolean left) {
        View card = weekCard .getVisibility()==View.VISIBLE ? weekCard
                : monthCard.getVisibility()==View.VISIBLE ? monthCard
                : yearCard;
        // run the animation only after the card has been laid out
        card.post(() -> {
            int w = card.getWidth();
            // start off‐screen
            card.setTranslationX(left ?  w : -w);
            // slide back into place
            card.animate()
                    .translationX(0)
                    .setDuration(450)
                    .start();
        });
    }
    private void gotoNext() {                          // W → M → Y → W
        int id = viewGroup.getCheckedButtonId();
        if      (id == R.id.W_Button) viewGroup.check(R.id.M_Button);
        else if (id == R.id.M_Button) viewGroup.check(R.id.Y_Button);
        else                          viewGroup.check(R.id.W_Button);
        slideAnim(true);
    }
    private void gotoPrev() {                          // reverse
        int id = viewGroup.getCheckedButtonId();
        if      (id == R.id.Y_Button) viewGroup.check(R.id.M_Button);
        else if (id == R.id.M_Button) viewGroup.check(R.id.W_Button);
        else                          viewGroup.check(R.id.Y_Button);
        slideAnim(false);
    }

}