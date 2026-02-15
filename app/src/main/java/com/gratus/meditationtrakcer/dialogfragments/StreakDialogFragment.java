package com.gratus.meditationtrakcer.dialogfragments;

import static com.gratus.meditationtrakcer.utils.ClearFocusUtils.clearFocusOnKeyboardHide;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.DialogFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

import com.gratus.meditationtrakcer.R;
import com.gratus.meditationtrakcer.BaseActivity;
import com.gratus.meditationtrakcer.datamodels.Streak;
import com.gratus.meditationtrakcer.datamanagers.StreakManager;
import com.gratus.meditationtrakcer.utils.WidgetUpdateHelper;
import com.gratus.meditationtrakcer.widgets.StreakWidgetProvider;

public class StreakDialogFragment extends DialogFragment {

    public interface StreakInputListener {
        void onStreakInputConfirmed(int days, String startDate);
    }

    private static StreakInputListener listener;
    private StreakManager streakManager;

    public static StreakDialogFragment newInstance(StreakInputListener inputListener) {
        listener = inputListener;
        return new StreakDialogFragment();
    }

    private final Calendar calendar = Calendar.getInstance();
    private View blurredView, dialogView;
    private EditText inputStartDate, inputDays;
    private TextView longestStreakText, currentStreakText;
    private Button addStreak;
    private HorizontalScrollView scrollView;

    @SuppressLint("ClickableViewAccessibility")
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_streak, null);

        // --- REMOVED The old blur logic that targeted the local view ---
        streakManager = new StreakManager(requireContext());

        // --- View Initialization ---
        scrollView = dialogView.findViewById(R.id.streak_scroll);
        inputDays = dialogView.findViewById(R.id.streak_days_input);
        inputStartDate = dialogView.findViewById(R.id.streak_start_date);
        addStreak = dialogView.findViewById(R.id.add_streak);

        // Stats Views
        longestStreakText = dialogView.findViewById(R.id.streak_longest_int);
        currentStreakText = dialogView.findViewById(R.id.streak_current_int);
        TextView currentStreakLabel = dialogView.findViewById(R.id.streak_current_title); // For modifying title if needed

        clearFocusOnKeyboardHide(inputDays, dialogView);

        // --- 1. Populate Stats (Longest & Current) ---
        populateStreakStats(longestStreakText, currentStreakText);

        setupHorizontalScrollSnap(scrollView, dialogView);
        setupOnClickListeners();

        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(dialogView);
        // ---> ADD THIS BEFORE RETURNING THE DIALOG <---
        if (getActivity() instanceof BaseActivity) {
            ((BaseActivity) getActivity()).applySystemFontToView(dialogView);
        }
        return dialog;
    }

    // ✅ NEW: Helper method to handle Flick (Velocity) and Snap
    @SuppressLint("ClickableViewAccessibility")
    private void setupHorizontalScrollSnap(HorizontalScrollView scrollView, View dialogView) {
        final GestureDetector gestureDetector = new GestureDetector(requireContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(@NonNull MotionEvent e) {
                // Must return true to allow further gestures like Fling to be detected
                return true;
            }

            @Override
            public boolean onFling(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
                try {
                    // Small threshold for "flick" detection
                    int SWIPE_THRESHOLD_VELOCITY = 300;

                    if (Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                        View inputPage = dialogView.findViewById(R.id.streak_input);
                        int pageWidth = inputPage.getWidth();
                        int scrollX = scrollView.getScrollX();
                        int currentPage = (scrollX + (pageWidth / 2)) / pageWidth;

                        int targetPage;
                        // Negative Velocity = Swipe Left (Finger Right->Left) -> Go Next
                        // Positive Velocity = Swipe Right (Finger Left->Right) -> Go Previous
                        if (velocityX < 0) {
                            targetPage = 1; // Force go to Next Page
                        } else {
                            targetPage = 0; // Force go to Previous Page
                        }

                        // Bounds Check (Assuming 2 pages: 0 and 1)
                        targetPage = Math.max(0, Math.min(targetPage, 1));

                        int targetX = targetPage * pageWidth;
                        scrollView.smoothScrollTo(targetX, 0);
                        return true; // Mark handled so we don't snap again
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        });

        scrollView.setOnTouchListener((v, event) -> {
            // 1. Pass event to detector.
            // If onFling triggers (during ACTION_UP), this returns true.
            boolean isFlingHandled = gestureDetector.onTouchEvent(event);

            // 2. Handle Release (ACTION_UP)
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (isFlingHandled) {
                    return true; // Fling took care of the scroll
                }

                // 3. Fallback: If no flick detected (just slow drag), use position-based snap
                View inputPage = dialogView.findViewById(R.id.streak_input);
                int pageWidth = inputPage.getWidth();
                int scrollX = scrollView.getScrollX();
                int page = (scrollX + (pageWidth / 2)) / pageWidth;
                int targetX = page * pageWidth;

                scrollView.post(() -> scrollView.smoothScrollTo(targetX, 0));
                return true; // Consume event to stop default inertia
            }

            // 4. Return false for Down/Move to let ScrollView handle normal dragging
            return false;
        });
    }

    private void setupOnClickListeners() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        inputStartDate.setText("");

        inputStartDate.setOnClickListener(v -> {
            DatePickerDialog dPDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        inputStartDate.setText(sdf.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            // After creating the dialog, set the first day of week on its DatePicker
            dPDialog.getDatePicker().setFirstDayOfWeek(Calendar.MONDAY);

            dPDialog.setOnShowListener(dialog -> {
                Button positiveButton = dPDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                Button negativeButton = dPDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                boolean isDarkMode = isDarkMode();
                int positiveColor = isDarkMode ? ContextCompat.getColor(requireContext(), R.color.inverseprimary) : ContextCompat.getColor(requireContext(), R.color.inverseprimary);
                int negativeColor = isDarkMode ? ContextCompat.getColor(requireContext(), R.color.inverseprimary) : ContextCompat.getColor(requireContext(), R.color.inverseprimary);
                positiveButton.setTextColor(positiveColor);
                negativeButton.setTextColor(negativeColor);

                // Apply rounded background
                Objects.requireNonNull(dPDialog.getWindow()).setBackgroundDrawableResource(R.drawable.datepicker_rounded_corners);
            });

            dPDialog.show();
        });

        addStreak.setOnClickListener(v -> {
            String dayStr = inputDays.getText().toString().trim();
            String startDateStr = inputStartDate.getText().toString().trim();
            if (!dayStr.isEmpty() && !startDateStr.isEmpty()) { // Ensure date is not empty
                int days = Integer.parseInt(dayStr);
                if (listener != null) {
                    listener.onStreakInputConfirmed(days, startDateStr);
                    WidgetUpdateHelper.updateAllWidgets(requireContext());
                }
                dismiss();
            }
        });
    }

    private void populateStreakStats(TextView longestTv, TextView currentTv) {
        // 1. Longest Streak
        int longest = streakManager.getLongestStreak();
        longestTv.setText(String.valueOf(longest));

        // 2. Current Streak Logic
        Streak activeStreak = streakManager.getActiveStreak();

        if (activeStreak != null) {
            // Scenario A: Active Goal Exists -> Show "Achieved / Target"
            // Ensure we calculate progress first so it's up to date
            streakManager.updateActiveStreakProgress();
            // Refetch to get updated achieved days
            activeStreak = streakManager.getActiveStreak();

            if(activeStreak != null) {
                String progress = activeStreak.getAchievedDays() + "/" + activeStreak.getTargetDays();
                currentTv.setText(progress);
            }
        } else {
            // Scenario B: No Active Goal -> Show simple contiguous count
            int currentContiguous = streakManager.getContiguousMeditationDays();
            currentTv.setText(String.valueOf(currentContiguous));
        }
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