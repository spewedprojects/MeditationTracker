package com.gratus.meditationtrakcer.models;

import java.util.HashMap;
import java.util.Map;

public class MeditationReportData {
    // ID parameters
    public String reportId;
    public String title;
    public long generatedTimestamp;

    // ✅ FIXED: Name matches usage in Generator/JsonHelper
    public boolean isYearly;

    // --- Row 1 & Mini Card ---
    public float totalHours;
    public int consistencyScore;
    public int bestStreak;
    public int avgSessionLength;
    public int daysNotMeditated;

    // --- Detail Stats ---
    public int weeksNotMeditated;
    public float streakStability;
    public int totalSessions;
    public float avgSessionGap;

    // --- Activity Extremes ---
    public String mostActiveMonthLabel;
    public float mostActiveMonthValue;
    public String leastActiveMonthLabel;
    public float leastActiveMonthValue;

    // --- Charts Data ---
    public Map<String, Integer> preferredTimes = new HashMap<>();
    public Map<String, Integer> sessionFrequency = new HashMap<>();

    public MeditationReportData() {
        preferredTimes.put("Morning", 0);
        preferredTimes.put("Noon", 0);
        preferredTimes.put("Evening", 0);

        sessionFrequency.put("0-5 min", 0);
        sessionFrequency.put("5-10 min", 0);
        sessionFrequency.put("10-25 min", 0);
        sessionFrequency.put(">25mins", 0);
    }
}