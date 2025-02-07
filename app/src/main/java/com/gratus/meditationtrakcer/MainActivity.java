package com.gratus.meditationtrakcer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends BaseActivity {

    private TextView dateDisplay, timerDisplay, todayTotalDisplay;
    private Button recordButton, addEntryButton, moreMenuButton;
    private EditText manualHours, manualMinutes, manualSeconds;
    private boolean isTimerRunning = false;
    private int secondsElapsed = 0;
    private int totalSecondsLogged = 0;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the toolbar and menu button
        setupToolbar(R.id.toolbar2, R.id.menubutton);

        // Initialize UI elements
        dateDisplay = findViewById(R.id.date_display);
        timerDisplay = findViewById(R.id.timer_display);
        todayTotalDisplay = findViewById(R.id.today_total);
        recordButton = findViewById(R.id.record);
        addEntryButton = findViewById(R.id.add_entry);
        manualHours = findViewById(R.id.manual_hours);
        manualMinutes = findViewById(R.id.manual_minutes);
        manualSeconds = findViewById(R.id.manual_seconds);
        moreMenuButton = findViewById(R.id.menubutton);
        displayShortestAndLatestGoal();  // New method call

        // Display today's date
        updateDateDisplay();

        // ✅ Load saved total from database
        MeditationLogDatabaseHelper dbHelper = new MeditationLogDatabaseHelper(this);
        totalSecondsLogged = dbHelper.getTodayLoggedSeconds();
        updateTodayTotal();

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

        // Menu button functionality
        //moreMenuButton.setOnClickListener(v -> openMenu());

        Button gotoGoalsButton = findViewById(R.id.goto_goals);
        gotoGoalsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GoalsActivity.class);
            startActivity(intent);
        });
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

    // Update date in date_display
    private void updateDateDisplay() {
        String currentDate = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(new Date());
        dateDisplay.setText(currentDate);
    }

    // Start the timer
    private void startTimer() {
        if (!TimerService.isTimerRunning) {
            Intent serviceIntent = new Intent(this, TimerService.class);
            startService(serviceIntent);
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

            GoalsDatabaseHelper goalsDbHelper = new GoalsDatabaseHelper(this);
            goalsDbHelper.updateGoalsProgress(secondsElapsed);

            // Refresh UI
            updateTodayTotal();
            secondsElapsed = 0; // Reset elapsed time
            updateTimerDisplay();
            displayShortestAndLatestGoal(); // Refresh goal card
        } else {
            Log.d("MainActivity", "Timer is not running. Ignoring stop request.");
        }
    }

    // Timer logic
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isTimerRunning) {
                secondsElapsed++;
                updateTimerDisplay();
                handler.postDelayed(this, 1000);
            }
        }
    };

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
        todayTotalDisplay.setText(String.format(Locale.getDefault(), "Today's Total: %dh %dm %ds", hours, minutes, seconds));
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

        // ✅ Update Goals Progress
        GoalsDatabaseHelper goalsDbHelper = new GoalsDatabaseHelper(this);
        goalsDbHelper.updateGoalsProgress(additionalSeconds);

        updateTodayTotal();

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

        // Query to fetch the shortest and latest goal
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + GoalsDatabaseHelper.TABLE_GOALS +
                        " WHERE " + GoalsDatabaseHelper.COLUMN_PROGRESS_HOURS + " < " + GoalsDatabaseHelper.COLUMN_TARGET_HOURS +
                        " AND (" + GoalsDatabaseHelper.COLUMN_END_DATE + " IS NULL OR date(" + GoalsDatabaseHelper.COLUMN_END_DATE + ") >= date('now'))" +
                        " ORDER BY " + GoalsDatabaseHelper.COLUMN_TARGET_HOURS + " ASC, " + GoalsDatabaseHelper.COLUMN_START_DATE + " DESC LIMIT 1",
                null
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
        db.close();
    }

    // Open MenuActivity without animation
//    private void openMenu() {
//        Intent intent = new Intent(MainActivity.this, MenuActivity.class);
//        startActivity(intent);
//    } // Commenting this out, yes, it is old method. main activity already extends base activity that includes this functionality.

    @Override
    protected void onResume() {
        super.onResume();

        // Sync the timer state with TimerService
        isTimerRunning = TimerService.isTimerRunning;

        if (isTimerRunning) {
            recordButton.setText("Stop"); // Update button to "Stop"
        } else {
            recordButton.setText("Start"); // Update button to "Start"
        }

        // Register the broadcast receiver for timer updates
        LocalBroadcastManager.getInstance(this).registerReceiver(timerUpdateReceiver, new IntentFilter("TIMER_UPDATED"));
        updateDateDisplay();  // Refresh date display when returning ot main screen.
        updateTodayTotal(); // Refresh today's total when returning to main screen.
        updateTimerDisplay(); // Refresh timer display when returning to main screen.
        displayShortestAndLatestGoal(); // Refresh shortest and latest goal when returning to main screen.
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(timerUpdateReceiver);
    }
}
