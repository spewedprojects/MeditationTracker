package com.gratus.meditationtrakcer.dialogfragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.gratus.meditationtrakcer.BaseActivity;
import com.gratus.meditationtrakcer.R;
import com.gratus.meditationtrakcer.adapters.YearAdapter;
import com.gratus.meditationtrakcer.utils.YearUtils;
import com.gratus.meditationtrakcer.utils.CenterZoomLayoutManager;

import java.util.Calendar;
import java.util.List;

public class YMPickerDialogFragment extends DialogFragment {

    public enum PickerMode { YEAR_ONLY, YEAR_MONTH }

    public interface OnYMSelectedListener {
        void onYearSelected(int year);
        void onYearMonthSelected(int year, int month); // month: 1–12
    }

    private static final String ARG_MODE = "picker_mode";

    private static OnYMSelectedListener listener;
    private PickerMode mode;

    private int selectedYear;
    private int selectedMonth = -1;


    private View blurredView;
    private final Calendar calendar = Calendar.getInstance();

    public static YMPickerDialogFragment newInstance(
            PickerMode mode,
            OnYMSelectedListener ymListener
    ) {
        listener = ymListener;
        Bundle args = new Bundle();
        args.putString(ARG_MODE, mode.name());

        YMPickerDialogFragment fragment = new YMPickerDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        mode = PickerMode.valueOf(
                requireArguments().getString(ARG_MODE)
        );

        View dialogView;
        if (mode == PickerMode.YEAR_ONLY) {
            dialogView = LayoutInflater.from(getActivity())
                    .inflate(R.layout.dialog_year_picker, null);
            setupYearOnly(dialogView);
        } else {
            dialogView = LayoutInflater.from(getActivity())
                    .inflate(R.layout.dialog_yearmonth_picker, null);
            setupYearMonth(dialogView);
        }

        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(dialogView);

        // ---> ADD THIS BEFORE RETURNING THE VIEW <---
        if (getActivity() instanceof BaseActivity) {
            ((BaseActivity) getActivity()).applySystemFontToView(dialogView);
        }
        return dialog;
    }

    /* ---------------- YEAR ONLY (Vertical Carousel) ---------------- */
    private void setupYearOnly(View view) {
        RecyclerView rvYears = view.findViewById(R.id.rv_years);
        View selectBtn = view.findViewById(R.id.btnselect_Year);

        selectedYear = calendar.get(Calendar.YEAR);
        List<Integer> years = YearUtils.generatePastYears();

        // Use Custom Layout Manager (Vertical)
        CenterZoomLayoutManager lm = new CenterZoomLayoutManager(requireContext(), RecyclerView.VERTICAL, false);
        rvYears.setLayoutManager(lm);

        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(rvYears);

        YearAdapter adapter = new YearAdapter(years);
        rvYears.setAdapter(adapter);

        // Scroll Logic: Update selectedYear when scroll settles
        rvYears.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    View centerView = snapHelper.findSnapView(lm);
                    if (centerView != null) {
                        int pos = lm.getPosition(centerView);
                        selectedYear = adapter.getYearAt(pos);
                    }
                }
            }
        });

        // Initial Scroll
        rvYears.scrollToPosition(years.indexOf(selectedYear));
        // Force a tiny scroll to trigger the scaling effect immediately
        // Calculate padding to center the first/last items
        // Since item size is 1/3 of parent, padding needs to be 1/3 of parent
        // Post this to run AFTER the view has been measured
        rvYears.post(() -> {
            // Force padding to be exactly 1/3 of the height
            int padding = rvYears.getHeight() / 3;
            rvYears.setPadding(0, padding, 0, padding);

            // Scroll to selected year immediately
            int index = years.indexOf(selectedYear);
            if (index >= 0) {
                rvYears.scrollToPosition(index);
                // Tiny scroll tweak to trigger the "Zoom" effect immediately
                rvYears.scrollBy(0, 1);
            }
        });

        selectBtn.setOnClickListener(v -> {
            if (listener != null) listener.onYearSelected(selectedYear);
            dismiss();
        });
    }

    /* ---------------- YEAR + MONTH (Horizontal Carousel) ---------------- */
    private void setupYearMonth(View view) {
        RecyclerView rvYears = view.findViewById(R.id.rv_years_horizontal);
        selectedYear = calendar.get(Calendar.YEAR);
        List<Integer> years = YearUtils.generatePastYears();

        // Use Custom Layout Manager (Horizontal)
        CenterZoomLayoutManager lm = new CenterZoomLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false);
        rvYears.setLayoutManager(lm);

        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(rvYears);

        YearAdapter adapter = new YearAdapter(years);
        rvYears.setAdapter(adapter);

        // ✅ CRITICAL FIX: Update Month Availability on Scroll Snap
        rvYears.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    View centerView = snapHelper.findSnapView(lm);
                    if (centerView != null) {
                        int pos = lm.getPosition(centerView);
                        int newYear = adapter.getYearAt(pos);

                        // Only update if year actually changed
                        if (newYear != -1 && newYear != selectedYear) {
                            selectedYear = newYear;
                            updateMonthAvailability(view); // Update grid
                        }
                    }
                }
            }
        });

        // Initial Scroll
        rvYears.post(() -> {
            int padding = rvYears.getWidth() / 3;
            rvYears.setPadding(padding, 0, padding, 0);

            int index = years.indexOf(selectedYear);
            if (index >= 0) {
                rvYears.scrollToPosition(index);
                rvYears.scrollBy(1, 0);
            }
        });

        // Month Click Listeners
        int[] monthIds = {
                R.id.month_jan, R.id.month_feb, R.id.month_mar,
                R.id.month_apr, R.id.month_may, R.id.month_jun,
                R.id.month_jul, R.id.month_aug, R.id.month_sep,
                R.id.month_oct, R.id.month_nov, R.id.month_dec
        };

        for (int i = 0; i < monthIds.length; i++) {
            int monthIndex = i + 1;
            view.findViewById(monthIds[i]).setOnClickListener(v -> {
                selectedMonth = monthIndex;
                if (listener != null) listener.onYearMonthSelected(selectedYear, selectedMonth);
                dismiss();
            });
        }

        updateMonthAvailability(view);
    }

    private void updateMonthAvailability(View root) {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;

        int[] monthIds = {
                R.id.month_jan, R.id.month_feb, R.id.month_mar,
                R.id.month_apr, R.id.month_may, R.id.month_jun,
                R.id.month_jul, R.id.month_aug, R.id.month_sep,
                R.id.month_oct, R.id.month_nov, R.id.month_dec
        };

        for (int i = 0; i < monthIds.length; i++) {
            int month = i + 1;
            View btn = root.findViewById(monthIds[i]);

            // Logic:
            // 1. If selected year is PAST, all months enabled.
            // 2. If selected year is CURRENT, months <= current month enabled.
            // 3. If selected year is FUTURE, all disabled (optional, depending on use case).
            boolean enabled = selectedYear < currentYear ||
                    (selectedYear == currentYear && month <= currentMonth);

            btn.setEnabled(enabled);
            btn.setAlpha(enabled ? 1f : 0.3f);
        }
    }


    /* ---------------- WINDOW / BLUR (CONSISTENT) ---------------- */
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, false);
                window.setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                window.setBackgroundDrawableResource(android.R.color.transparent);
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

                window.setStatusBarColor(Color.TRANSPARENT);
                window.setNavigationBarColor(Color.TRANSPARENT);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    applyBlur();
                }
            }
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            removeBlur();
        }
        super.onDismiss(dialog);
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void applyBlur() {
        blurredView = requireActivity()
                .getWindow()
                .getDecorView()
                .findViewById(android.R.id.content);
        if (blurredView != null) {
            blurredView.setRenderEffect(
                    RenderEffect.createBlurEffect(
                            12f, 12f, Shader.TileMode.DECAL
                    )
            );
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void removeBlur() {
        if (blurredView != null) {
            blurredView.setRenderEffect(null);
        }
    }
}
