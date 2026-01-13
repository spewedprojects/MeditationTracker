package com.gratus.meditationtrakcer;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.content.Intent;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gratus.meditationtrakcer.adapters.ReportsAdapter;
import com.gratus.meditationtrakcer.dialogfragments.YMPickerDialogFragment;
import com.gratus.meditationtrakcer.models.MeditationReportData;
import com.gratus.meditationtrakcer.utils.ReportGenerator;
import com.gratus.meditationtrakcer.utils.ReportJsonHelper;

import com.google.android.material.button.MaterialButtonToggleGroup;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ReportsActivity extends BaseActivity {

    private ImageButton btn_YearReport, btn_MonthReport;
    private LinearLayout buttonContainer, initiateButtonLayout, MYreport;
    private RecyclerView rvReports;
    private ReportsAdapter adapter;
    private MotionLayout motionLayout;
    private List<MeditationReportData> reportList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reports_motion);
        motionLayout = findViewById(R.id.report_root);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.report_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Initialize the toolbar and menu button
        setupToolbar(R.id.toolbar2, R.id.menubutton);

        btn_YearReport = findViewById(R.id.btn_reportYear);
        btn_MonthReport = findViewById(R.id.btn_reportMonth);

        rvReports = findViewById(R.id.rv_reports_list);

        // Setup RecyclerView
        rvReports.setLayoutManager(new LinearLayoutManager(this));

        // Load Data
        refreshDataAndState();

        btn_MonthReport.setOnClickListener(v -> {
            YMPickerDialogFragment.newInstance(
                    YMPickerDialogFragment.PickerMode.YEAR_MONTH,
                    new YMPickerDialogFragment.OnYMSelectedListener() {
                        @Override
                        public void onYearSelected(int year) {}

                        @Override
                        public void onYearMonthSelected(int year, int month) {
                            // handle month report (13/01/26)
                            generateMonthReport(year, month);
                        }
                    }
            ).show(getSupportFragmentManager(), "ym_picker");
        });

        btn_YearReport.setOnClickListener(v -> {
            YMPickerDialogFragment.newInstance(
                    YMPickerDialogFragment.PickerMode.YEAR_ONLY,
                    new YMPickerDialogFragment.OnYMSelectedListener() {
                        @Override
                        public void onYearSelected(int year) {
                            // handle year report (13/01/26)
                            generateYearReport(year);
                        }

                        @Override
                        public void onYearMonthSelected(int year, int month) {}
                    }
            ).show(getSupportFragmentManager(), "y_picker");
        });


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Your custom logic here
                // To simply go back:
                setEnabled(false); // Disable this callback
                getOnBackPressedDispatcher().onBackPressed(); // Call again
            }
        });

    }

    private void refreshDataAndState() {
        reportList = ReportJsonHelper.loadReports(this);

        adapter = new ReportsAdapter(this, reportList, (data, sharedView) -> {
            // NEW: Show Dialog
            com.gratus.meditationtrakcer.dialogfragments.ReportDetailDialogFragment
                    .newInstance(data.reportId)
                    .show(getSupportFragmentManager(), "detail_report_dialog");
        });

        // Add a data observer to check if list becomes empty after deletion
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                checkStateTransition();
            }
        });

        rvReports.setAdapter(adapter);
        checkStateTransition();
    }

    private void checkStateTransition() {
        if (reportList == null || reportList.isEmpty()) {
            motionLayout.transitionToStart();
        } else {
            motionLayout.transitionToEnd();
        }
    }

    private void generateMonthReport(int year, int month) {
        // Calculate Start Date (e.g., "2025-05-01")
        String startDate = String.format(Locale.getDefault(), "%04d-%02d-01", year, month);

        // Calculate End Date
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1);
        int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        String endDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month, lastDay);

        // Title
        String title = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.getTime());

        // Generate & Save
        MeditationReportData data = ReportGenerator.generateReport(this, startDate, endDate, false, title);
        ReportJsonHelper.saveReport(this, data);

        refreshDataAndState();
    }

    private void generateYearReport(int year) {
        String startDate = String.format(Locale.getDefault(), "%04d-01-01", year);
        String endDate = String.format(Locale.getDefault(), "%04d-12-31", year);
        String title = "Year " + year;

        // Generate & Save
        MeditationReportData data = ReportGenerator.generateReport(this, startDate, endDate, true, title);
        ReportJsonHelper.saveReport(this, data);

        refreshDataAndState();
    }
}


