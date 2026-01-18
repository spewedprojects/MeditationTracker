package com.gratus.meditationtrakcer.utils;

import android.content.Context;
import com.gratus.meditationtrakcer.models.MeditationReportData;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ReportJsonHelper {

    // Use a dedicated directory for reports
    private static final String REPORT_DIR = "reports";

    // Helper to get (and create if needed) the reports directory
    private static File getReportDir(Context context) {
        File dir = new File(context.getFilesDir(), REPORT_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static void saveReport(Context context, MeditationReportData d) {
        try {
            JSONObject obj = new JSONObject();
            // --- Metadata ---
            obj.put("id", d.reportId);
            obj.put("title", d.title);
            obj.put("generatedTimestamp", d.generatedTimestamp); // Crucial for sorting!
            obj.put("isYearly", d.isYearly);

            // --- Metrics ---
            obj.put("totalHours", (double) d.totalHours);
            obj.put("consistency", d.consistencyScore);
            obj.put("bestStreak", d.bestStreak);
            obj.put("avgSession", d.avgSessionLength);
            obj.put("daysWithout", d.daysNotMeditated);
            obj.put("weeksWithout", d.weeksNotMeditated);
            obj.put("streakStability", (double) d.streakStability);
            obj.put("totalSessions", d.totalSessions);
            obj.put("avgSessionGap", (double) d.avgSessionGap);

            // --- Extremes (Handle N/A) ---
            obj.put("mostActiveMonthLabel", d.mostActiveMonthLabel != null ? d.mostActiveMonthLabel : JSONObject.NULL);
            obj.put("mostActiveMonthValue", (double) d.mostActiveMonthValue);
            obj.put("leastActiveMonthLabel", d.leastActiveMonthLabel != null ? d.leastActiveMonthLabel : JSONObject.NULL);
            obj.put("leastActiveMonthValue", (double) d.leastActiveMonthValue);

            // --- Maps ---
            JSONObject timeMap = new JSONObject(d.preferredTimes);
            obj.put("preferredTimes", timeMap);

            JSONObject freqMap = new JSONObject(d.sessionFrequency);
            obj.put("sessionFrequency", freqMap);

            // Write to a separate file: "report_ID.json"
            File file = new File(getReportDir(context), "report_" + d.reportId + ".json");
            FileOutputStream fos = new FileOutputStream(file);

            // ✅ Indentation: Use 4 spaces for pretty print
            fos.write(obj.toString(4).getBytes());
            fos.close();

        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void deleteReport(Context context, String reportId) {
        try {
            // Construct the specific filename to delete
            File file = new File(getReportDir(context), "report_" + reportId + ".json");
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static List<MeditationReportData> loadReports(Context context) {
        List<MeditationReportData> list = new ArrayList<>();
        File dir = getReportDir(context);
        File[] files = dir.listFiles();

        if (files == null) return list;

        for (File file : files) {
            // Only process our report files
            if (!file.getName().startsWith("report_") || !file.getName().endsWith(".json")) continue;

            try {
                FileInputStream fis = new FileInputStream(file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                fis.close();

                JSONObject obj = new JSONObject(sb.toString());
                MeditationReportData d = new MeditationReportData();

                // --- Metadata ---
                d.reportId = obj.optString("id");
                d.title = obj.optString("title");
                d.generatedTimestamp = obj.optLong("generatedTimestamp", 0);
                d.isYearly = obj.optBoolean("isYearly");

                // --- Metrics ---
                d.totalHours = (float) obj.optDouble("totalHours");
                d.consistencyScore = obj.optInt("consistency");
                d.bestStreak = obj.optInt("bestStreak");
                d.avgSessionLength = obj.optInt("avgSession");
                d.daysNotMeditated = obj.optInt("daysWithout");
                d.weeksNotMeditated = obj.optInt("weeksWithout");
                d.streakStability = (float) obj.optDouble("streakStability");
                d.totalSessions = obj.optInt("totalSessions");
                d.avgSessionGap = (float) obj.optDouble("avgSessionGap", 0.0);

                // --- Extremes ---
                if (!obj.isNull("mostActiveMonthLabel")) {
                    d.mostActiveMonthLabel = obj.optString("mostActiveMonthLabel");
                }
                d.mostActiveMonthValue = (float) obj.optDouble("mostActiveMonthValue");

                if (!obj.isNull("leastActiveMonthLabel")) {
                    d.leastActiveMonthLabel = obj.optString("leastActiveMonthLabel");
                }
                d.leastActiveMonthValue = (float) obj.optDouble("leastActiveMonthValue");

                // --- Maps ---
                JSONObject timeObj = obj.optJSONObject("preferredTimes");
                if (timeObj != null) {
                    Iterator<String> keys = timeObj.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        d.preferredTimes.put(key, timeObj.getInt(key));
                    }
                }

                JSONObject freqObj = obj.optJSONObject("sessionFrequency");
                if (freqObj != null) {
                    Iterator<String> keys = freqObj.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        d.sessionFrequency.put(key, freqObj.getInt(key));
                    }
                }

                list.add(d);

            } catch (Exception e) { e.printStackTrace(); }
        }

        // Sort by timestamp (Newest first)
        Collections.sort(list, (o1, o2) -> Long.compare(o2.generatedTimestamp, o1.generatedTimestamp));

        return list;
    }
}