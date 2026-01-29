package com.gratus.meditationtrakcer;

import android.graphics.Typeface;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.highlight.Highlight;
import com.gratus.meditationtrakcer.databasehelpers.MeditationLogDatabaseHelper;
import com.gratus.meditationtrakcer.utils.MeditationChartManager;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SummaryFragment extends Fragment {

    private static final String ARG_MODE = "mode";
    public static final int MODE_WEEK = 0;
    public static final int MODE_MONTH = 1;
    public static final int MODE_YEAR = 2;

    private int currentMode;
    private Typeface myCustomFont;

    // Functional Interface for the specific drill-down action
    private interface DrillDownAction {
        void execute(float xIndex);
    }

    // Executor for background tasks
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Helper to get parent activity safely
    private SummaryActivity getParent() {
        return (SummaryActivity) getActivity();
    }

    public static SummaryFragment newInstance(int mode) {
        SummaryFragment fragment = new SummaryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MODE, mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentMode = getArguments().getInt(ARG_MODE);
        }
        myCustomFont = getResources().getFont(R.font.atkinsonhyperlegiblenext_regular);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_summary_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUI(view);

        // FIX: Load data immediately when the view is created in the background buffer.
        // This ensures the graph is ready BEFORE you swipe to it.
        refreshData();
    }

    // Called when the tab becomes visible to refresh data
    @Override
    public void onResume() {
        super.onResume();
        //refreshData();
    }

    public void refreshData() {
        if (getView() == null) return;
        if (currentMode == MODE_WEEK) updateWeeklySummary(getView());
        else if (currentMode == MODE_MONTH) updateMonthlySummary(getView());
        else if (currentMode == MODE_YEAR) updateYearlySummary(getView());
    }

    private void setupUI(View view) {
        TextView title = view.findViewById(R.id.page_title);
        TextView desc = view.findViewById(R.id.page_desc);
        ImageButton reportBtn = view.findViewById(R.id.report_button);

        // Setup Buttons based on Mode
        if (currentMode == MODE_WEEK) {
            title.setText(R.string.menu_weekly);
            desc.setText(R.string.week_summ_desc);
            reportBtn.setVisibility(View.GONE);

            view.findViewById(R.id.btn_prev).setOnClickListener(v -> {
                getParent().adjustWeek(-7);
                refreshData();
            });
            view.findViewById(R.id.btn_next).setOnClickListener(v -> {
                getParent().adjustWeek(7);
                refreshData();
            });

        } else if (currentMode == MODE_MONTH) {
            title.setText(R.string.menu_monthly);
            desc.setText(R.string.month_summ_desc);
            reportBtn.setVisibility(View.VISIBLE);
            reportBtn.setOnClickListener(v -> getParent().generateAndOpenMonthReport());

            view.findViewById(R.id.btn_prev).setOnClickListener(v -> {
                getParent().adjustMonth(-1);
                refreshData();
            });
            view.findViewById(R.id.btn_next).setOnClickListener(v -> {
                getParent().adjustMonth(1);
                refreshData();
            });

        } else { // YEAR
            title.setText(R.string.menu_yearly);
            desc.setText(R.string.year_summ_desc);
            reportBtn.setVisibility(View.VISIBLE);
            reportBtn.setOnClickListener(v -> getParent().generateAndOpenYearReport());

            view.findViewById(R.id.btn_prev).setOnClickListener(v -> {
                getParent().adjustYear(-1);
                refreshData();
            });
            view.findViewById(R.id.btn_next).setOnClickListener(v -> {
                getParent().adjustYear(1);
                refreshData();
            });
        }
    }

    // --- REUSABLE DRILL DOWN HELPER ---
    private void setupDrillDown(BarChart chart, DrillDownAction action) {
        MeditationChartManager manager = new MeditationChartManager(getContext(), chart, myCustomFont);
        manager.setDrillDownListener(new OnChartGestureListener() {
            @Override
            public void onChartLongPressed(MotionEvent me) {
                Highlight h = chart.getHighlightByTouchPoint(me.getX(), me.getY());
                if (h != null) {
                    // Execute the specific action passed via lambda
                    action.execute(h.getX());
                }
            }

            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
                // Prevent ViewPager from swiping while interacting with Chart
                getParent().disableSwipe();
            }

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
                // Re-enable ViewPager swipe
                getParent().enableSwipe();
            }

            @Override public void onChartDoubleTapped(MotionEvent me) {}
            @Override public void onChartSingleTapped(MotionEvent me) {}
            @Override public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {}
            @Override public void onChartScale(MotionEvent me, float scaleX, float scaleY) {}
            @Override public void onChartTranslate(MotionEvent me, float dX, float dY) {}
        });
    }

    // --- CHART LOGIC ---

    // UPDATE: Modify updateWeeklySummary to run in background
    private void updateWeeklySummary(View view) {
        // Show a loading state here if you want (e.g. progressBar.setVisibility(View.VISIBLE))

        executor.execute(() -> {
            // --- BACKGROUND THREAD STARTS ---
            // 1. Heavy DB Operations
            String startDate = getParent().selectedWeekStartDate;
            MeditationLogDatabaseHelper dbHelper = new MeditationLogDatabaseHelper(getContext());

            // Fetch data
            ArrayList<BarEntry> entries = dbHelper.getWeeklyMeditationDataForDateRange(startDate);
            float total = dbHelper.getTotalWeeklyMeditationHoursForDateRange(startDate, getParent().getNextWeekStartDate(startDate));

            // Calculate Labels
            ArrayList<String> labels = new ArrayList<>();
            try {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());
                LocalDate start = LocalDate.parse(startDate, fmt);
                DateTimeFormatter outFmt = DateTimeFormatter.ofPattern("dd-E", Locale.getDefault());
                LocalDate monday = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                for (int i = 0; i < 7; i++) labels.add(monday.plusDays(i).format(outFmt));
            } catch (Exception e) { e.printStackTrace(); }

            // Prepare UI strings
            String weekNumStr = getParent().getWeekNumber(startDate);
            String dateRangeStr = getParent().getDateRange(startDate);

            // --- BACKGROUND THREAD ENDS ---

            // 2. Post results back to Main UI Thread
            mainHandler.post(() -> {
                if (getView() == null) return; // Fragment might be destroyed

                setupChart(view, entries, labels, 0.5f);

                ((TextView) view.findViewById(R.id.total_hours)).setText(String.format("Total: %.2f hours", total));

                TextView navMain = view.findViewById(R.id.nav_label_main);
                TextView navSub = view.findViewById(R.id.nav_label_sub);

                navSub.setVisibility(View.VISIBLE);
                navMain.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
                navMain.setText(weekNumStr);
                navSub.setText(dateRangeStr);
            });
        });
    }

    // UPDATE: Modify updateMonthlySummary
    private void updateMonthlySummary(View view) {
        executor.execute(() -> {
            String startDate = getParent().selectedMonthStartDate;
            MeditationLogDatabaseHelper dbHelper = new MeditationLogDatabaseHelper(getContext());

            ArrayList<BarEntry> entries = dbHelper.getMonthlyMeditationDataForDateRange(startDate);
            float total = dbHelper.getTotalMonthlyMeditationHoursForDateRange(startDate, getParent().getNextMonthStartDate(startDate));

            ArrayList<String> labels = new ArrayList<>();
            try {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());
                LocalDate start = LocalDate.parse(startDate, fmt);
                WeekFields wf = WeekFields.of(Locale.getDefault());
                for (int i = 0; i < 5; i++) {
                    int weekNum = start.plusWeeks(i).get(wf.weekOfWeekBasedYear());
                    labels.add("Week #" + weekNum);
                }
            } catch (Exception e) { for(int i=1;i<=5;i++) labels.add("Week "+i); }

            String monthYearStr = getParent().getMonthYear(startDate);
            String yearStr = getParent().getYear(startDate);

            mainHandler.post(() -> {
                if (getView() == null) return;

                BarChart chart = setupChart(view, entries, labels, 0.45f);
                setupDrillDown(chart, xIndex -> getParent().drillDownToWeek((int) xIndex));

                ((TextView) view.findViewById(R.id.total_hours)).setText(String.format("Total: %.2f hours", total));

                TextView navMain = view.findViewById(R.id.nav_label_main);
                TextView navSub = view.findViewById(R.id.nav_label_sub);

                navSub.setVisibility(View.VISIBLE);
                navMain.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18);
                navMain.setText(monthYearStr);
                navSub.setText(yearStr);
            });
        });
    }

    // UPDATE: Modify updateYearlySummary
    private void updateYearlySummary(View view) {
        executor.execute(() -> {
            String startDate = getParent().selectedYearStartDate;
            MeditationLogDatabaseHelper dbHelper = new MeditationLogDatabaseHelper(getContext());

            ArrayList<BarEntry> entries = dbHelper.getYearlyMeditationDataForDateRange(startDate);
            float total = dbHelper.getTotalYearlyMeditationHoursForDateRange(startDate, getParent().getNextYearStartDate(startDate));

            ArrayList<String> labels = new ArrayList<>(Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"));

            String yearStr = getParent().getYear(startDate);

            mainHandler.post(() -> {
                if (getView() == null) return;

                BarChart chart = setupChart(view, entries, labels, 0.8f);
                setupDrillDown(chart, xIndex -> getParent().drillDownToMonth((int) xIndex));

                ((TextView) view.findViewById(R.id.total_hours)).setText(String.format("Total: %.2f hours", total));

                TextView navMain = view.findViewById(R.id.nav_label_main);
                TextView navSub = view.findViewById(R.id.nav_label_sub);

                navSub.setVisibility(View.GONE);
                navMain.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 20);
                navMain.setText(yearStr);
            });
        });
    }

    private BarChart setupChart(View view, ArrayList<BarEntry> entries, ArrayList<String> labels, float width) {
        BarChart chart = view.findViewById(R.id.barChart);
        MeditationChartManager manager = new MeditationChartManager(getContext(), chart, myCustomFont);
        manager.displayChart(entries, labels, width);
        return chart;
    }

    // Don't forget to shut down the executor when the fragment is destroyed to prevent leaks
    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}