package com.gratus.meditationtrakcer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.Log;

// Bar Chart Integration
import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.renderer.BarChartRenderer;
import com.github.mikephil.charting.utils.ViewPortHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MeditationLogDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "meditation_logs.db";
    private static final int DATABASE_VERSION = 3;

    public static final String TABLE_LOGS = "logs";
    public static final String COLUMN_ID = "session_id";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_TOTAL_SECONDS = "total_seconds";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_LOGS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_DATE + " TEXT, " + // Updated to include timestamp
                    COLUMN_TOTAL_SECONDS + " INTEGER DEFAULT 0);";

    public MeditationLogDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            // Step 1: Create a new table with the updated schema
            db.execSQL("CREATE TABLE logs_new (" +
                    "session_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "date TEXT, " + // Now stores timestamps
                    "total_seconds INTEGER DEFAULT 0);");

            // Step 2: Copy data from the old table to the new table, appending '00:00:00' to the date
            db.execSQL("INSERT INTO logs_new (date, total_seconds) " +
                    "SELECT date || ' 00:00:00', total_seconds FROM " + TABLE_LOGS);

            // Step 3: Drop the old table
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOGS);

            // Step 4: Rename the new table to the original name
            db.execSQL("ALTER TABLE logs_new RENAME TO " + TABLE_LOGS);
        }
    }

//    public double getLoggedHours(String startDateTime, String endDateTime) {
//        SQLiteDatabase db = this.getReadableDatabase();
//
//        Cursor cursor = db.rawQuery(
//                "SELECT SUM(" + COLUMN_TOTAL_SECONDS + ") FROM " + TABLE_LOGS +
//                        " WHERE datetime(" + COLUMN_DATE + ") BETWEEN datetime(?) AND datetime(?)",
//                new String[]{startDateTime, endDateTime}
//        );
//
//        double totalHours = 0;
//        if (cursor.moveToFirst()) {
//            totalHours = cursor.getDouble(0) / 3600.0; // Convert seconds to hours
//        }
//        cursor.close();
//
//        Log.d("MeditationLogHelper", "Logged Hours between " + startDateTime + " and " + endDateTime + ": " + totalHours);
//        return totalHours;
//    }

    public double getLoggedHours(String startDateTime, String endDateTime) {
        Log.d("MeditationLogHelper", "getLoggedHours called with Start: " + startDateTime + ", End: " + endDateTime);

        SQLiteDatabase db = this.getReadableDatabase();
        double totalHours = 0;

        try {
            // Query the database
            String query = "SELECT SUM(total_seconds) FROM logs WHERE datetime(date) BETWEEN datetime(?) AND datetime(?)";
            Cursor cursor = db.rawQuery(query, new String[]{startDateTime, endDateTime});

            if (cursor != null && cursor.moveToFirst()) {
                totalHours = cursor.getDouble(0) / 3600.0; // Convert seconds to hours
                Log.d("MeditationLogHelper", "Total Seconds: " + cursor.getDouble(0));
            } else {
                Log.d("MeditationLogHelper", "No matching records found for the given range.");
            }

            if (cursor != null) cursor.close();
        } catch (Exception e) {
            Log.e("MeditationLogHelper", "Error in getLoggedHours: " +e.getMessage());
        } finally {
            db.close();
            Log.d("MeditationLogHelper", "Database connection closed.");
        }

        Log.d("MeditationLogHelper", "Returning Logged Hours: " + totalHours);
        return totalHours;
    }



    // ✅ Update daily log
    public void updateDailyLog(int additionalSeconds) {
        SQLiteDatabase db = this.getWritableDatabase();
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_LOGS +
                        " (" + COLUMN_DATE + ", " + COLUMN_TOTAL_SECONDS + ") VALUES (?, 0)",
                new String[]{now});

        db.execSQL("UPDATE " + TABLE_LOGS +
                " SET " + COLUMN_TOTAL_SECONDS + " = " + COLUMN_TOTAL_SECONDS + " + ?" +
                " WHERE " + COLUMN_DATE + " = ?", new String[]{String.valueOf(additionalSeconds), now});

        db.close();
    }

    // ✅ Get today's logged time
    public int getTodayLoggedSeconds() {
        SQLiteDatabase db = this.getReadableDatabase();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()); // Use date only
        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + COLUMN_TOTAL_SECONDS + ") FROM " + TABLE_LOGS +
                        " WHERE date(" + COLUMN_DATE + ") = date(?)",
                new String[]{today}
        );

        int totalSeconds = 0;
        if (cursor.moveToFirst()) {
            totalSeconds = cursor.getInt(0);
        }

        cursor.close();
        db.close();
        return totalSeconds;
    }


    // Methods to get weekly, monthly, and yearly data

    // Method to get weekly data for a specific date range
    public ArrayList<BarEntry> getWeeklyMeditationDataForDateRange(String startDate) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        float[] dayTotals = new float[7];
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT ((strftime('%w', date) + 6) % 7) AS day, SUM(total_seconds)/3600.0 " +
                        "FROM logs WHERE strftime('%Y-%m-%d', date) >= ? AND strftime('%Y-%m-%d', date) < date(?, '+7 days') " +
                        "GROUP BY day ORDER BY day",
                new String[]{startDate, startDate});

        if (cursor.moveToFirst()) {
            do {
                int dayIndex = cursor.getInt(0);
                float totalHours = cursor.getFloat(1);
                dayTotals[dayIndex] = totalHours;
            } while (cursor.moveToNext());
        }
        cursor.close();

        for (int i = 0; i < 7; i++) {
            entries.add(new BarEntry(i, dayTotals[i]));
        }
        return entries;
    }

    // Method to get total meditation hours for a date range
    public float getTotalWeeklyMeditationHoursForDateRange(String startDate, String endDate) {
        float totalHours = 0;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT SUM(total_seconds)/3600.0 FROM logs WHERE strftime('%Y-%m-%d', date) >= ? AND strftime('%Y-%m-%d', date) < ?",
                new String[]{startDate, endDate});

        if (cursor.moveToFirst()) {
            totalHours = cursor.getFloat(0);
        }
        cursor.close();
        return totalHours;
    }

    // Method to get monthly data for a specific date range
    public ArrayList<BarEntry> getMonthlyMeditationDataForDateRange(String startDate) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        float[] weekTotals = new float[5]; // To hold totals for 5 possible weeks
        SQLiteDatabase db = this.getReadableDatabase();

        // Define the query to fetch dates and their corresponding totals
        String query = "SELECT date, SUM(total_seconds)/3600.0 " +
                "FROM logs WHERE date >= ? AND date < date(?, '+1 month') " +
                "GROUP BY date ORDER BY date";

        Cursor cursor = db.rawQuery(query, new String[]{startDate, startDate});

        if (cursor.moveToFirst()) {
            do {
                String logDate = cursor.getString(0); // Get the date from the database
                float totalHours = cursor.getFloat(1); // Get the total hours for that day

                // Handle both `yyyy-MM-dd` and `yyyy-MM-dd HH:mm:ss` formats
                try {
                    Date date;
                    if (logDate.length() == 10) { // Format `yyyy-MM-dd`
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        date = dateFormat.parse(logDate);
                    } else { // Format `yyyy-MM-dd HH:mm:ss`
                        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        date = dateTimeFormat.parse(logDate);
                    }

                    // Determine the week of the month
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(date);

                    int weekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH) - 1; // Zero-based index
                    if (weekOfMonth >= 0 && weekOfMonth < 5) {
                        weekTotals[weekOfMonth] += totalHours; // Add hours to the respective week
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        // Populate the BarEntry list
        for (int i = 0; i < 5; i++) {
            entries.add(new BarEntry(i, weekTotals[i]));
        }

        return entries;
    }

    // Method to get total meditation hours for a specific month
    public float getTotalMonthlyMeditationHoursForDateRange(String startDate, String endDate) {
        float totalHours = 0;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT SUM(total_seconds)/3600.0 FROM logs WHERE strftime('%Y-%m-%d', date) >= ? AND strftime('%Y-%m-%d', date) < ?",
                new String[]{startDate, endDate});

        if (cursor.moveToFirst()) {
            totalHours = cursor.getFloat(0);
        }
        cursor.close();
        return totalHours;
    }

    // Method to get yearly data for a specific date range
    public ArrayList<BarEntry> getYearlyMeditationDataForDateRange(String startDate) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        float[] monthTotals = new float[12]; // For 12 months in a year
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT strftime('%m', date) AS month, SUM(total_seconds)/3600.0 " +
                        "FROM logs WHERE strftime('%Y-%m-%d', date) >= ? AND strftime('%Y-%m-%d', date) < date(?, '+1 year') " +
                        "GROUP BY month ORDER BY month",
                new String[]{startDate, startDate});


        if (cursor.moveToFirst()) {
            do {
                int monthIndex = Integer.parseInt(cursor.getString(0)) - 1; // Convert month to 0-indexed
                float totalHours = cursor.getFloat(1);
                monthTotals[monthIndex] = totalHours;
            } while (cursor.moveToNext());
        }
        cursor.close();

        for (int i = 0; i < 12; i++) {
            entries.add(new BarEntry(i, monthTotals[i]));
        }
        return entries;
    }

    // Method to get total meditation hours for a specific year
    public float getTotalYearlyMeditationHoursForDateRange(String startDate, String endDate) {
        float totalHours = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(
                    "SELECT SUM(total_seconds)/3600.0 FROM logs WHERE strftime('%Y-%m-%d', date) >= ? AND strftime('%Y-%m-%d', date) < ?",
                    new String[]{startDate, endDate});

            if (cursor.moveToFirst()) {
                totalHours = cursor.getFloat(0);
            }
        } catch (Exception e) {
            e.printStackTrace(); // Log the exception if needed
        } finally {
            if (cursor != null) {
                cursor.close(); // Ensure the cursor is closed
            }
            db.close(); // Ensure the database is closed
        }

        return totalHours;
    }

    public JSONArray getLogsAsJSONArray() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM logs", null);

        JSONArray jsonArray = new JSONArray();
        try {
            while (cursor.moveToNext()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("session_id", cursor.getInt(cursor.getColumnIndexOrThrow("session_id")));
                jsonObject.put("date", cursor.getString(cursor.getColumnIndexOrThrow("date")));
                jsonObject.put("total_seconds", cursor.getInt(cursor.getColumnIndexOrThrow("total_seconds")));
                jsonArray.put(jsonObject);
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally { // New line here....
            if (cursor != null) {
                cursor.close();
            }
            db.close(); // Ensure database is closed ....to here
        }
        return jsonArray;
    }

    public boolean importDataFromJSONArray(JSONArray logsArray) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.beginTransaction();
            db.delete("logs", null, null); // Clear existing data

            for (int i = 0; i < logsArray.length(); i++) {
                JSONObject log = logsArray.getJSONObject(i);

                ContentValues values = new ContentValues();
                values.put("session_id", log.getInt("session_id"));
                values.put("date", log.getString("date"));
                values.put("total_seconds", log.getInt("total_seconds"));

                db.insert("logs", null, values);
            }

            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.endTransaction(); // Ensure transaction is ended
            db.close(); // Ensure database is closed
        }
        return true;
    }


}
