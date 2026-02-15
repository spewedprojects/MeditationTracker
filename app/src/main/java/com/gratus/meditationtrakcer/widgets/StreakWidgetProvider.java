package com.gratus.meditationtrakcer.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import com.gratus.meditationtrakcer.MainActivity;
import com.gratus.meditationtrakcer.R;
import com.gratus.meditationtrakcer.datamanagers.StreakManager;
import com.gratus.meditationtrakcer.datamodels.Streak;

public class StreakWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateStreakWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateStreakWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_streak);
        StreakManager streakManager = new StreakManager(context);

        // 1. Get Active Streak Data
        // Ensure progress is fresh
        streakManager.updateActiveStreakProgress();
        Streak activeStreak = streakManager.getActiveStreak();

        if (activeStreak != null) {
            // === ACTIVE STATE ===
            int current = activeStreak.getAchievedDays();
            int target = activeStreak.getTargetDays();
            int progress = (target > 0) ? (current * 100 / target) : 0;

            // 1. Set Background (Active Stroke)
            views.setInt(R.id.streak_widget_layout, "setBackgroundResource", R.drawable.widget_streak_active_background);

            // 2. Show Progress Bar
            views.setViewVisibility(R.id.widget_streak_progress, View.VISIBLE);
            views.setProgressBar(R.id.widget_streak_progress, 100, progress, false);

            // 3. Set Text
            views.setTextViewText(R.id.widget_streak_number, String.valueOf(current));

            // 4. Reset Position (Translation Y = 0dp) -> 0 padding
            int paddingPx = dpToPx(context, 2);
            views.setViewPadding(R.id.streakdaysField, 0, paddingPx, 0, 0);

        } else {
            // === INACTIVE STATE ===
            int contiguousDays = streakManager.getContiguousMeditationDays();

            // 1. Set Background (Plain)
            views.setInt(R.id.streak_widget_layout, "setBackgroundResource", R.drawable.widget_streak_background);

            // 2. Hide Progress Bar
            views.setViewVisibility(R.id.widget_streak_progress, View.GONE);

            // 3. Set Text
            views.setTextViewText(R.id.widget_streak_number, String.valueOf(contiguousDays));

            // 4. Apply Offset (Translation Y = 12dp) -> Simulated via Top Padding
            int paddingPx = dpToPx(context, 12);
            views.setViewPadding(R.id.streakdaysField, 0, paddingPx, 0, 0);
        }

        // Open App on Click
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.streak_widget_layout, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    // Helper to convert dp to px
    private static int dpToPx(Context context, float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
}