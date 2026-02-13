package com.gratus.meditationtrakcer.dialogfragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.DialogFragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarEntry;
import com.gratus.meditationtrakcer.BaseActivity;
import com.gratus.meditationtrakcer.R;
import com.gratus.meditationtrakcer.models.MeditationReportData;
import com.gratus.meditationtrakcer.utils.MeditationChartManager;
import com.gratus.meditationtrakcer.utils.ReportJsonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportDetailDialogFragment extends DialogFragment {

    private static final String ARG_REPORT_ID = "arg_report_id";

    // Static factory method to pass arguments
    public static ReportDetailDialogFragment newInstance(String reportId) {
        ReportDetailDialogFragment fragment = new ReportDetailDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_REPORT_ID, reportId);
        fragment.setArguments(args);
        return fragment;
    }

    private View blurredView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.report_card, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ---> ADD THIS BEFORE RETURNING THE VIEW <---
        if (getActivity() instanceof BaseActivity) {
            ((BaseActivity) getActivity()).applySystemFontToView(view);
        }

        if (getArguments() == null) {
            dismiss();
            return;
        }

        String reportId = getArguments().getString(ARG_REPORT_ID);
        List<MeditationReportData> reports = ReportJsonHelper.loadReports(requireContext());

        MeditationReportData data = null;
        for (MeditationReportData r : reports) {
            if (r.reportId.equals(reportId)) {
                data = r;
                break;
            }
        }

        if (data == null) {
            dismiss();
            return;
        }

        populateViews(view, data);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                // --- NEW: Enable edge-to-edge display ---
                WindowCompat.setDecorFitsSystemWindows(window, false);

                // Set window to full screen
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                // Keep the window background transparent
                window.setBackgroundDrawableResource(android.R.color.transparent);
                // Clear default dim
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

                // --- NEW: Set system bar colors to transparent ---
                window.setStatusBarColor(Color.TRANSPARENT);
                window.setNavigationBarColor(Color.TRANSPARENT);

                // Apply blur if supported
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    applyBlur();
                }
            }
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        // Remove the blur effect when the dialog is dismissed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            removeBlur();
        }
        super.onDismiss(dialog);
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void applyBlur() {
        blurredView = requireActivity().getWindow().getDecorView().findViewById(android.R.id.content);
        if (blurredView != null) {
            RenderEffect blur = RenderEffect.createBlurEffect(8f, 8f, Shader.TileMode.DECAL);
            blurredView.setRenderEffect(blur);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void removeBlur() {
        if (blurredView != null) {
            blurredView.setRenderEffect(null);
        }
    }

    private void populateViews(View view, MeditationReportData data) {
        // --- Header ---
        // Note: I added a null check or use a fallback if title is missing
        TextView tvTitle = view.findViewById(R.id.tvReportTitle);
        if (tvTitle != null) tvTitle.setText(data.title);

        TextView tvSub = view.findViewById(R.id.tvReportYearMonth);
        // Assuming your Data model has title/isYearly logic, or use generic text
        if (tvSub != null) tvSub.setText(data.isYearly ? "Yearly Meditation Report" : "Monthly Meditation Report");

        // --- Row 1: Hero Metrics ---
        ((TextView) view.findViewById(R.id.tvTotalHoursValue)).setText(fmt(data.totalHours));
        ((TextView) view.findViewById(R.id.tvConsistencyValue)).setText(String.valueOf(data.consistencyScore));
        ((TextView) view.findViewById(R.id.tvBestStreakValue)).setText(String.valueOf(data.bestStreak));

        // --- Row 2: Gaps ---
        ((TextView) view.findViewById(R.id.tvDaysNotMeditatedValue)).setText(String.valueOf(data.daysNotMeditated));
        ((TextView) view.findViewById(R.id.tvWeeksNotMeditatedValue)).setText(String.valueOf(data.weeksNotMeditated));
        ((TextView) view.findViewById(R.id.tvStreakStabilityValue)).setText(fmt(data.streakStability));

        // --- Row 3: Sessions ---
        ((TextView) view.findViewById(R.id.tvTotalSessionsValue)).setText(String.valueOf(data.totalSessions));
        ((TextView) view.findViewById(R.id.tvAvgGapValue)).setText(fmt(data.avgSessionGap));
        ((TextView) view.findViewById(R.id.tvAvgSessionValue)).setText(String.valueOf(data.avgSessionLength));

        // --- Row 4: Activity ---
        TextView tvMost = view.findViewById(R.id.tvMostActiveValue);
        if (data.mostActiveMonthLabel != null) {
            tvMost.setText(data.mostActiveMonthLabel + " – " + fmt(data.mostActiveMonthValue));
        } else {
            tvMost.setText("N/A");
        }

        TextView tvLeast = view.findViewById(R.id.tvLeastActiveValue);
        if (data.leastActiveMonthLabel != null) {
            tvLeast.setText(data.leastActiveMonthLabel + " – " + fmt(data.leastActiveMonthValue));
        } else {
            tvLeast.setText("N/A");
        }

        // --- Row 5: Charts ---
        setupCharts(view, data);
    }

    private void setupCharts(View view, MeditationReportData data) {

        // Read preference (Default true)
        android.content.SharedPreferences prefs = requireContext().
                getSharedPreferences(com.gratus.meditationtrakcer.BaseActivity.SHARED_PREFS_NAME, android.content.Context.MODE_PRIVATE);
        boolean showYAxis = prefs.getBoolean("y_axis_visible", true);
        boolean useSystemFont = prefs.getBoolean("use_system_font", false);
        Typeface font;
        if (useSystemFont) {
            font = android.graphics.Typeface.DEFAULT;
        } else {
            font = getResources().getFont(R.font.atkinsonhyperlegiblenext_regular);
        }

        // 1. Preferred Times Chart
        BarChart timeChart = view.findViewById(R.id.chartPreferredTimes);
        MeditationChartManager timeManager = new MeditationChartManager(requireContext(), timeChart, font);
        timeManager.setYAxisEnabled(showYAxis); // Set based on preference

        ArrayList<BarEntry> timeEntries = new ArrayList<>();
        List<String> timeLabels = new ArrayList<>();
        int i = 0;
        String[] timeKeys = { "Morning", "Noon", "Evening" };
        for (String key : timeKeys) {
            Integer val = data.preferredTimes.get(key);
            timeEntries.add(new BarEntry(i++, val != null ? val : 0));
            timeLabels.add(key);
        }
        timeManager.displayChart(timeEntries, timeLabels, 0.5f);

        // 2. Frequency Chart
        BarChart freqChart = view.findViewById(R.id.chartSessionFrequency);
        MeditationChartManager freqManager = new MeditationChartManager(requireContext(), freqChart, font);
        freqManager.setYAxisEnabled(showYAxis); // Set based on preference

        ArrayList<BarEntry> freqEntries = new ArrayList<>();
        List<String> freqLabels = new ArrayList<>();
        String[] freqKeys = { "0-5 min", "5-10 min", "10-25 min", ">25mins" };
        String[] displayLabels = { "0-5", "5-10", "10-25", ">25" };

        for (int k = 0; k < freqKeys.length; k++) {
            Integer val = data.sessionFrequency.get(freqKeys[k]);
            freqEntries.add(new BarEntry(k, val != null ? val : 0));
            freqLabels.add(displayLabels[k]);
        }
        freqManager.displayChart(freqEntries, freqLabels, 0.5f);
    }

    private String fmt(float val) {
        if (val == (long) val) return String.format(Locale.getDefault(), "%d", (long) val);
        return String.format(Locale.getDefault(), "%.1f", val);
    }
}