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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        if (fromDateStr != null) {
            // --- FORWARD-COUNTING LOGIC (For an active streak goal) ---
            // This part remains the same, counting from a start date forwards.
            try {
                cal.setTime(sdf.parse(fromDateStr));
            } catch (Exception e) {
                try {
                    cal.setTime(new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(fromDateStr));
                } catch (Exception ex) {
                    return 0;
                }
            }

            Calendar todayCal = Calendar.getInstance();
            while (cal.before(todayCal) || cal.equals(todayCal)) {
                String dateToCheck = sdf.format(cal.getTime());
                if (meditationDates.contains(dateToCheck)) {
                    contiguousDays++;
                } else {
                    // A single missed day breaks the chain for the active goal.
                    return contiguousDays;
                }
                cal.add(Calendar.DATE, 1);
            }
            return contiguousDays;

        } else {
            // --- NEW BACKWARD-COUNTING LOGIC (For general streak display) ---
            // This loop starts from today and goes back in time.
            while (true) {
                String dateToCheck = sdf.format(cal.getTime());
                if (meditationDates.contains(dateToCheck)) {
                    contiguousDays++;
                    // Move to the previous day to continue checking.
                    cal.add(Calendar.DATE, -1);
                } else {
                    // The first day without a meditation log is found, so the streak is broken.
                    // Stop counting and exit the loop.
                    break;
                }
            }
            return contiguousDays;
        }
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