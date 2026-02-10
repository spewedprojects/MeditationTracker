package com.gratus.meditationtrakcer.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.View;
import android.widget.RemoteViews;

import com.gratus.meditationtrakcer.GoalsActivity;
import com.gratus.meditationtrakcer.R;
import com.gratus.meditationtrakcer.databasehelpers.GoalsDatabaseHelper;
import com.gratus.meditationtrakcer.databasehelpers.MeditationLogDatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class GoalWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateGoalWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateGoalWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_goal);

        // 1. Query DB for Shortest/Latest Goal (Logic copied from MainActivity)
        GoalsDatabaseHelper dbHelper = new GoalsDatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String today = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + GoalsDatabaseHelper.TABLE_GOALS +
                        " WHERE " + GoalsDatabaseHelper.COLUMN_PROGRESS_HOURS + " < " + GoalsDatabaseHelper.COLUMN_TARGET_HOURS +
                        " AND (" +
                        "   (date(" + GoalsDatabaseHelper.COLUMN_START_DATE + ") <= date(?) AND date(" + GoalsDatabaseHelper.COLUMN_END_DATE + ") >= date(?))" +
                        "   OR (date(" + GoalsDatabaseHelper.COLUMN_START_DATE + ") > date(?))" +
                        ") ORDER BY " +
                        "   CASE WHEN date(" + GoalsDatabaseHelper.COLUMN_START_DATE + ") <= date(?) AND date(" + GoalsDatabaseHelper.COLUMN_END_DATE + ") >= date(?) THEN 0 ELSE 1 END, " +
                        GoalsDatabaseHelper.COLUMN_TARGET_HOURS + " ASC, " +
                        GoalsDatabaseHelper.COLUMN_START_DATE + " DESC LIMIT 1",
                new String[]{today, today, today, today, today}
        );

        if (cursor != null && cursor.moveToFirst()) {
            // GOAL FOUND
            String description = cursor.getString(cursor.getColumnIndexOrThrow(GoalsDatabaseHelper.COLUMN_DESCRIPTION));
            double targetHours = cursor.getDouble(cursor.getColumnIndexOrThrow(GoalsDatabaseHelper.COLUMN_TARGET_HOURS));
            String startDateTime = cursor.getString(cursor.getColumnIndexOrThrow(GoalsDatabaseHelper.COLUMN_START_DATE));
            String endDateTime = cursor.getString(cursor.getColumnIndexOrThrow(GoalsDatabaseHelper.COLUMN_END_DATE));

            // Calculate Progress
            MeditationLogDatabaseHelper logHelper = new MeditationLogDatabaseHelper(context);
            double loggedHours = logHelper.getLoggedHours(startDateTime, endDateTime);
            logHelper.close();

            int progressPercentage = (int) ((loggedHours * 100.0f) / targetHours);

            // Format Strings
            String targetFormatted = (targetHours == Math.floor(targetHours)) ?
                    String.format(Locale.US, "%.0f", targetHours) : String.format(Locale.US, "%.1f", targetHours);

            // Calculate "Daily Target" (Simplified for Widget)
            String dailyString = "";
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date sDate = sdf.parse(startDateTime);
                Date eDate = sdf.parse(endDateTime);
                long diff = eDate.getTime() - sDate.getTime();
                long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) + 1;
                dailyString = days + " days";
            } catch (Exception e) { e.printStackTrace(); }

            // Update Views
            views.setViewVisibility(R.id.widget_goal_progress, View.VISIBLE);
            views.setViewVisibility(R.id.widget_goal_percentage, View.VISIBLE);

            views.setTextViewText(R.id.widget_goal_title, description);
            views.setTextViewText(R.id.widget_goal_details, "Target: " + targetFormatted + "h | " + dailyString);
            views.setProgressBar(R.id.widget_goal_progress, 100, progressPercentage, false);
            views.setTextViewText(R.id.widget_goal_percentage, progressPercentage + "%");

        } else {
            // NO ACTIVE GOAL
            views.setTextViewText(R.id.widget_goal_title, "No Active Goal");
            views.setTextViewText(R.id.widget_goal_details, "Create a goal in the app");
            views.setViewVisibility(R.id.widget_goal_progress, View.GONE);
            views.setViewVisibility(R.id.widget_goal_percentage, View.GONE);
        }

        if (cursor != null) cursor.close();
        db.close();
        dbHelper.close();

        // Open GoalsActivity on click
        Intent intent = new Intent(context, GoalsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.goals_widget_layout, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}