package com.gratus.meditationtrakcer;

import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class StreakManager {

    private Context context;
    private StreakDatabaseHelper streakDbHelper;
    private MeditationLogDatabaseHelper logDbHelper;

    public StreakManager(Context context) {
        this.context = context;
        this.streakDbHelper = new StreakDatabaseHelper(context);
        this.logDbHelper = new MeditationLogDatabaseHelper(context);
    }

    public Streak getActiveStreak() {
        return streakDbHelper.getActiveStreak();
    }

    public void startNewStreak(String startDateStr, int targetDays) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date startDate = sdf.parse(startDateStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);
            cal.add(Calendar.DATE, targetDays - 1);
            String endDateStr = sdf.format(cal.getTime());

            streakDbHelper.addStreak(startDateStr, endDateStr, targetDays);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // THIS IS THE NEW CORE LOGIC METHOD
    /**
     * Finds the active streak, calculates its current progress, and updates the database.
     */
    public void updateActiveStreakProgress() {
        Streak activeStreak = getActiveStreak();
        if (activeStreak != null) {
            // Calculate the contiguous days meditated since the streak started
            int contiguousDays = calculateContiguousDays(activeStreak.getStartDate());

            // Save this progress to the database for that specific streak
            streakDbHelper.updateAchievedDays(activeStreak.getId(), contiguousDays);
        }
    }

    public int getContiguousMeditationDays() {
        // Calculate from today backwards
        return calculateContiguousDays(null);
    }

    private int calculateContiguousDays(String fromDateStr) {
        HashSet<String> meditationDates = getMeditationDates();
        if (meditationDates.isEmpty()) {
            return 0;
        }

        int contiguousDays = 0;
        Calendar cal = Calendar.getInstance();

        // If a start date is provided, start counting from there
        if(fromDateStr != null) {
            try {
                cal.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(fromDateStr));
            } catch (Exception e) { /* a format of dd-MM-yyyy may be passed */
                try {
                    cal.setTime(new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(fromDateStr));
                } catch (Exception ex) { return 0; }
            }
        }

        // Loop from the starting day until today
        Calendar todayCal = Calendar.getInstance();
        while(cal.before(todayCal) || cal.equals(todayCal)){
            String dateToCheck = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
            if(meditationDates.contains(dateToCheck)) {
                contiguousDays++;
            } else {
                // For an active streak goal, a single missed day breaks the chain from the start
                if (fromDateStr != null) return contiguousDays;
                    // If just calculating general contiguous days, any break means we are done
                else return contiguousDays;
            }
            cal.add(Calendar.DATE, 1);
        }

        return contiguousDays;
    }


    private HashSet<String> getMeditationDates() {
        HashSet<String> dates = new HashSet<>();
        SQLiteDatabase db = logDbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT DISTINCT date(" + MeditationLogDatabaseHelper.COLUMN_DATE + ") as day FROM " + MeditationLogDatabaseHelper.TABLE_LOGS,
                null
        );
        if (cursor != null && cursor.moveToFirst()) {
            do {
                dates.add(cursor.getString(cursor.getColumnIndexOrThrow("day")));
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return dates;
    }
}