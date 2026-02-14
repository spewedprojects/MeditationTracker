package com.gratus.meditationtrakcer.datamanagers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent; // ⬅️ new; used for tap on notification to launch app main screen
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
//import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.SharedPreferences;     // ⬅️ new

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.gratus.meditationtrakcer.MainActivity;
import com.gratus.meditationtrakcer.databasehelpers.GoalsDatabaseHelper;
import com.gratus.meditationtrakcer.databasehelpers.MeditationLogDatabaseHelper;
import com.gratus.meditationtrakcer.utils.WidgetUpdateHelper;
import com.gratus.meditationtrakcer.widgets.TimerWidgetProvider;
import com.gratus.meditationtrakcer.R;

import java.util.Locale;

public class TimerService extends Service {
    // Unique channel ID for the foreground service notification
    public static final String CHANNEL_ID = "MeditationTimerChannel";
    public static final String ACTION_START = "START_TIMER"; // Explicit Start Action
    public static final String ACTION_STOP = "STOP_TIMER";   // Explicit Stop Action
    public static final String EXTRA_SAVE_DATA = "SAVE_DATA"; // New Flag

    // Handler to manage periodic timer updates
    private Handler handler = new Handler();

    // Indicates whether the timer is running
    public static boolean isTimerRunning = false; // Static variable to track timer state

    // Tracks the total elapsed time in seconds
    private int secondsElapsed = 0;
    private long startTime = 0;
    private static final String SP_NAME = "timer_prefs";
    private static final String KEY_START_TIME = "timer_start";
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences(SP_NAME, MODE_PRIVATE);     // ⬅️ new
        // Create the notification channel for the service
        createNotificationChannel();

        // Start the service in the foreground with an initial notification
        startForeground(1, getNotification("Meditation Timer Running..."));
    }

    private void sendTimeUpdate() {
        Intent intent = new Intent("TIMER_UPDATED");
        intent.putExtra("secondsElapsed", secondsElapsed); // Send elapsed time
        // LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        intent.setPackage(getPackageName()); // This is the key change to keep the broadcast local
        sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : "";

        if (ACTION_START.equals(action)) {
            startForeground(1, getNotification("Meditation Timer Running..."));
            startTimer();
        } else if (ACTION_STOP.equals(action)) {
            // Check if we should save data (True for Widget, False for App)
            boolean shouldSave = intent.getBooleanExtra(EXTRA_SAVE_DATA, false);
            stopTimer(shouldSave);
            stopForeground(true);
            stopSelf();
        } else {
            // Default behavior (e.g. generic start)
            if (!isTimerRunning) {
                startForeground(1, getNotification("Meditation Timer Running..."));
                startTimer();
            }
        }
        updateWidgets();
        return START_STICKY;
    }

    /**
     * Starts the timer and schedules periodic updates.
     */
    private void startTimer() {
        if (!isTimerRunning) {
            // If we have a stored stamp, pick it up – otherwise start new
            if (prefs.contains(KEY_START_TIME)) {
                startTime = prefs.getLong(KEY_START_TIME, System.currentTimeMillis());
            } else {
                startTime = System.currentTimeMillis();
                prefs.edit().putLong(KEY_START_TIME, startTime).apply();
            }

            // Rebuild secondsElapsed so notification & UI show the right figure
            secondsElapsed = (int) ((System.currentTimeMillis() - startTime) / 1000);
            isTimerRunning = true;
            handler.postDelayed(timerRunnable, 1000);
        }
    }

    /**
     * Stops the timer and removes any scheduled updates.
     */
    private void stopTimer(boolean saveToDb) {
        if (isTimerRunning) {
            long endTime = System.currentTimeMillis();

            // ✅ FIX 1: Prevent doubling. Recalculate total time from start, don't add to existing.
            int finalSeconds = (int) ((endTime - startTime) / 1000);

            // ✅ FIX 2: Save to Database if requested (Widget scenario)
            if (saveToDb) {
                // 1. Log the session
                MeditationLogDatabaseHelper dbHelper = new MeditationLogDatabaseHelper(this);
                dbHelper.logSessionWithTimestamp(startTime, finalSeconds);
                dbHelper.close();

                // 2. Update Goals
                GoalsDatabaseHelper goalsDbHelper = new GoalsDatabaseHelper(this);
                goalsDbHelper.updateGoalsProgress(finalSeconds);
                goalsDbHelper.close();

                // 3. Update Streak
                StreakManager streakManager = new StreakManager(this);
                streakManager.updateActiveStreakProgress();
            }
            WidgetUpdateHelper.updateAllWidgets(this);

            isTimerRunning = false;
            handler.removeCallbacks(timerRunnable);
            prefs.edit().remove(KEY_START_TIME).apply();

            // 4. ✅ CRITICAL FIX: Reset the global counter to 0
            secondsElapsed = 0;

            // 5. Broadcast the "0" state to both App and Widget
            sendTimeUpdate(); // Will now send 0 to MainActivity
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isTimerRunning = false; // Reset the state when the service is destroyed
    }

    /**
     * Formats the elapsed time in seconds to HH:mm:ss format.
     *
     * @param seconds The total elapsed time in seconds.
     * @return A formatted time string in HH:mm:ss format.
     */
    private String formatTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;

        // Return the time in HH:mm:ss format
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
    }


    /**
     * Runnable that increments the timer and updates the notification.
     */
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isTimerRunning) {
                long now = System.currentTimeMillis();
                // Rebuild the elapsed time from the persisted start-stamp
                long startTime = prefs.getLong(KEY_START_TIME, now);
                secondsElapsed = (int) ((now - startTime) / 1000);
                Log.d("TimerService", "Timer running: secondsElapsed = " + secondsElapsed);
                updateNotification("Elapsed Time: " + formatTime(secondsElapsed)); // Update the notification
                sendTimeUpdate(); // Send the update to MainActivity

                // NEW: Push update to the widget
                updateWidgets();  // Update Widget
                handler.postDelayed(this, 1000); // Schedule the next update in 1 second
            }
        }
    };

    // ✅ NEW: Helper to update the widget directly from Service
    public void updateWidgets() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

        // 1. Update Timer Widgets
        ComponentName thisWidget = new ComponentName(this, TimerWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        // Call the static update method in your Provider
        // We pass 'secondsElapsed' and 'isTimerRunning' state
        for (int appWidgetId : appWidgetIds) {
            TimerWidgetProvider.updateTimerWidget(this, appWidgetManager, appWidgetId, secondsElapsed, isTimerRunning);
        }

        // 2. ✅ Update Goal Widgets (Only on STOP, or periodically, but updating here is safe)
        ComponentName goalWidget = new ComponentName(this, com.gratus.meditationtrakcer.widgets.GoalWidgetProvider.class);
        int[] goalIds = appWidgetManager.getAppWidgetIds(goalWidget);
        for (int id : goalIds) {
            // We can reuse the static update method we created
            com.gratus.meditationtrakcer.widgets.GoalWidgetProvider.updateGoalWidget(this, appWidgetManager, id);
        }

        // 3. ✅ Update Streak Widgets
        ComponentName streakWidget = new ComponentName(this, com.gratus.meditationtrakcer.widgets.StreakWidgetProvider.class);
        int[] streakIds = appWidgetManager.getAppWidgetIds(streakWidget);
        for (int id : streakIds) {
            com.gratus.meditationtrakcer.widgets.StreakWidgetProvider.updateStreakWidget(this, appWidgetManager, id);
        }
    }

    /** PendingIntent that re-opens MainActivity when the notification is tapped */
    private PendingIntent buildContentIntent() {
        Intent launch = new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;   // required on API 23+
        }

        return PendingIntent.getActivity(this, 0, launch, flags);
    }

    /**
     * Updates the foreground service notification with the latest timer value.
     *
     * @param contentText The text to display in the notification.
     */
    private void updateNotification(String contentText) {
        Notification notification = getNotification(contentText);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(1, notification); // Update the existing notification
        }
    }

    /**
     * Creates a notification for the service.
     *
     * @param contentText The text to display in the notification.
     * @return The notification instance.
     */
    private Notification getNotification(String contentText) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Meditation Tracker") // Notification title
                .setContentText(contentText) // Notification content
                .setSmallIcon(R.drawable.mtapp_icon2) // Icon to display
                .setOngoing(true) // Prevent the user from swiping it away
                .setContentIntent(buildContentIntent())   // ⬅️ key line
                .build();
    }

    /**
     * Creates a notification channel (required for Android 8.0+).
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Meditation Timer Channel", // Channel name
                    NotificationManager.IMPORTANCE_LOW // Low priority (no sound or vibration)
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel); // Register the channel
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // This service doesn't support binding
        return null;
    }
}
