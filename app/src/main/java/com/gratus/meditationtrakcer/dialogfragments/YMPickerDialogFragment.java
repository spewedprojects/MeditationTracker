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

import com.gratus.meditationtrakcer.R;

import java.util.Calendar;

public class YMPickerDialogFragment extends DialogFragment {

    public enum PickerMode {
        YEAR_ONLY,
        YEAR_MONTH
    }

    public interface OnYMSelectedListener {
        void onYearSelected(int year);
        void onYearMonthSelected(int year, int month); // month: 1–12
    }

    private static final String ARG_MODE = "picker_mode";

    private static OnYMSelectedListener listener;
    private PickerMode mode;

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
        return dialog;
    }

    /* ---------------- YEAR ONLY ---------------- */

    private void setupYearOnly(View view) {
        TextView yearView = view.findViewById(R.id.list_years);
        View selectBtn = view.findViewById(R.id.btnselect_Year);

        final int[] selectedYear = {calendar.get(Calendar.YEAR)};
        yearView.setText(String.valueOf(selectedYear[0]));

        yearView.setOnClickListener(v -> {
            selectedYear[0]++;
            yearView.setText(String.valueOf(selectedYear[0]));
        });

        selectBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onYearSelected(selectedYear[0]);
            }
            dismiss();
        });
    }

    /* ---------------- YEAR + MONTH ---------------- */

    private void setupYearMonth(View view) {
        TextView yearView = view.findViewById(R.id.select_Year);

        final int[] selectedYear = {calendar.get(Calendar.YEAR)};
        final int[] selectedMonth = {-1};

        yearView.setText(String.valueOf(selectedYear[0]));

        yearView.setOnClickListener(v -> {
            selectedYear[0]++;
            yearView.setText(String.valueOf(selectedYear[0]));
        });

        int[] monthIds = {
                R.id.month_jan, R.id.month_feb, R.id.month_mar,
                R.id.month_apr, R.id.month_may, R.id.month_jun,
                R.id.month_jul, R.id.month_aug, R.id.month_sep,
                R.id.month_oct, R.id.month_nov, R.id.month_dec
        };

        for (int i = 0; i < monthIds.length; i++) {
            int monthIndex = i + 1;
            view.findViewById(monthIds[i]).setOnClickListener(v -> {
                selectedMonth[0] = monthIndex;
                if (listener != null) {
                    listener.onYearMonthSelected(selectedYear[0], selectedMonth[0]);
                }
                dismiss();
            });
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
