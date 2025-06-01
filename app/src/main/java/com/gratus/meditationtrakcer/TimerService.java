package com.gratus.meditationtrakcer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.SharedPreferences;     // ⬅️ new

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.Locale;

public class TimerService extends Service {
    // Unique channel ID for the foreground service notification
    public static final String CHANNEL_ID = "MeditationTimerChannel";

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
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Check if the intent has a "STOP_TIMER" action to stop the timer
        Log.d("TimerService", "onStartCommand called");
        if (intent.getAction() != null && intent.getAction().equals("STOP_TIMER")) {
            stopTimer(); // Stop the timer
            stopSelf(); // Stop the service
        } else {
            // Start the timer if no "STOP_TIMER" action is provided
            startTimer();
        }
        // Keep the service running until explicitly stopped
        return START_STICKY;
    }

    /**
     * Starts the timer and schedules periodic updates.
     */
    private void startTimer() {
        if (!isTimerRunning) {
            startTime = System.currentTimeMillis(); // Record the start time
            prefs.edit().putLong(KEY_START_TIME, startTime).apply(); // ⬅️ save
            isTimerRunning = true;
            handler.postDelayed(timerRunnable, 1000);
        }
    }

    /**
     * Stops the timer and removes any scheduled updates.
     */
    private void stopTimer() {
        if (isTimerRunning) {
            long endTime = System.currentTimeMillis();
            secondsElapsed += (int) ((endTime - startTime) / 1000); // Calculate total elapsed time
            isTimerRunning = false;
            handler.removeCallbacks(timerRunnable);
            prefs.edit().remove(KEY_START_TIME).apply();   // ⬅️ wipe
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
                secondsElapsed++; // Increment the elapsed time
                //Log.d("TimerService", "Timer running: secondsElapsed = " + secondsElapsed);
                updateNotification("Elapsed Time: " + formatTime(secondsElapsed)); // Update the notification
                sendTimeUpdate(); // Send the update to MainActivity
                handler.postDelayed(this, 1000); // Schedule the next update in 1 second
            }
        }
    };

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
                    NotificationManager.IMPORTANCE_HIGH // Low priority (no sound or vibration)
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
