package com.gratus.meditationtrakcer.utils;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.gratus.meditationtrakcer.datamanagers.TimerService;
import com.gratus.meditationtrakcer.widgets.GoalWidgetProvider;
import com.gratus.meditationtrakcer.widgets.TimerWidgetProvider;
import com.gratus.meditationtrakcer.widgets.StreakWidgetProvider;

public class WidgetUpdateHelper {

    /**
     * Triggers an update for ALL widgets (Timer, Goal, Streak).
     * Call this whenever data changes in the database (Add/Delete Goal, Manual Entry, etc.)
     */
    public static void updateAllWidgets(Context context) {
        AppWidgetManager man = AppWidgetManager.getInstance(context);

        // 1. Update Timer Widget
        // We pass 'false' for isRunning and 0 for seconds because this helper is usually called
        // when we modify DB data (Manual Entry/Goal Change), not during a live ticking timer.
        // If the timer IS running, the Service will overwrite this in the next second anyway.
        int[] timerIds = man.getAppWidgetIds(new ComponentName(context, TimerWidgetProvider.class));
        for (int id : timerIds) {
            TimerWidgetProvider.updateTimerWidget(context, man, id, 0, TimerService.isTimerRunning);
        }

        // 2. Update Goal Widget
        int[] goalIds = man.getAppWidgetIds(new ComponentName(context, GoalWidgetProvider.class));
        for (int id : goalIds) {
            GoalWidgetProvider.updateGoalWidget(context, man, id);
        }

        // 3. Update Streak Widget
        int[] streakIds = man.getAppWidgetIds(new ComponentName(context, StreakWidgetProvider.class));
        for (int id : streakIds) {
            StreakWidgetProvider.updateStreakWidget(context, man, id);
        }
    }
}