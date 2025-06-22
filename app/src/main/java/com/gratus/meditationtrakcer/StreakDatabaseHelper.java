package com.gratus.meditationtrakcer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StreakDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "streaks.db";
    private static final int DATABASE_VERSION = 1; // Keep version 1 for a clean install, increment for an upgrade

    public static final String TABLE_STREAKS = "streaks";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_START_DATE = "start_date";
    public static final String COLUMN_END_DATE = "end_date";
    public static final String COLUMN_TARGET_DAYS = "target_days";
    public static final String COLUMN_ACHIEVED_DAYS = "achieved_days"; // The new column

    private static final String TABLE_STREAKS_CREATE =
            "CREATE TABLE " + TABLE_STREAKS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_START_DATE + " TEXT, " +
                    COLUMN_END_DATE + " TEXT, " +
                    COLUMN_TARGET_DAYS + " INTEGER, " +
                    COLUMN_ACHIEVED_DAYS + " INTEGER DEFAULT 0);"; // Add column to creation string

    public StreakDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_STREAKS_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For a real-world app, you would use ALTER TABLE here to preserve data.
        // For development, dropping and recreating is simpler.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STREAKS);
        onCreate(db);
    }

    public void addStreak(String startDate, String endDate, int targetDays) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_START_DATE, startDate);
        values.put(COLUMN_END_DATE, endDate);
        values.put(COLUMN_TARGET_DAYS, targetDays);
        values.put(COLUMN_ACHIEVED_DAYS, 0); // Explicitly set initial achieved days to 0
        db.insert(TABLE_STREAKS, null, values);
        db.close();
    }

    // Add this new method to update a streak's progress
    public void updateAchievedDays(int streakId, int achievedDays) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ACHIEVED_DAYS, achievedDays);
        db.update(TABLE_STREAKS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(streakId)});
        db.close();
    }

    public Streak getActiveStreak() {
        SQLiteDatabase db = this.getReadableDatabase();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_STREAKS +
                        " WHERE date(?) BETWEEN date(" + COLUMN_START_DATE + ") AND date(" + COLUMN_END_DATE + ")" +
                        " ORDER BY " + COLUMN_START_DATE + " DESC LIMIT 1",
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