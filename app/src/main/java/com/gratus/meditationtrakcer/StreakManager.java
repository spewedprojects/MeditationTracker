package com.gratus.meditationtrakcer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class StreakManager {
    private final MeditationLogDatabaseHelper dbHelper;

    public StreakManager(Context context) {
        dbHelper = new MeditationLogDatabaseHelper(context);
    }

    // Start or overwrite streak
    public void setNewStreak(String startDate, int targetDays) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Delete existing
        db.delete("streak_log", null, null);

        // Calculate end date
        Calendar calendar = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            calendar.setTime(sdf.parse(startDate));
            calendar.add(Calendar.DATE, targetDays - 1);
            String endDate = sdf.format(calendar.getTime());

            ContentValues values = new ContentValues();
            values.put("start_date", startDate);
            values.put("end_date", endDate);
            values.put("target_days", targetDays);
            values.put("longest_streak", getLongestStreak()); // preserved

            db.insert("streak_log", null, values);
        } catch (Exception e) {
            e.printStackTrace();
        }

        db.close();
    }

    public boolean isStreakActive() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM streak_log", null);
        boolean active = false;
        if (cursor.moveToFirst()) {
            String endDate = cursor.getString(cursor.getColumnIndexOrThrow("end_date"));
            active = compareToday(endDate) <= 0; // today <= end date
        }
        cursor.close();
        db.close();
        return active;
    }

    public int getCurrentStreakProgress(String todayDate) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int days = 0;
        Cursor cursor = db.rawQuery("SELECT start_date FROM streak_log", null);
        if (cursor.moveToFirst()) {
            String startDate = cursor.getString(cursor.getColumnIndexOrThrow("start_date"));
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                long diff = sdf.parse(todayDate).getTime() - sdf.parse(startDate).getTime();
                days = (int) (diff / (1000 * 60 * 60 * 24)) + 1;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        cursor.close();
        db.close();
        return days;
    }

    public void resetStreak() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("streak_log", null, null);
        db.close();
    }

    public int getLongestStreak() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT longest_streak FROM streak_log ORDER BY id DESC LIMIT 1", null);
        int longest = 0;
        if (cursor.moveToFirst()) {
            longest = cursor.getInt(cursor.getColumnIndexOrThrow("longest_streak"));
        }
        cursor.close();
        db.close();
        return longest;
    }

    public String[] getStartAndEndDate() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT start_date, end_date FROM streak_log", null);
        String[] dates = new String[2];
        if (cursor.moveToFirst()) {
            dates[0] = cursor.getString(cursor.getColumnIndexOrThrow("start_date"));
            dates[1] = cursor.getString(cursor.getColumnIndexOrThrow("end_date"));
        }
        cursor.close();
        db.close();
        return dates;
    }

    private int compareToday(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar today = Calendar.getInstance();
            Calendar date = Calendar.getInstance();
            date.setTime(sdf.parse(dateStr));
            return today.getTime().compareTo(date.getTime());
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
    }
}
