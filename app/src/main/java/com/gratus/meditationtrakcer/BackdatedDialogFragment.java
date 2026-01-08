package com.gratus.meditationtrakcer;

import android.app.Dialog;
import android.content.Context;
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
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

public class BackdatedDialogFragment extends DialogFragment {

    // Interface to pass data back to MainActivity
    public interface BackdatedEntryListener {
        void onBackdatedEntryAdded(long dateInMillis, int durationSeconds);
    }

    private BackdatedEntryListener listener;
    private long selectedDateInMillis;
    private View blurredView;

    public static BackdatedDialogFragment newInstance() {
        return new BackdatedDialogFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof BackdatedEntryListener) {
            listener = (BackdatedEntryListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement BackdatedEntryListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.backdated_manual_dialog, container, false);
        Typeface customFont = ResourcesCompat.getFont(requireContext(), R.font.atkinsonhyperlegiblenext_regular);

        // Make the dialog background transparent so your CardView radius shows correctly
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        // Initialize Views
        CalendarView calendarView = view.findViewById(R.id.backdate_picker);
        applyFontRecursively(calendarView, customFont);

        EditText editHours = view.findViewById(R.id.manual_hours_B);
        EditText editMinutes = view.findViewById(R.id.manual_minutes_B);
        EditText editSeconds = view.findViewById(R.id.manual_seconds_B);
        Button btnAdd = view.findViewById(R.id.add_entry_B);

        // Configure CalendarView
        long today = System.currentTimeMillis();
        calendarView.setMaxDate(today); // Disable future dates
        selectedDateInMillis = today;   // Default to today

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            // Create a Calendar object to store the selected date
            Calendar c = Calendar.getInstance();
            c.set(year, month, dayOfMonth);
            // Optionally set time to noon to avoid timezone edge cases
            c.set(Calendar.HOUR_OF_DAY, 12);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            selectedDateInMillis = c.getTimeInMillis();
        });

        // Handle Add Button
        btnAdd.setOnClickListener(v -> {
            int h = parseInput(editHours);
            int m = parseInput(editMinutes);
            int s = parseInput(editSeconds);

            int totalSeconds = (h * 3600) + (m * 60) + s;

            if (totalSeconds > 0) {
                // Pass data back to MainActivity
                if (listener != null) {
                    listener.onBackdatedEntryAdded(selectedDateInMillis, totalSeconds);
                }
                dismiss();
            } else {
                Toast.makeText(getContext(), "Please enter a valid duration", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void applyFontRecursively(View view, Typeface typeface) {
        if (view instanceof android.widget.TextView) {
            ((android.widget.TextView) view).setTypeface(typeface);
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyFontRecursively(group.getChildAt(i), typeface);
            }
        }
    }

    // Helper to parse EditText safely
    private int parseInput(EditText editText) {
        String input = editText.getText().toString().trim();
        if (input.isEmpty()) return 0;
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

//    @Override
//    public void onStart() {
//        super.onStart();
//        // Ensure the dialog is wide enough
//        Dialog dialog = getDialog();
//        if (dialog != null) {
//            int width = ViewGroup.LayoutParams.WRAP_CONTENT;
//            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
//            dialog.getWindow().setLayout(width, height);
//        }
//    }

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
                window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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
            RenderEffect blur = RenderEffect.createBlurEffect(12f, 12f, Shader.TileMode.DECAL);
            blurredView.setRenderEffect(blur);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void removeBlur() {
        if (blurredView != null) {
            blurredView.setRenderEffect(null);
        }
    }
}