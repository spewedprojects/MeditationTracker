package com.gratus.meditationtrakcer;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.gratus.meditationtrakcer.dialogfragments.YMPickerDialogFragment;

import com.google.android.material.button.MaterialButtonToggleGroup;

public class ReportsActivity extends BaseActivity {

    private ImageButton btn_YearReport, btn_MonthReport;
    private LinearLayout buttonContainer, initiateButtonLayout, MYreport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reports);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.report_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Initialize the toolbar and menu button
        setupToolbar(R.id.toolbar2, R.id.menubutton);

        btn_YearReport = findViewById(R.id.btn_reportYear);
        btn_MonthReport = findViewById(R.id.btn_reportMonth);


        btn_MonthReport.setOnClickListener(v -> {
            YMPickerDialogFragment.newInstance(
                    YMPickerDialogFragment.PickerMode.YEAR_MONTH,
                    new YMPickerDialogFragment.OnYMSelectedListener() {
                        @Override
                        public void onYearSelected(int year) {}

                        @Override
                        public void onYearMonthSelected(int year, int month) {
                            // handle month report
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
                            // handle year report
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
}

