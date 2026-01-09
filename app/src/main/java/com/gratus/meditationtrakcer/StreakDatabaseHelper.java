package com.gratus.meditationtrakcer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StreakDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "streaks.db";
    private static final int DATABASE_VERSION = 2; // Keep version 1 for a clean install, increment for an upgrade

    public static final String TABLE_STREAKS = "streaks";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_START_DATE = "start_date";
    public static final String COLUMN_END_DATE = "end_date";
    public static final String COLUMN_TARGET_DAYS = "target_days";
    public static final String COLUMN_ACHIEVED_DAYS = "achieved_days"; // The new column
    public static final String COLUMN_STATUS = "status"; // New Column: 1 = Active, 0 = Inactive/Failed

    private static final String TABLE_STREAKS_CREATE =
            "CREATE TABLE " + TABLE_STREAKS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_START_DATE + " TEXT, " +
                    COLUMN_END_DATE + " TEXT, " +
                    COLUMN_TARGET_DAYS + " INTEGER, " +
                    COLUMN_ACHIEVED_DAYS + " INTEGER DEFAULT 0, " +
                    COLUMN_STATUS + " INTEGER DEFAULT 1);"; // Default to Active

    public StreakDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_STREAKS_CREATE);
    }

    // ✅ UPDATED: Safe Upgrade Method
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // If upgrading from version 1 to 2, we just want to add the new 'status' column.
            // We do NOT want to drop the table.
            try {
                // Add COLUMN_STATUS if it doesn't exist
                db.execSQL("ALTER TABLE " + TABLE_STREAKS + " ADD COLUMN " + COLUMN_STATUS + " INTEGER DEFAULT 1");
            } catch (Exception e) {
                Log.e("StreakDB", "Error updating DB: " + e.getMessage());
            }
        }
    }

    public void addStreak(String startDate, String endDate, int targetDays) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_START_DATE, startDate);
        values.put(COLUMN_END_DATE, endDate);
        values.put(COLUMN_TARGET_DAYS, targetDays);
        values.put(COLUMN_ACHIEVED_DAYS, 0); // Explicitly set initial achieved days to 0
        values.put(COLUMN_STATUS, 1); // Set as Active
        db.insert(TABLE_STREAKS, null, values);
        db.close();
    }

    // --- (08/01/2026) NEW: Method to delete any streak for future implementation in case I make an interface to browse and delete streak goals similar to regular goals.---
    public void deleteStreak(int streakId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_STREAKS, COLUMN_ID + " = ?", new String[]{String.valueOf(streakId)});
        db.close();
    }

    // (08/01/2026) ✅ NEW: Mark streak as ACTIVE (Used to resurrect a streak if a backdated entry fills the gap)
    public void markStreakActive(int streakId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATUS, 1); // Set to Active
        db.update(TABLE_STREAKS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(streakId)});
        db.close();
    }

    // (08/01/2026) ✅ NEW: Get ANY streak (Active OR Inactive) that covers today
    // We use this when updating progress to check if a failed streak has been fixed.
    public Streak getPotentialStreak() {
        SQLiteDatabase db = this.getReadableDatabase();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Logic: Find a streak that includes today, regardless of status.
        // We ORDER BY STATUS DESC so if (hypothetically) both exist, we pick the Active (1) one first.
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_STREAKS +
                        " WHERE date(?) BETWEEN date(" + COLUMN_START_DATE + ") AND date(" + COLUMN_END_DATE + ")" +
                        " ORDER BY " + COLUMN_STATUS + " DESC, " +
                        COLUMN_START_DATE + " DESC, " +
                        COLUMN_TARGET_DAYS + " ASC LIMIT 1",
                new String[]{today}
        );

        Streak streak = null;
        if (cursor != null && cursor.moveToFirst()) {
            streak = new Streak(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_START_DATE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_END_DATE)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TARGET_DAYS)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACHIEVED_DAYS))
            );
        }

        if (cursor != null) cursor.close();
        db.close();
        return streak;
    }

    // Add this new method to update a streak's progress
    public void updateAchievedDays(int streakId, int achievedDays) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ACHIEVED_DAYS, achievedDays);
        db.update(TABLE_STREAKS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(streakId)});
        db.close();
    }

    // --- (08/01/2026) NEW: Mark streak as inactive (Failed) without deleting ---
    public void markStreakInactive(int streakId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATUS, 0); // Set to Inactive
        db.update(TABLE_STREAKS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(streakId)});
        db.close();
    }

    public Streak getActiveStreak() {
        SQLiteDatabase db = this.getReadableDatabase();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Prioritize goals that: (08/01/2026)
        // 1. Include today's date
        // 2. Are marked ACTIVE (status = 1)
        // 3. Start most recently (Start Date DESC)
        // 4. Have the shortest duration (Target Days ASC)
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_STREAKS +
                        " WHERE date(?) BETWEEN date(" + COLUMN_START_DATE + ") AND date(" + COLUMN_END_DATE + ")" +
                        " AND " + COLUMN_STATUS + " = 1 " +
                        " ORDER BY " + COLUMN_START_DATE + " DESC, " + COLUMN_TARGET_DAYS + " ASC LIMIT 1",
                new String[]{today}
        );

        Streak activeStreak = null;
        if (cursor != null && cursor.moveToFirst()) {
            activeStreak = new Streak(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_START_DATE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_END_DATE)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TARGET_DAYS)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACHIEVED_DAYS)) // Populate the new field
            );
        }

        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return activeStreak;
    }
}