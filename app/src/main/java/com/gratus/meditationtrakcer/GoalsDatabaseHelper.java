package com.gratus.meditationtrakcer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GoalsDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "goals.db";
    private static final int DATABASE_VERSION = 5;

    public static final String TABLE_GOALS = "goals";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_TARGET_HOURS = "target_hours";
    public static final String COLUMN_PROGRESS_HOURS = "progress_hours";
    public static final String COLUMN_START_DATE = "start_date";
    public static final String COLUMN_END_DATE = "end_date";

    private static final String TABLE_GOALS_CREATE =
            "CREATE TABLE " + TABLE_GOALS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_DESCRIPTION + " TEXT, " +
                    COLUMN_TARGET_HOURS + " INTEGER, " +
                    COLUMN_PROGRESS_HOURS + " INTEGER DEFAULT 0, " +
                    COLUMN_START_DATE + " TEXT, " + // Stores timestamp
                    COLUMN_END_DATE + " TEXT);";    // Stores timestamp

    public GoalsDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_GOALS_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            // Step 1: Create a new table with the updated schema
            db.execSQL("CREATE TABLE goals_new (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "description TEXT, " +
                    "target_hours INTEGER, " +
                    "progress_hours INTEGER DEFAULT 0, " +
                    "start_date TEXT, " +
                    "end_date TEXT);");

            // Step 2: Copy data from the old table, converting dates to yyyy-MM-dd HH:mm:ss
            db.execSQL("INSERT INTO goals_new (description, target_hours, progress_hours, start_date, end_date) " +
                    "SELECT description, target_hours, progress_hours, " +
                    "substr(start_date, 7, 4) || '-' || substr(start_date, 4, 2) || '-' || substr(start_date, 1, 2) || ' 00:00:00', " + // Convert dd/MM/yyyy to yyyy-MM-dd
                    "substr(end_date, 7, 4) || '-' || substr(end_date, 4, 2) || '-' || substr(end_date, 1, 2) || ' 00:00:00' " + // Convert dd/MM/yyyy to yyyy-MM-dd
                    "FROM " + TABLE_GOALS);

            // Step 3: Drop the old table
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_GOALS);

            // Step 4: Rename the new table
            db.execSQL("ALTER TABLE goals_new RENAME TO " + TABLE_GOALS);
        }
    }

    // ✅ Update progress of goals based on meditation logs
    public void updateGoalsProgress(int additionalSeconds) {
        SQLiteDatabase db = this.getWritableDatabase();
        float additionalHours = additionalSeconds / 3600.0f;

        String today = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        db.execSQL("UPDATE " + TABLE_GOALS +
                        " SET " + COLUMN_PROGRESS_HOURS + " = " + COLUMN_PROGRESS_HOURS + " + ?" +
                        " WHERE date(" + COLUMN_START_DATE + ") <= date(?) AND date(" + COLUMN_END_DATE + ") >= date(?)",
                new String[]{String.valueOf(additionalHours), today, today});

        db.close();
    }

    public JSONArray getGoalsAsJSONArray() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_GOALS, null);

        JSONArray jsonArray = new JSONArray();
        try {
            while (cursor.moveToNext()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("_id", cursor.getInt(cursor.getColumnIndexOrThrow("_id")));
                jsonObject.put("description", cursor.getString(cursor.getColumnIndexOrThrow("description")));
                jsonObject.put("target_hours", cursor.getInt(cursor.getColumnIndexOrThrow("target_hours")));
                jsonObject.put("progress_hours", cursor.getInt(cursor.getColumnIndexOrThrow("progress_hours")));
                jsonObject.put("start_date", cursor.getString(cursor.getColumnIndexOrThrow("start_date"))); // Includes timestamp
                jsonObject.put("end_date", cursor.getString(cursor.getColumnIndexOrThrow("end_date")));     // Includes timestamp
                jsonArray.put(jsonObject);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return jsonArray;
    }

    public boolean importDataFromJSONArray(JSONArray goalsArray) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.beginTransaction();
            db.delete("goals", null, null); // Clear existing data

            for (int i = 0; i < goalsArray.length(); i++) {
                JSONObject goal = goalsArray.getJSONObject(i);

                ContentValues values = new ContentValues();
                values.put("_id", goal.getInt("_id"));
                values.put("description", goal.getString("description"));
                values.put("target_hours", goal.getInt("target_hours"));
                values.put("progress_hours", goal.getInt("progress_hours"));
                values.put("start_date", goal.getString("start_date"));
                values.put("end_date", goal.getString("end_date"));

                db.insert("goals", null, values);
            }

            db.setTransactionSuccessful();
            db.endTransaction();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
