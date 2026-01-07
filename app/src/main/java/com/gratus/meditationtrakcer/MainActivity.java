package com.gratus.meditationtrakcer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
//import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends BaseActivity {

    private TextView dateDisplay, timerDisplay, todayTotalDisplay, weekTotalDisplay, streakText;
    private Button recordButton, addEntryButton;
    private ImageButton gotoGoalsButton, moreMenuButton;
    private EditText manualHours, manualMinutes, manualSeconds;
    private boolean isTimerRunning = false;
    private int secondsElapsed = 0;
    private int totalSecondsLogged = 0;
    private Handler handler = new Handler();
    private StreakManager streakManager; // Add this

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize the toolbar and menu button
        setupToolbar(R.id.toolbar2, R.id.menubutton);

        // Initialize your new manager
        streakManager = new StreakManager(this);

        // Initialize UI elements
        dateDisplay = findViewById(R.id.date_display);
        timerDisplay = findViewById(R.id.timer_display);
        todayTotalDisplay = findViewById(R.id.today_total);
        weekTotalDisplay = findViewById(R.id.week_total);
        recordButton = findViewById(R.id.record);
        addEntryButton = findViewById(R.id.add_entry);
        manualHours = findViewById(R.id.manual_hours);
        manualMinutes = findViewById(R.id.manual_minutes);
        manualSeconds = findViewById(R.id.manual_seconds);
        //moreMenuButton = findViewById(R.id.menubutton);

        MaterialCardView streakCard = findViewById(R.id.cardView3_streak);

        // Streak dialog toggle
        streakCard.setOnLongClickListener(v -> {
            StreakDialogFragment dialog = StreakDialogFragment.newInstance((days, startDate) -> {
                // This is the callback from the dialog
                streakManager.startNewStreak(startDate, days);
                // Refresh the UI immediately
                refreshStreakUI();
            });
            dialog.show(getSupportFragmentManager(), "streak_dialog");
            return true;
        });

        // --- UPDATED LISTENER FOR WEEK VIEW ---
        // Long-pressing week total takes you to Summary activity, specifically WEEK view
        weekTotalDisplay.setOnLongClickListener(v -> {
            // Force "Week" view preference before launching.
            // This ensures SummaryActivity opens in "W" mode regardless of previous state.
            SharedPreferences sp = getSharedPreferences("summary_prefs", MODE_PRIVATE);
            sp.edit().putString("last_view", "W").apply();

            Intent intent = new Intent(v.getContext(), SummaryActivity.class);
            v.getContext().startActivity(intent);
            return true; // Important: Consume the long click event
        });

        displayShortestAndLatestGoal();  // New method call

        // Display today's date
        updateDateDisplay();

        // ✅ Load saved total from database
        MeditationLogDatabaseHelper dbHelper = new MeditationLogDatabaseHelper(this);
        totalSecondsLogged = dbHelper.getTodayLoggedSeconds();
        dbHelper.close();

        // Record button functionality
        recordButton.setOnClickListener(v -> {
            if (isTimerRunning) {
                stopTimer();
            } else {
                startTimer();
            }
        });

        // Add manual entry functionality
        addEntryButton.setOnClickListener(v -> addManualEntry());

        updateTodayTotal();
        updateWeekTotal();

        // Menu button functionality
        //moreMenuButton.setOnClickListener(v -> openMenu());

        gotoGoalsButton = findViewById(R.id.goto_goals);
        gotoGoalsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GoalsActivity.class);
            startActivity(intent);
        });

        refreshStreakUI();
    }

    private BroadcastReceiver timerUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("TIMER_UPDATED")) {
                secondsElapsed = intent.getIntExtra("secondsElapsed", 0); // Get the elapsed time
                updateTimerDisplay(); // Update the timer display
            }
        }
    };

    private SpannableString createStyledStreakText(int days) {
        streakText = findViewById(R.id.streak);
        String numberPart = String.valueOf(days);
        String unitPart = "d";
        String fullText = numberPart + unitPart;

        SpannableString styledText = new SpannableString(fullText);

        // --- Define the styles for the "d" character ---

        // 1. Set a smaller font size (e.g., 25% of the original size, try 16% next)
        float relativeSize = 0.18f;
        styledText.setSpan(
                new RelativeSizeSpan(relativeSize),
                numberPart.length(), // Start index of "d"
                fullText.length(),   // End index of "d"
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // 2. Set transparency (alpha)
        // We'll get the original text color and apply a new alpha value to it.
        int originalColor = streakText.getCurrentTextColor(); // Use the TextView's current color
        int alpha = 100; // Set alpha value (0-255, where 0 is transparent, 255 is opaque)
        int translucentColor = ColorUtils.setAlphaComponent(originalColor, alpha);

        styledText.setSpan(
                new ForegroundColorSpan(translucentColor),
                numberPart.length(), // Start index of "d"
                fullText.length(),   // End index of "d"
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        return styledText;
    }

    private void refreshStreakUI() {
        TextView streakText = findViewById(R.id.streak);
        ProgressBar streakProgress = findViewById(R.id.streak_progress_bar);
        MaterialCardView streakCard = findViewById(R.id.cardView3_streak);

        // Get the latest streak data (which now includes the updated achievedDays)
        Streak activeStreak = streakManager.getActiveStreak();

        if (activeStreak != null) {
            // An active streak goal exists
            // USE the data directly from the object
            int progressDays = activeStreak.getAchievedDays();
            int targetDays = activeStreak.getTargetDays();

            int percentage = (targetDays > 0) ? (progressDays * 100 / targetDays) : 0;

            streakText.setText(createStyledStreakText(progressDays));
            streakProgress.setProgress(Math.min(percentage, 100));
            streakProgress.setVisibility(View.VISIBLE);
            streakCard.setStrokeColor(ContextCompat.getColor(this, R.color.success_green));

        } else {
            // No active streak, show general contiguous days
            int contiguousDays = streakManager.getContiguousMeditationDays();
            streakText.setText(createStyledStreakText(contiguousDays));
            streakProgress.setVisibility(View.GONE);
            streakCard.setStrokeColor(ContextCompat.getColor(this, android.R.color.transparent));
        }
    }

    // Update date in date_display
    private void updateDateDisplay() {
        String currentDate = new SimpleDateFormat("E, MMMM d, yyy", Locale.getDefault()).format(new Date());
        dateDisplay.setText(currentDate);
    }

    // Start the timer
    private void startTimer() {
        if (!TimerService.isTimerRunning) {
            Intent serviceIntent = new Intent(this, TimerService.class);
            startForegroundService(serviceIntent); // Start the service in the foreground. It was startService before.
            isTimerRunning = true;
            recordButton.setText("Stop");
        } else {
            Log.d("MainActivity", "Timer is already running. Ignoring start request.");
        }
    }

    // Stop the timer and reset the timer display
    private void stopTimer() {
        if (TimerService.isTimerRunning) {
            Intent serviceIntent = new Intent(this, TimerService.class);
            serviceIntent.setAction("STOP_TIMER");
            startService(serviceIntent);
            isTimerRunning = false;
            recordButton.setText("Start");

            // Add elapsed time to today's total
            totalSecondsLogged += secondsElapsed;

            // Update the database
            MeditationLogDatabaseHelper logDbHelper = new MeditationLogDatabaseHelper(this);
            logDbHelper.updateDailyLog(secondsElapsed);
            logDbHelper.close();

            GoalsDatabaseHelper goalsDbHelper = new GoalsDatabaseHelper(this);
            goalsDbHelper.updateGoalsProgress(secondsElapsed);
            goalsDbHelper.close();

            // Refresh UI
            updateTodayTotal();
            updateWeekTotal();
            secondsElapsed = 0; // Reset elapsed time
            updateTimerDisplay();
            displayShortestAndLatestGoal(); // Refresh goal card

            streakManager.updateActiveStreakProgress();
            refreshStreakUI();
        } else {
            Log.d("MainActivity", "Timer is not running. Ignoring stop request.");
        }
    }

    // Timer logic exists in TimerService (runnable)

    // Update timer display
    private void updateTimerDisplay() {
        int hours = secondsElapsed / 3600;
        int minutes = (secondsElapsed % 3600) / 60;
        int seconds = secondsElapsed % 60;
        timerDisplay.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
    }

    // Update today's total time display
    private void updateTodayTotal() {
        int hours = totalSecondsLogged / 3600;
        int minutes = (totalSecondsLogged % 3600) / 60;
        int seconds = totalSecondsLogged % 60;
        todayTotalDisplay.setText(String.format(Locale.getDefault(), "Today: %dh %dm %ds", hours, minutes, seconds));
    }

    private void updateWeekTotal() {
        MeditationLogDatabaseHelper db = new MeditationLogDatabaseHelper(this);
        double hoursThisWeek = db.getHoursForCurrentWeek();

        weekTotalDisplay.setText(
                String.format(Locale.getDefault(),
                        "This Week: %.2f hrs", hoursThisWeek));
        db.close();
    }

    // Add manual entry time to total only
    private void addManualEntry() {
        int hours = parseInput(manualHours);
        int minutes = parseInput(manualMinutes);
        int seconds = parseInput(manualSeconds);

        // ✅ Define additionalSeconds properly
        int additionalSeconds = (hours * 3600) + (minutes * 60) + seconds;

        totalSecondsLogged += additionalSeconds;

        // ✅ Update Meditation Log
        MeditationLogDatabaseHelper logDbHelper = new MeditationLogDatabaseHelper(this);
        logDbHelper.updateDailyLog(additionalSeconds);
        logDbHelper.close();

        // ✅ Update Goals Progress
        GoalsDatabaseHelper goalsDbHelper = new GoalsDatabaseHelper(this);
        goalsDbHelper.updateGoalsProgress(additionalSeconds);
        goalsDbHelper.close();

        updateTodayTotal();
        updateWeekTotal();

        // Reset manual input fields
        manualHours.setText("");
        manualMinutes.setText("");
        manualSeconds.setText("");

        // Hide the keyboard and remove cursor focus after manual entry
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        manualHours.clearFocus();
        manualMinutes.clearFocus();
        manualSeconds.clearFocus();

        // 🟢 Refresh Goal Card
        displayShortestAndLatestGoal(); // Refresh the goal card

        streakManager.updateActiveStreakProgress();
        refreshStreakUI();
    }

    // Parse input from EditText
    private int parseInput(EditText editText) {
        String input = editText.getText().toString();
        return input.isEmpty() ? 0 : Integer.parseInt(input);
    }

    private String formatDate(String dateTime) {
        try {
            // Parse the original date format
            SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = originalFormat.parse(dateTime);

            // Format to "MMM dd"
            SimpleDateFormat newFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
            return newFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return ""; // Return empty string if parsing fails
        }
    }

    private void displayShortestAndLatestGoal() {
        GoalsDatabaseHelper dbHelper = new GoalsDatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Current date in "yyyy-MM-dd" format
        String today = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        // Query to fetch:
        // 1. A goal that includes today's date
        // 2. If none, the shortest and latest future goal
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + GoalsDatabaseHelper.TABLE_GOALS +
                        " WHERE " + GoalsDatabaseHelper.COLUMN_PROGRESS_HOURS + " < " + GoalsDatabaseHelper.COLUMN_TARGET_HOURS +
                        " AND (" +
                        "   (date(" + GoalsDatabaseHelper.COLUMN_START_DATE + ") <= date(?) AND date(" + GoalsDatabaseHelper.COLUMN_END_DATE + ") >= date(?))" + // Prioritize goals that include today's date
                        "   OR (date(" + GoalsDatabaseHelper.COLUMN_START_DATE + ") > date(?))" + // If no valid goal for today, pick the shortest & latest upcoming goal
                        ") ORDER BY " +
                        "   CASE WHEN date(" + GoalsDatabaseHelper.COLUMN_START_DATE + ") <= date(?) AND date(" + GoalsDatabaseHelper.COLUMN_END_DATE + ") >= date(?) THEN 0 ELSE 1 END, " + // Prioritize today's goal
                        GoalsDatabaseHelper.COLUMN_TARGET_HOURS + " ASC, " +
                        GoalsDatabaseHelper.COLUMN_START_DATE + " DESC LIMIT 1",
                new String[]{today, today, today, today, today}
        );


        View goalCardView = findViewById(R.id.cardView_goals_list);

        if (cursor != null && cursor.moveToFirst()) {
            String description = cursor.getString(cursor.getColumnIndexOrThrow(GoalsDatabaseHelper.COLUMN_DESCRIPTION));
            int targetHours = cursor.getInt(cursor.getColumnIndexOrThrow(GoalsDatabaseHelper.COLUMN_TARGET_HOURS));
            String startDateTime = cursor.getString(cursor.getColumnIndexOrThrow(GoalsDatabaseHelper.COLUMN_START_DATE));
            String endDateTime = cursor.getString(cursor.getColumnIndexOrThrow(GoalsDatabaseHelper.COLUMN_END_DATE));

            // Dynamically calculate logged hours
            MeditationLogDatabaseHelper meditationLogDatabaseHelper = new MeditationLogDatabaseHelper(this);
            double loggedHours = meditationLogDatabaseHelper.getLoggedHours(startDateTime, endDateTime);
            int progressPercentage = (int) ((loggedHours * 100.0f) / targetHours);
            if (progressPercentage > 100) progressPercentage = 100;

            // Bind data to the views
            TextView goalTitle = findViewById(R.id.goal_title_home);
            TextView goalDuration = findViewById(R.id.goal_duration_home);
            ProgressBar goalProgressBar = findViewById(R.id.goal_progress_bar2);
            TextView goalProgressPercentage = findViewById(R.id.goal_progress_percentage2);

            // Format dates
            String formattedStartDate = formatDate(startDateTime);
            String formattedEndDate = formatDate(endDateTime);

            goalTitle.setText(description);
            goalDuration.setText("Target: " + targetHours + " hours | " + formattedStartDate + " - " + formattedEndDate);
            goalProgressBar.setProgress(progressPercentage);
            goalProgressPercentage.setText(progressPercentage + "%");

            goalCardView.setVisibility(View.VISIBLE); // Show the card
        } else {
            goalCardView.setVisibility(View.GONE); // Hide the card if no goal exists
        }

        if (cursor != null) {
            cursor.close();
        }
        dbHelper.close();
        db.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Sync the timer state with TimerService
        isTimerRunning = TimerService.isTimerRunning;

        // ✅ Load saved total from shared preferences
        SharedPreferences sp = getSharedPreferences("timer_prefs", MODE_PRIVATE);
        long stored = sp.getLong("timer_start", -1);
        if (stored != -1) {
            // Re-compute elapsed for immediate display
            secondsElapsed = (int) ((System.currentTimeMillis() - stored) / 1000);

            // 🔶 If the service isn’t running any more, start it again
            if (!TimerService.isTimerRunning) {
                Intent revive = new Intent(this, TimerService.class);
                ContextCompat.startForegroundService(this, revive);
            }
            isTimerRunning = true;
        } else {
            isTimerRunning = false;
        }

        recordButton.setText(isTimerRunning ? "Stop" : "Start");
        updateTimerDisplay();

        // Register the broadcast receiver for timer updates
        //LocalBroadcastManager.getInstance(this).registerReceiver(timerUpdateReceiver, new IntentFilter("TIMER_UPDATED"));
        ContextCompat.registerReceiver(this, timerUpdateReceiver, new IntentFilter("TIMER_UPDATED"), ContextCompat.RECEIVER_NOT_EXPORTED);
        updateDateDisplay();  // Refresh date display when returning ot main screen.
        updateTodayTotal(); // Refresh today's total when returning to main screen.
        updateWeekTotal(); // Refresh week's total when returning to main screen.
        updateTimerDisplay(); // Refresh timer display when returning to main screen.
        displayShortestAndLatestGoal(); // Refresh shortest and latest goal when returning to main screen.
        manualHours.clearFocus();
        manualMinutes.clearFocus();
        manualSeconds.clearFocus();
        // Update the streak progress in the database when the app becomes active
        streakManager.updateActiveStreakProgress();
        // Refresh the streak card UI
        refreshStreakUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // When the app is paused, check and update the longest streak using the manager.
        // Also update the progress when the app is paused.
        streakManager.updateActiveStreakProgress();
        refreshStreakUI();
        updateDateDisplay();
        updateTodayTotal(); // Refresh today's total when returning to main screen.
        updateWeekTotal(); // Refresh week's total when returning to main screen.
        manualHours.clearFocus();
        manualMinutes.clearFocus();
        manualSeconds.clearFocus();
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(timerUpdateReceiver);
        unregisterReceiver(timerUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
