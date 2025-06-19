package com.gratus.meditationtrakcer;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.DatePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import com.gratus.meditationtrakcer.R;

public class StreakDialogFragment extends DialogFragment {

    public interface StreakInputListener {
        void onStreakInputConfirmed(int days, String startDate);
    }

    private static StreakInputListener listener;

    public static StreakDialogFragment newInstance(StreakInputListener inputListener) {
        listener = inputListener;
        return new StreakDialogFragment();
    }

    private final Calendar calendar = Calendar.getInstance();
    private View blurredView;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_streak, null);

        // --- REMOVED The old blur logic that targeted the local view ---

        EditText inputDays = dialogView.findViewById(R.id.streak_days_input);
        EditText inputStartDate = dialogView.findViewById(R.id.streak_start_date);
        Button addStreak = dialogView.findViewById(R.id.add_streak);

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        inputStartDate.setText("");

        inputStartDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        inputStartDate.setText(sdf.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            datePickerDialog.setOnShowListener(dialog -> {
                Button positiveButton = datePickerDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                Button negativeButton = datePickerDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                boolean isDarkMode = isDarkMode();
                int positiveColor = isDarkMode ? ContextCompat.getColor(requireContext(), R.color.light_primaryVariant) : ContextCompat.getColor(requireContext(), R.color.dark_primary);
                int negativeColor = isDarkMode ? ContextCompat.getColor(requireContext(), R.color.light_primaryVariant) : ContextCompat.getColor(requireContext(), R.color.dark_primary);
                positiveButton.setTextColor(positiveColor);
                negativeButton.setTextColor(negativeColor);
            });

            datePickerDialog.show();
        });

        addStreak.setOnClickListener(v -> {
            String dayStr = inputDays.getText().toString().trim();
            String startDateStr = inputStartDate.getText().toString().trim();
            if (!dayStr.isEmpty()) {
                int days = Integer.parseInt(dayStr);
                if (listener != null) listener.onStreakInputConfirmed(days, startDateStr);
                dismiss();
            }
        });

        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(dialogView);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setDimAmount(0.32f);
            // Remove the default dim background
            //dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            // Apply blur to the activity's content view
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                applyBlur();
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
            RenderEffect blur = RenderEffect.createBlurEffect(12f, 12f, Shader.TileMode.CLAMP);
            blurredView.setRenderEffect(blur);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void removeBlur() {
        if (blurredView != null) {
            blurredView.setRenderEffect(null);
        }
    }

    private boolean isDarkMode() {
        boolean isDarkMode;
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            isDarkMode = (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES);
        } else {
            isDarkMode = (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES);
        }
        return isDarkMode;
    }
}