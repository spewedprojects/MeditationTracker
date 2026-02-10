package com.gratus.meditationtrakcer.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import com.gratus.meditationtrakcer.MainActivity;
import com.gratus.meditationtrakcer.R;
import com.gratus.meditationtrakcer.datamanagers.TimerService;

import java.util.Locale;

public class MeditationWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Initial update when widget is placed
        for (int appWidgetId : appWidgetIds) {
            // Passing 0 and false as default since we don't know state yet,
            // but the service will override this quickly if running.
            updateWidget(context, appWidgetManager, appWidgetId, 0, TimerService.isTimerRunning);
        }
    }

    // Helper method called by onUpdate AND by TimerService
    public static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, int seconds, boolean isRunning) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_timer);

        // 1. Format Time
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        String timeString = String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s);
        views.setTextViewText(R.id.widget_timer_display, timeString);

        // 2. Configure Start/Stop Button
        Intent serviceIntent = new Intent(context, TimerService.class);
        if (isRunning) {
            views.setTextViewText(R.id.widget_record_btn, "Stop");
            serviceIntent.setAction(TimerService.ACTION_STOP);
            serviceIntent.putExtra(TimerService.EXTRA_SAVE_DATA, true); // ✅ Tell service to save
        } else {
            views.setTextViewText(R.id.widget_record_btn, "Start");
            serviceIntent.setAction(TimerService.ACTION_START);
        }

        // ✅ CRITICAL FIX: Use getForegroundService for Android O+
        // This allows the widget to start the service even if the app is closed/backgrounded.
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pendingIntent = PendingIntent.getForegroundService(context, 100, serviceIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getService(context, 100, serviceIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }

        views.setOnClickPendingIntent(R.id.widget_record_btn, pendingIntent);

        // 3. Open App on Title Click (Optional but recommended)
        Intent appIntent = new Intent(context, MainActivity.class);
        PendingIntent appPendingIntent = PendingIntent.getActivity(context, 0, appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_timer_display, appPendingIntent);

        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}