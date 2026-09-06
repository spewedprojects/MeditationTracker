package com.gratus.meditationtrakcer.datamanagers;

import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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

    /**
     * Starts a new streak goal with a specified start date and target duration.
     *
     * @param startDateStr The start date in "yyyy-MM-dd" format.
     * @param targetDays   The number of target days for the streak.
     */
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

    /**
     * Calculates the current contiguous meditation days counting backwards from today.
     * If the user has not meditated today, it starts counting from yesterday.
     *
     * @return The number of consecutive days meditated.
     */
    public int getContiguousMeditationDays() {
        // Calculate from today backwards
        return calculateContiguousDays(null);
    }

    /**
     * Calculates the length of the longest meditation streak ever recorded.
     * A streak is defined as consecutive days with at least one meditation log.
     *
     * @return The number of days in the longest streak.
     */
    public int getLongestStreak() {
        HashSet<String> dateSet = getMeditationDates();
        if (dateSet.isEmpty()) return 0;

        List<Date> dates = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Convert Strings to Dates for sorting
        for (String s : dateSet) {
            try {
                dates.add(sdf.parse(s));
            } catch (Exception e) { e.printStackTrace(); }
        }

        Collections.sort(dates);

        int maxStreak = 1;
        int currentStreak = 1;

        // Iterate through sorted dates
        for (int i = 0; i < dates.size() - 1; i++) {
            long diffInMillis = dates.get(i + 1).getTime() - dates.get(i).getTime();
            long diffInDays = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);

            if (diffInDays == 1) {
                // Consecutive day
                currentStreak++;
            } else if (diffInDays > 1) {
                // Break in streak
                maxStreak = Math.max(maxStreak, currentStreak);
                currentStreak = 1;
            }
            // If diffInDays == 0 (duplicate entries), do nothing
        }

        return Math.max(maxStreak, currentStreak);
    }

    /**
     * Calculates and formats the date range of the longest meditation streak.
     * If multiple streaks share the same maximum length, the most recent one is returned.
     *
     * @return A formatted string representing the date range (e.g., "Aug 1 - 26, 26").
     *         Returns an empty string if no meditation logs exist.
     */
    public String getLongestStreakDateRange() {
        HashSet<String> dateSet = getMeditationDates();
        if (dateSet.isEmpty()) return "";

        List<Date> dates = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (String s : dateSet) {
            try {
                dates.add(sdf.parse(s));
            } catch (Exception e) { e.printStackTrace(); }
        }

        if (dates.isEmpty()) return "";
        Collections.sort(dates);

        int maxStreak = 1;
        int currentStreak = 1;
        Date currentStart = dates.get(0);
        Date maxStart = dates.get(0);
        Date maxEnd = dates.get(0);

        for (int i = 0; i < dates.size() - 1; i++) {
            long diffInMillis = dates.get(i + 1).getTime() - dates.get(i).getTime();
            long diffInDays = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);

            if (diffInDays == 1) {
                currentStreak++;
            } else if (diffInDays > 1) {
                if (currentStreak >= maxStreak) {
                    maxStreak = currentStreak;
                    maxStart = currentStart;
                    maxEnd = dates.get(i);
                }
                currentStreak = 1;
                currentStart = dates.get(i + 1);
            }
        }

        // Final check after loop
        if (currentStreak >= maxStreak) {
            maxStart = currentStart;
            maxEnd = dates.get(dates.size() - 1);
        }

        return formatStreakRange(maxStart, maxEnd);
    }

    /**
     * Formats a date range according to the following rules:
     * - Same month: "Aug 1 - 26, 26"
     * - Same year, different months: "Aug 1 - Sep 2, 26"
     * - Different years: "Aug 1, 25 - Jan 1, 26"
     * - Single day: "Aug 1, 26"
     *
     * @param start The start date of the streak.
     * @param end The end date of the streak.
     * @return A formatted string representing the date range.
     */
    private String formatStreakRange(Date start, Date end) {
        Calendar calStart = Calendar.getInstance();
        calStart.setTime(start);
        Calendar calEnd = Calendar.getInstance();
        calEnd.setTime(end);

        int startYear = calStart.get(Calendar.YEAR);
        int endYear = calEnd.get(Calendar.YEAR);
        int startMonth = calStart.get(Calendar.MONTH);
        int endMonth = calEnd.get(Calendar.MONTH);

        SimpleDateFormat fullDate = new SimpleDateFormat("MMM d, yy", Locale.getDefault());
        SimpleDateFormat monthDay = new SimpleDateFormat("MMM d", Locale.getDefault());
        SimpleDateFormat dayOnly = new SimpleDateFormat("d", Locale.getDefault());
        SimpleDateFormat yearOnly = new SimpleDateFormat("yy", Locale.getDefault());

        if (startYear != endYear) {
            // Different years: Aug 1, 25 - Jan 1, 26
            return fullDate.format(start) + " - " + fullDate.format(end);
        } else if (startMonth != endMonth) {
            // Same year, different months: Aug 1 - Sep 2, 26
            return monthDay.format(start) + " - " + fullDate.format(end);
        } else if (calStart.get(Calendar.DAY_OF_MONTH) != calEnd.get(Calendar.DAY_OF_MONTH)) {
            // Same month: Aug 1 - 26, 26
            return monthDay.format(start) + " - " + dayOnly.format(end) + ", " + yearOnly.format(end);
        } else {
            // Single day: Aug 1, 26
            return fullDate.format(start);
        }
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