package com.gratus.meditationtrakcer;


import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

// WeeklyActivity.java - Bar Chart Integration
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarEntry;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.gratus.meditationtrakcer.databasehelpers.MeditationLogDatabaseHelper;
import com.gratus.meditationtrakcer.utils.MeditationChartManager;


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

    private Typeface myCustomFont; //  <-- Add this

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_summary);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

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
        // Load the custom font
        myCustomFont = getResources().getFont(R.font.atkinsonhyperlegiblenext_regular); // <-- Add this

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

    /** 100 % opaque for the checked button, 20 % opaque (≈ 70 % transparent)
     *  for the others.  */
    private void refreshButtonTransparency(int checkedId) {
        btnWeekly .setAlpha(checkedId == R.id.W_Button ? 1f : 0.2f);
        btnMonthly.setAlpha(checkedId == R.id.M_Button ? 1f : 0.2f);
        btnYearly .setAlpha(checkedId == R.id.Y_Button ? 1f : 0.2f);
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

    /** ── HELPER CLASS FOR LONG PRESS ─────────────────────────────
    This abstract class handles the gesture detection logic
    and exposes a simple onDrillDown method for the Activity to use.**/
    abstract class DrillDownListener implements OnChartGestureListener {
        private final BarChart chart;

        public DrillDownListener(BarChart chart) {
            this.chart = chart;
        }

        // We will implement this in the anonymous classes above
        public abstract void onDrillDown(float xIndex);

        @Override
        public void onChartLongPressed(MotionEvent me) {
            // Get the Highlight at the touch point
            Highlight h = chart.getHighlightByTouchPoint(me.getX(), me.getY());

            if (h != null) {
                // Perform the drill down using the X index of the bar
                onDrillDown(h.getX());
            }
        }

        @Override public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {}
        @Override public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {}
        @Override public void onChartDoubleTapped(MotionEvent me) {}
        @Override public void onChartSingleTapped(MotionEvent me) {}
        @Override public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {}
        @Override public void onChartScale(MotionEvent me, float scaleX, float scaleY) {}
        @Override public void onChartTranslate(MotionEvent me, float dX, float dY) {}
    }


    // ----- WEEKLY VIEW -----
    private void updateWeeklySummary() {
        MeditationLogDatabaseHelper dbHelper = new MeditationLogDatabaseHelper(this);
        ArrayList<BarEntry> weeklyEntries = dbHelper.getWeeklyMeditationDataForDateRange(selectedWeekStartDate);
        float totalHours = dbHelper.getTotalWeeklyMeditationHoursForDateRange(selectedWeekStartDate, getNextWeekStartDate(selectedWeekStartDate));

        // Generate Labels (Logic remains here as it's specific to the view)
        ArrayList<String> weekLabels = new ArrayList<>();
        try {
            java.time.format.DateTimeFormatter inputFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());
            LocalDate currentWeekStart = LocalDate.parse(selectedWeekStartDate, inputFormatter);
            java.time.format.DateTimeFormatter outputFormatter = java.time.format.DateTimeFormatter.ofPattern("dd-E", Locale.getDefault());

            LocalDate mondayOfWeek = currentWeekStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            for (int i = 0; i < 7; i++) {
                weekLabels.add(mondayOfWeek.plusDays(i).format(outputFormatter));
            }
        } catch (Exception e) {
            e.printStackTrace();
            weekLabels.addAll(Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"));
        }

        // --- NEW CODE: Use the Manager ---
        BarChart weeklyBarChart = findViewById(R.id.weeklyBarChart);
        MeditationChartManager chartManager = new MeditationChartManager(this, weeklyBarChart, myCustomFont);

        // Setup Chart (Pass data, labels, and specific bar width 0.5f)
        chartManager.displayChart(weeklyEntries, weekLabels, 0.5f);
        // --------------------------------

        // Update Text Views
        TextView weekTotalTextView = findViewById(R.id.week_total);
        weekTotalTextView.setText(String.format("Total: %.2f hours", totalHours));

        ((TextView) findViewById(R.id.displayed_week)).setText(getWeekNumber(selectedWeekStartDate));
        ((TextView) findViewById(R.id.displayed_weekDates)).setText(getDateRange(selectedWeekStartDate));
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


    // ----- MONTHLY VIEW -----
    private void updateMonthlySummary() {
        MeditationLogDatabaseHelper dbHelper = new MeditationLogDatabaseHelper(this);
        ArrayList<BarEntry> monthlyEntries = dbHelper.getMonthlyMeditationDataForDateRange(selectedMonthStartDate);
        float totalHours = dbHelper.getTotalMonthlyMeditationHoursForDateRange(selectedMonthStartDate, getNextMonthStartDate(selectedMonthStartDate));

        // Generate Labels
        ArrayList<String> monthLabels = new ArrayList<>();
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());
            LocalDate monthStart = LocalDate.parse(selectedMonthStartDate, formatter);
            WeekFields weekFields = WeekFields.of(Locale.getDefault());
            for (int i = 0; i < 5; i++) {
                int weekNum = monthStart.plusWeeks(i).get(weekFields.weekOfWeekBasedYear());
                monthLabels.add("Week #" + weekNum);
            }
        } catch (Exception e) {
            for (int i = 1; i <= 5; i++) monthLabels.add("Week " + i);
        }

        // --- NEW CODE: Use the Manager ---
        BarChart monthlyBarChart = findViewById(R.id.monthlyBarChart);
        MeditationChartManager chartManager = new MeditationChartManager(this, monthlyBarChart, myCustomFont);

        // Setup Chart (Width 0.45f)
        chartManager.displayChart(monthlyEntries, monthLabels, 0.45f);

            // Attach Drill Down Listener
            chartManager.setDrillDownListener(new DrillDownListener(monthlyBarChart) {
                @Override
                public void onDrillDown(float xIndex) {
                // ... (Existing Drill Down Logic) ...
                // e.g. Calculate target week, set selectedWeekStartDate, check(R.id.W_Button)
                try {
                    // xIndex corresponds to the bar index (0, 1, 2, 3, 4)
                    int weekOffset = (int) xIndex;

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());
                    LocalDate monthStart = LocalDate.parse(selectedMonthStartDate, formatter);

                    // Calculate start date of the specific week tapped
                    // Ensure we align to the Monday of that specific week
                    LocalDate targetWeekDate = monthStart.plusWeeks(weekOffset)
                            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

                    selectedWeekStartDate = targetWeekDate.format(formatter);

                    // FIX: Force reload of Week view so it updates to the new date
                    weekLoaded = false;

                    // Switch tab to Week
                    viewGroup.check(R.id.W_Button);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        // --------------------------------

        ((TextView) findViewById(R.id.month_total)).setText(String.format("Total: %.2f hours", totalHours));
        ((TextView) findViewById(R.id.displayed_month)).setText(getMonthYear(selectedMonthStartDate));
        ((TextView) findViewById(R.id.displayed_monthYear)).setText(getYear(selectedMonthStartDate));
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


    // ----- YEARLY VIEW -----
    private void updateYearlySummary() {
        MeditationLogDatabaseHelper dbHelper = new MeditationLogDatabaseHelper(this);
        ArrayList<BarEntry> yearlyEntries = dbHelper.getYearlyMeditationDataForDateRange(selectedYearStartDate);
        float totalHours = dbHelper.getTotalYearlyMeditationHoursForDateRange(selectedYearStartDate, getNextYearStartDate(selectedYearStartDate));

        // Generate Labels
        ArrayList<String> yearLabels = new ArrayList<>(Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"));

        // --- NEW CODE: Use the Manager ---
        BarChart yearlyBarChart = findViewById(R.id.yearlyBarChart);
        MeditationChartManager chartManager = new MeditationChartManager(this, yearlyBarChart, myCustomFont);

        // Setup Chart (Width 0.8f)
        chartManager.displayChart(yearlyEntries, yearLabels, 0.8f);

        // Attach Drill Down Listener
        chartManager.setDrillDownListener(new DrillDownListener(yearlyBarChart) {
            @Override
            public void onDrillDown(float xIndex) {
                // ... (Existing Drill Down Logic) ...
                // e.g. Calculate target month, set selectedMonthStartDate, check(R.id.M_Button)
                try {
                    // xIndex corresponds to Month index (0=Jan, 1=Feb ...)
                    int monthIndex = (int) xIndex;
                    if (monthIndex < 0 || monthIndex > 11) return;

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());
                    LocalDate yearStart = LocalDate.parse(selectedYearStartDate, formatter);

                    // Calculate start date of the specific month tapped
                    LocalDate targetMonthDate = yearStart.withMonth(monthIndex + 1).withDayOfMonth(1);

                    selectedMonthStartDate = targetMonthDate.format(formatter);

                    // FIX: Force reload of Month view so it updates to the new date
                    monthLoaded = false;

                    // Switch tab to Month
                    viewGroup.check(R.id.M_Button);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        // --------------------------------

        ((TextView) findViewById(R.id.year_total)).setText(String.format("Total: %.2f hours", totalHours));
        ((TextView) findViewById(R.id.displayed_year)).setText(getYear(selectedYearStartDate));
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

    // This is the new method that does all the work. It prepares the cards, runs the simultaneous animations, and cleans up afterward.
    private void animateCardSwipe(CardView fromCard, CardView toCard, int toId, boolean isNext) {
        // Update button highlighting immediately for better responsiveness
        refreshButtonTransparency(toId);

        // Use post() to ensure the fromCard has been measured and has a width.
        fromCard.post(() -> {
            // === START OF CHANGES ===

            // A. Get the margin values in pixels to create a gap between cards.
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) fromCard.getLayoutParams();
            // The total gap is the end margin of the outgoing card + the start margin of the incoming one.
            int gap = params.getMarginEnd() + params.getMarginStart();

            // B. Prepare cards for animation
            int width = fromCard.getWidth();
            toCard.setVisibility(View.VISIBLE);

            // Set the starting positions, accounting for the width AND the gap.
            fromCard.setTranslationX(0);
            toCard.setTranslationX(isNext ? (width + gap) : -(width + gap));

            // C. Animate the 'from' card moving off-screen, including the gap distance.
            fromCard.animate()
                    .translationX(isNext ? -(width + gap) : (width + gap))
                    .setDuration(450)
                    .start();

            // === END OF CHANGES ===

            // C. Animate the 'to' card moving into the center
            toCard.animate()
                    .translationX(0)
                    .setDuration(450)
                    .withEndAction(() -> {
                        // D. Cleanup after animation completes
                        fromCard.setVisibility(View.GONE);
                        fromCard.setTranslationX(0); // Reset position for next time
                        toCard.setTranslationX(0);   // Reset position for next time

                        // E. Officially update the toggle group's state.
                        // This triggers the listener to save the preference.
                        viewGroup.check(toId);
                    })
                    .start();
        });
    }

    private void gotoNext() {
        // W → M → Y → W
        // 1. Identify which cards are 'from' and 'to'
        int fromId = viewGroup.getCheckedButtonId();
        int toId;
        CardView fromCard, toCard;

        if (fromId == R.id.W_Button) {
            toId = R.id.M_Button;
            fromCard = weekCard;
            toCard = monthCard;
            maybeLoadMonth(); // 2. Pre-load data for the incoming card
        } else if (fromId == R.id.M_Button) {
            toId = R.id.Y_Button;
            fromCard = monthCard;
            toCard = yearCard;
            maybeLoadYear();
        } else { // fromId is R.id.Y_Button
            toId = R.id.W_Button;
            fromCard = yearCard;
            toCard = weekCard;
            maybeLoadWeek();
        }

        // 3. Perform the animation
        animateCardSwipe(fromCard, toCard, toId, true); // true means isNext (swiping left)
    }
    private void gotoPrev() {
        // reverse
        // 1. Identify which cards are 'from' and 'to'
        int fromId = viewGroup.getCheckedButtonId();
        int toId;
        CardView fromCard, toCard;

        if (fromId == R.id.Y_Button) {
            toId = R.id.M_Button;
            fromCard = yearCard;
            toCard = monthCard;
            maybeLoadMonth();
        } else if (fromId == R.id.M_Button) {
            toId = R.id.W_Button;
            fromCard = monthCard;
            toCard = weekCard;
            maybeLoadWeek();
        } else { // fromId is R.id.W_Button
            toId = R.id.Y_Button;
            fromCard = weekCard;
            toCard = yearCard;
            maybeLoadYear();
        }

        // 3. Perform the animation
        animateCardSwipe(fromCard, toCard, toId, false); // false means isPrev (swiping right)
    }

}