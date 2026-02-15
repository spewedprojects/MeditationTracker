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
import androidx.viewpager2.widget.ViewPager2;

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
import com.gratus.meditationtrakcer.adapters.SummaryPagerAdapter;
import com.gratus.meditationtrakcer.databasehelpers.MeditationLogDatabaseHelper;
import com.gratus.meditationtrakcer.models.MeditationReportData;
import com.gratus.meditationtrakcer.utils.MeditationChartManager;

public class SummaryActivity extends BaseActivity {

    // ── remembering which tab was last viewed ─────────────
    // private static final String PREFS_SUMMARY = "summary_prefs"; // Legacy
    private static final String KEY_LAST_VIEW = "last_view"; // "W","M","Y"

    // ── mode toggle ───────────────────────────────
    private MaterialButtonToggleGroup viewGroup;
    private MaterialButton btnWeekly, btnMonthly, btnYearly;

    public String selectedWeekStartDate, selectedMonthStartDate, selectedYearStartDate;
    private ViewPager2 viewPager;
    private SummaryPagerAdapter adapter;

    // Calculate the "Middle" of the infinite list
    // Ensure it is divisible by 3 so it maps cleanly to index 0 (Week)
    private static final int START_POSITION = Integer.MAX_VALUE / 2 - ((Integer.MAX_VALUE / 2) % 3);

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

        // Date Defaults: Initialize dates so they are not null
        if (selectedWeekStartDate == null)
            selectedWeekStartDate = getMondayOfCurrentWeek();
        if (selectedMonthStartDate == null)
            selectedMonthStartDate = getFirstDayOfCurrentMonth();
        if (selectedYearStartDate == null)
            selectedYearStartDate = getFirstDayOfCurrentYear();
        // ----------------------

        // Initialize the toolbar and menu button
        setupToolbar(R.id.toolbar2, R.id.menubutton);

        // 1) grab view handles (already done in your file) ……………………………………
        viewGroup = findViewById(R.id.WMY_group);
        btnWeekly = findViewById(R.id.W_Button);
        btnMonthly = findViewById(R.id.M_Button);
        btnYearly = findViewById(R.id.Y_Button);
        viewGroup.setSingleSelection(true);

        viewPager = findViewById(R.id.view_pager_summary);
        viewGroup = findViewById(R.id.WMY_group);

        adapter = new SummaryPagerAdapter(this);
        viewPager.setAdapter(adapter);
        // Keeps the current page + 1 page on either side in memory.
        // Anything further away is destroyed to save RAM.
        viewPager.setOffscreenPageLimit(6);

        // --- 1. RESTORE STATE CORRECTLY ---
        SharedPreferences sp = getSharedPreferences(BaseActivity.SHARED_PREFS_NAME, MODE_PRIVATE);
        String last = sp.getString(KEY_LAST_VIEW, "W");
        int targetMod = last.equals("M") ? 1 : last.equals("Y") ? 2 : 0;

        // Set the ViewPager to the middle + offset.
        // false = no smooth scroll for initial setup
        viewPager.setCurrentItem(START_POSITION + targetMod, false);

        // Sync buttons visually without triggering listeners
        syncButtons(targetMod);

        // --- 2. BUTTON CLICK FIX ---
        viewGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked)
                return;

            // Determine target type (0=Week, 1=Month, 2=Year)
            int targetType = (checkedId == R.id.M_Button) ? 1 : (checkedId == R.id.Y_Button) ? 2 : 0;

            // Calculate current type based on massive position
            int currentPos = viewPager.getCurrentItem();
            int currentType = currentPos % 3;

            if (currentType == targetType)
                return; // Already there

            // Calculate the nearest neighbor.
            // Example: We are at 1,000,000 (Week). User clicks Month (1).
            // We want 1,000,001. NOT 1.
            int newPos = currentPos + (targetType - currentType);

            viewPager.setCurrentItem(newPos, true); // Smooth scroll to neighbor

            // Save Prefs
            sp.edit().putString(KEY_LAST_VIEW, targetType == 1 ? "M" : targetType == 2 ? "Y" : "W").apply();
            refreshButtonTransparency(checkedId);
        });

        // --- 3. SWIPE FIX ---
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                // Map the massive position back to 0, 1, 2
                int type = position % 3;

                // Sync the buttons to match the swipe
                syncButtons(type);

                // Save Prefs on swipe too
                String code = type == 1 ? "M" : type == 2 ? "Y" : "W";
                sp.edit().putString(KEY_LAST_VIEW, code).apply();
            }
        });
    }

    private void syncButtons(int type) {
        int targetId = (type == 1) ? R.id.M_Button : (type == 2) ? R.id.Y_Button : R.id.W_Button;

        // CRITICAL SAFETY CHECK:
        // Only update the button if it is NOT ALREADY checked.
        // This stops the infinite loop of "Swipe -> Check Button -> Trigger Listener ->
        // Swipe Again".
        if (viewGroup.getCheckedButtonId() != targetId) {
            viewGroup.check(targetId);
        }

        refreshButtonTransparency(targetId);
    }

    /**
     * 100 % opaque for the checked button, 25 % opaque (≈ 75 % transparent)
     * for the others.
     */
    private void refreshButtonTransparency(int checkedId) {
        btnWeekly.setAlpha(checkedId == R.id.W_Button ? 1f : 0.25f);
        btnMonthly.setAlpha(checkedId == R.id.M_Button ? 1f : 0.25f);
        btnYearly.setAlpha(checkedId == R.id.Y_Button ? 1f : 0.25f);
    }

    // (13/01/26) - For reports
    public void generateAndOpenMonthReport() {
        // 1. Determine dates based on currently selected month
        // selectedMonthStartDate is already formatted "yyyy-MM-dd"
        // We need the end date of that month

        String startDate = selectedMonthStartDate;
        String endDate = getNextMonthStartDate(startDate);
        // Note: getNextMonthStartDate returns 1st of next month.
        // ReportGenerator expects inclusive end date usually, but SQL queries handle <=
        // datetime properly.
        // Actually, let's be precise. We need the actual last day.

        Calendar cal = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            cal.setTime(sdf.parse(startDate));
            String title = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.getTime());

            int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            String preciseEndDate = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, lastDay);

            // 2. Generate
            MeditationReportData data = com.gratus.meditationtrakcer.utils.ReportGenerator.generateReport(
                    this, startDate, preciseEndDate, false, title);

            // 3. Save (so it appears in history)
            com.gratus.meditationtrakcer.utils.ReportJsonHelper.saveReport(this, data);

            // NEW: Open Dialog directly
            com.gratus.meditationtrakcer.dialogfragments.ReportDetailDialogFragment
                    .newInstance(data.reportId)
                    .show(getSupportFragmentManager(), "month_report_dialog");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // (13/01/26) - For reports
    public void generateAndOpenYearReport() {
        String startDate = selectedYearStartDate; // "yyyy-01-01"

        try {
            // Get just the year for title
            String yearStr = startDate.split("-")[0];
            String title = "Year " + yearStr;
            String endDate = yearStr + "-12-31";

            // Generate
            MeditationReportData data = com.gratus.meditationtrakcer.utils.ReportGenerator.generateReport(
                    this, startDate, endDate, true, title);

            // Save
            com.gratus.meditationtrakcer.utils.ReportJsonHelper.saveReport(this, data);

            // NEW: Open Dialog directly
            com.gratus.meditationtrakcer.dialogfragments.ReportDetailDialogFragment
                    .newInstance(data.reportId)
                    .show(getSupportFragmentManager(), "year_report_dialog");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- FORMATTERS (Public so Fragment can access) ---
    public String getWeekNumber(String startDate) {
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

    // Another way to get the week number - some doubts, clarify them. Like the minimum days in first week=4. why?
//    public String getWeekNumber(String startDate) {
//        try {
//            // Parse the string to a LocalDate (Modern Java Time is safer than Calendar)
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());
//            LocalDate date = LocalDate.parse(startDate, formatter);
//
//            // Force ISO-8601 (Monday start) calculation
//            WeekFields weekFields = WeekFields.of(DayOfWeek.MONDAY, 4);
//            int weekOfYear = date.get(weekFields.weekOfWeekBasedYear());
//
//            return "Week #" + weekOfYear;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "Week #";
//        }
//    }

    public String getDateRange(String startDate) {
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

    public String getMonthYear(String startDate) {
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

    public String getYear(String startDate) {
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

    // ----- WEEKLY VIEW -----
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

    public String getNextWeekStartDate(String startDate) {
        return getAdjustedWeekStartDate(startDate, 7);
    }

    // ----- MONTHLY VIEW -----
    private String getFirstDayOfCurrentMonth() {
        return new SimpleDateFormat("yyyy-MM-01", Locale.getDefault()).format(Calendar.getInstance().getTime());
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

    public String getNextMonthStartDate(String startDate) {
        return getAdjustedMonthStartDate(startDate, 1);
    }

    // ----- YEARLY VIEW -----
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

    public String getNextYearStartDate(String startDate) {
        return getAdjustedYearStartDate(startDate, 1);
    }

    // --- NAVIGATION HELPERS ---
    public void adjustWeek(int days) {
        selectedWeekStartDate = getAdjustedDate(selectedWeekStartDate, Calendar.DAY_OF_YEAR, days);
    }

    public void adjustMonth(int months) {
        selectedMonthStartDate = getAdjustedDate(selectedMonthStartDate, Calendar.MONTH, months);
    }

    public void adjustYear(int years) {
        selectedYearStartDate = getAdjustedDate(selectedYearStartDate, Calendar.YEAR, years);
    }

    private String getAdjustedDate(String date, int field, int amount) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar c = Calendar.getInstance();
            c.setTime(sdf.parse(date));
            c.add(field, amount);
            if (field == Calendar.MONTH || field == Calendar.YEAR)
                c.set(Calendar.DAY_OF_MONTH, 1);
            return sdf.format(c.getTime());
        } catch (Exception e) {
            return date;
        }
    }

    // --- DRILL DOWN LOGIC ---
    public void drillDownToWeek(int weekOffset) {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());
            LocalDate monthStart = LocalDate.parse(selectedMonthStartDate, fmt);
            LocalDate target = monthStart.plusWeeks(weekOffset)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            selectedWeekStartDate = target.format(fmt);

            // FIX: Find the nearest "Week" page relative to where we are now. (29/01/26)
            // Do NOT jump to 0.
            int currentPos = viewPager.getCurrentItem();
            int currentMode = currentPos % 3;
            int targetMode = 0; // 0 = Week

            // Calculate offset (e.g., if at Month(1), target Week(0) -> offset is -1)
            int newPos = currentPos + (targetMode - currentMode);

            viewPager.setCurrentItem(newPos, true);

            // Force the adapter to refresh that specific page (in case it was already
            // preloaded with old data)
            adapter.notifyItemChanged(newPos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void drillDownToMonth(int monthIndex) {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());
            LocalDate yearStart = LocalDate.parse(selectedYearStartDate, fmt);
            LocalDate target = yearStart.withMonth(monthIndex + 1).withDayOfMonth(1);
            selectedMonthStartDate = target.format(fmt);

            // FIX: Find the nearest "Month" page relative to where we are now. (29/01/26)
            // Do NOT jump to 1.
            int currentPos = viewPager.getCurrentItem();
            int currentMode = currentPos % 3;
            int targetMode = 1; // 1 = Month

            // Calculate offset (e.g., if at Year(2), target Month(1) -> offset is -1)
            int newPos = currentPos + (targetMode - currentMode);

            viewPager.setCurrentItem(newPos, true);

            // Force refresh to ensure the chart reloads with the newly selected month date
            adapter.notifyItemChanged(newPos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Swipe gestures
    // --- CHART SWIPE HELPERS ---
    public void disableSwipe() {
        viewPager.setUserInputEnabled(false);
    }

    public void enableSwipe() {
        viewPager.setUserInputEnabled(true);
    }

}