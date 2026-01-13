package com.gratus.meditationtrakcer.utils;

import android.content.Context;
import com.gratus.meditationtrakcer.databasehelpers.MeditationLogDatabaseHelper;
import com.gratus.meditationtrakcer.models.MeditationReportData;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ReportGenerator {

    public static MeditationReportData generateReport(Context context, String startDate, String endDate, boolean isYearly, String title) {
        MeditationLogDatabaseHelper db = new MeditationLogDatabaseHelper(context);
        MeditationReportData data = new MeditationReportData();

        data.reportId = (isYearly ? "Y_" : "M_") + startDate.split("-")[0] + (isYearly ? "" : "_" + startDate.split("-")[1]);
        data.title = title;
        data.isYearly = isYearly;
        data.generatedTimestamp = System.currentTimeMillis();

        // 1. Fetch Raw Data
        ArrayList<MeditationLogDatabaseHelper.SessionData> sessions = db.getSessionDataForRange(startDate, endDate);
        data.totalSessions = sessions.size();

        if (sessions.isEmpty()) return data;

        // 2. Calculate Totals & Averages
        long totalSeconds = 0;
        for (MeditationLogDatabaseHelper.SessionData s : sessions) totalSeconds += s.durationSeconds;

        data.totalHours = totalSeconds / 3600f;
        data.avgSessionLength = (data.totalSessions > 0) ? (int) ((totalSeconds / 60) / data.totalSessions) : 0;

        // 3. Complex Calculations (Streaks, Gaps, Monthly Activity)
        processDatesAndActivity(data, sessions, startDate, endDate);

        // 4. Charts Data
        processCharts(data, sessions);

        return data;
    }

    private static void processDatesAndActivity(MeditationReportData data, ArrayList<MeditationLogDatabaseHelper.SessionData> sessions, String startStr, String endStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Set<String> uniqueDateStrings = new HashSet<>();
        List<Long> uniqueDayTimestamps = new ArrayList<>();

        // --- Prepare Monthly Aggregation (For Yearly Reports) ---
        // Map: Month Index (0-11) -> Total Hours
        Map<Integer, Float> monthlyHours = new HashMap<>();
        Calendar cal = Calendar.getInstance();

        // --- Get Unique Active Days & Aggregate Monthly Data ---
        for (MeditationLogDatabaseHelper.SessionData s : sessions) {
            // Unique Days
            String d = sdf.format(new Date(s.timestamp));
            if (!uniqueDateStrings.contains(d)) {
                uniqueDateStrings.add(d);
                try { uniqueDayTimestamps.add(sdf.parse(d).getTime()); } catch (Exception e) {}
            }

            // Monthly Aggregation (Only if Yearly)
            if (data.isYearly) {
                cal.setTimeInMillis(s.timestamp);
                int month = cal.get(Calendar.MONTH);
                float hours = s.durationSeconds / 3600f;
                monthlyHours.put(month, monthlyHours.getOrDefault(month, 0f) + hours);
            }
        }
        Collections.sort(uniqueDayTimestamps);

        // --- A. Consistency ---
        long diffInMillies = 0;
        try {
            Date s = sdf.parse(startStr);
            Date e = sdf.parse(endStr);
            diffInMillies = Math.abs(e.getTime() - s.getTime());
        } catch (Exception ex) {}

        int totalDaysInRange = (int) TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS) + 1;
        int activeDays = uniqueDayTimestamps.size();

        data.consistencyScore = (totalDaysInRange > 0) ? (int) (((float) activeDays / totalDaysInRange) * 100) : 0;
        data.daysNotMeditated = totalDaysInRange - activeDays;

        // --- B. Streaks & Stability (UPDATED LOGIC) ---
        int currentStreak = 0;
        int maxStreak = 0;

        // Variables for Streak Stability (Avg Streak Length)
        List<Integer> streakLengths = new ArrayList<>();

        // Variables for Avg Session Gap
        long totalGapDays = 0;
        int gapCount = 0;

        for (int i = 0; i < uniqueDayTimestamps.size(); i++) {
            if (i == 0) {
                currentStreak = 1;
                continue;
            }

            long prev = uniqueDayTimestamps.get(i - 1);
            long curr = uniqueDayTimestamps.get(i);
            long diffDays = TimeUnit.DAYS.convert(curr - prev, TimeUnit.MILLISECONDS);

            if (diffDays == 1) {
                // Consecutive day -> Extend streak
                currentStreak++;
            } else {
                // Break detected
                // 1. Record the finished streak length
                streakLengths.add(currentStreak);
                maxStreak = Math.max(maxStreak, currentStreak);
                currentStreak = 1;

                // 2. Calculate Gap
                // Example: Mon(active) -> Wed(active). diffDays=2. Gap=1 (Tue).
                long gap = diffDays - 1;
                totalGapDays += gap;
                gapCount++;
            }
        }
        // Capture the final streak
        streakLengths.add(currentStreak);
        maxStreak = Math.max(maxStreak, currentStreak);
        data.bestStreak = maxStreak;

        // Metric 1: Streak Stability = Average Streak Length
        if (!streakLengths.isEmpty()) {
            int sumStreaks = 0;
            for (int len : streakLengths) sumStreaks += len;
            data.streakStability = (float) sumStreaks / streakLengths.size();
        } else {
            data.streakStability = 0;
        }

        // Metric 2: Avg Session Gap = Average gap between active days
        data.avgSessionGap = (gapCount > 0) ? (float) totalGapDays / gapCount : 0;

        // --- C. Weeks Not Meditated ---
        try {
            cal.setTime(sdf.parse(startStr));
            cal.setFirstDayOfWeek(Calendar.MONDAY);
            while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                cal.add(Calendar.DAY_OF_YEAR, -1);
            }

            int zeroWeeks = 0;
            long endMillis = sdf.parse(endStr).getTime();

            while (cal.getTimeInMillis() <= endMillis) {
                long weekStart = cal.getTimeInMillis();
                cal.add(Calendar.DAY_OF_YEAR, 7);
                long weekEnd = cal.getTimeInMillis();

                boolean meditatedThisWeek = false;
                for (Long dayTs : uniqueDayTimestamps) {
                    if (dayTs >= weekStart && dayTs < weekEnd) {
                        meditatedThisWeek = true;
                        break;
                    }
                }
                if (!meditatedThisWeek) zeroWeeks++;
            }
            data.weeksNotMeditated = zeroWeeks;

        } catch (Exception e) { e.printStackTrace(); }

        // --- D. Yearly Activity Extremes (NEW LOGIC) ---
        if (data.isYearly && !monthlyHours.isEmpty()) {
            int maxMonth = -1;
            float maxVal = -1f;
            int minMonth = -1;
            float minVal = Float.MAX_VALUE;

            for (Map.Entry<Integer, Float> entry : monthlyHours.entrySet()) {
                if (entry.getValue() > maxVal) {
                    maxVal = entry.getValue();
                    maxMonth = entry.getKey();
                }
                if (entry.getValue() < minVal) {
                    minVal = entry.getValue();
                    minMonth = entry.getKey();
                }
            }

            String[] shortMonths = new DateFormatSymbols(Locale.getDefault()).getShortMonths();

            if (maxMonth != -1) {
                data.mostActiveMonthLabel = shortMonths[maxMonth];
                data.mostActiveMonthValue = maxVal;
            }
            if (minMonth != -1) {
                data.leastActiveMonthLabel = shortMonths[minMonth];
                data.leastActiveMonthValue = minVal;
            }
        }
    }

    private static void processCharts(MeditationReportData data, ArrayList<MeditationLogDatabaseHelper.SessionData> sessions) {
        Calendar cal = Calendar.getInstance();
        for (MeditationLogDatabaseHelper.SessionData s : sessions) {
            cal.setTimeInMillis(s.timestamp);
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            if (hour >= 4 && hour < 12) data.preferredTimes.put("Morning", data.preferredTimes.get("Morning") + 1);
            else if (hour >= 12 && hour < 17) data.preferredTimes.put("Noon", data.preferredTimes.get("Noon") + 1);
            else data.preferredTimes.put("Evening", data.preferredTimes.get("Evening") + 1);

            int mins = s.durationSeconds / 60;
            if (mins < 5) data.sessionFrequency.put("0-5 min", data.sessionFrequency.get("0-5 min") + 1);
            else if (mins < 10) data.sessionFrequency.put("5-10 min", data.sessionFrequency.get("5-10 min") + 1);
            else if (mins <= 25) data.sessionFrequency.put("10-25 min", data.sessionFrequency.get("10-25 min") + 1);
            else data.sessionFrequency.put(">25mins", data.sessionFrequency.get(">25mins") + 1);
        }
    }
}