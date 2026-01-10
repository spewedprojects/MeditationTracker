package com.gratus.meditationtrakcer.datamanagers;

import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.gratus.meditationtrakcer.datamodels.Streak;
import com.gratus.meditationtrakcer.databasehelpers.MeditationLogDatabaseHelper;
import com.gratus.meditationtrakcer.databasehelpers.StreakDatabaseHelper;

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

    // THIS IS THE NEW CORE LOGIC METHOD - (08/01/2026)
    /**
     * Finds the relevant streak (Active OR Failed), calculates progress,
     * and updates the status based on whether the gap is filled.
     */
    public void updateActiveStreakProgress() {
        // CHANGED: Use getPotentialStreak() instead of getActiveStreak(). - (08/01/2026)
        // This allows us to find a "Failed" streak that might now be fixed by the backdated entry.
        Streak currentStreak = streakDbHelper.getPotentialStreak();

        if (currentStreak != null) {

            // 1. Calculate contiguous days from the streak's start date
            // The backdated entry will now be included in this calculation
            int contiguousDays = calculateContiguousDays(currentStreak.getStartDate());

            // 2. Check for Success/Failure
            long daysElapsed = getDaysElapsed(currentStreak.getStartDate());

            // Logic:
            // If contiguousDays matches elapsed days (perfect streak)
            // OR contiguousDays matches elapsed days - 1 (we just haven't meditated *today* yet)
            if (contiguousDays >= (daysElapsed - 1)) {

                // THE STREAK IS SAFE (or RESTORED)!

                // 1. Save the new progress
                streakDbHelper.updateAchievedDays(currentStreak.getId(), contiguousDays);

                // 2. FORCE RE-ACTIVATION
                // If it was previously marked "Inactive" (0) because of a missing day,
                // this will flip it back to "Active" (1) now that the gap is filled.
                streakDbHelper.markStreakActive(currentStreak.getId());

            } else {
                // STREAK IS BROKEN
                // Even with the new entry, the gap is too large (missed more than just today).
                streakDbHelper.markStreakInactive(currentStreak.getId());
            }
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
            // --- FORWARD-COUNTING LOGIC (For Active Goal) ---
            try {
                cal.setTime(sdf.parse(fromDateStr));
            } catch (Exception e) {
                return 0;
            }

            // Normalize time to avoid issues
            Calendar todayCal = Calendar.getInstance();
            resetTime(todayCal);
            resetTime(cal);

            while (!cal.after(todayCal)) {
                String dateToCheck = sdf.format(cal.getTime());
                if (meditationDates.contains(dateToCheck)) {
                    contiguousDays++;
                } else {
                    // Break detected. Return what we have so far.
                    return contiguousDays;
                }
                cal.add(Calendar.DATE, 1);
            }
            return contiguousDays;

        } else {
            // --- REVISED BACKWARD-COUNTING LOGIC ---

            // First, check if a log exists for today.
            String todayStr = sdf.format(cal.getTime());
            boolean meditatedToday = meditationDates.contains(todayStr);

            // If the user has NOT meditated today, start the count from yesterday.
            if (!meditatedToday) {
                cal.add(Calendar.DATE, -1);
            }

            // Now, start the backward-counting loop from the correct day (either today or yesterday).
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

    // Helper to calculate days between start date and today (inclusive)
    private long getDaysElapsed(String startDateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date startDate = sdf.parse(startDateStr);
            Date today = new Date();

            // Normalize dates to ignore time
            Calendar startCal = Calendar.getInstance(); startCal.setTime(startDate); resetTime(startCal);
            Calendar todayCal = Calendar.getInstance(); todayCal.setTime(today); resetTime(todayCal);

            long diff = todayCal.getTimeInMillis() - startCal.getTimeInMillis();
            return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) + 1; // +1 to include start day
        } catch (Exception e) {
            return 1;
        }
    }

    private void resetTime(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
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