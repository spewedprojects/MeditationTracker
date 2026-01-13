package com.gratus.meditationtrakcer.utils;

import android.content.Context;
import com.gratus.meditationtrakcer.models.MeditationReportData;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ReportJsonHelper {
    private static final String FILENAME = "saved_reports.json";

    public static void saveReport(Context context, MeditationReportData newReport) {
        List<MeditationReportData> existing = loadReports(context);

        // Remove if exists (to overwrite)
        existing.removeIf(r -> r.reportId.equals(newReport.reportId));
        existing.add(0, newReport); // Add to top

        saveList(context, existing);
    }

    public static void deleteReport(Context context, String reportId) {
        List<MeditationReportData> existing = loadReports(context);
        existing.removeIf(r -> r.reportId.equals(reportId));
        saveList(context, existing);
    }

    private static void saveList(Context context, List<MeditationReportData> list) {
        JSONArray array = new JSONArray();
        try {
            for (MeditationReportData d : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", d.reportId);
                obj.put("title", d.title);

                // ✅ FIXED: Using isYearly
                obj.put("isYearly", d.isYearly);

                obj.put("totalHours", d.totalHours);
                obj.put("consistency", d.consistencyScore);
                obj.put("bestStreak", d.bestStreak);
                obj.put("avgSession", d.avgSessionLength);
                obj.put("daysWithout", d.daysNotMeditated);
                obj.put("weeksWithout", d.weeksNotMeditated);
                obj.put("streakStability", (double)d.streakStability);
                obj.put("totalSessions", d.totalSessions);

                // Maps
                JSONObject timeMap = new JSONObject(d.preferredTimes);
                obj.put("preferredTimes", timeMap);

                JSONObject freqMap = new JSONObject(d.sessionFrequency);
                obj.put("sessionFrequency", freqMap);

                array.put(obj);
            }

            File file = new File(context.getFilesDir(), FILENAME);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(array.toString().getBytes());
            fos.close();

        } catch (Exception e) { e.printStackTrace(); }
    }

    public static List<MeditationReportData> loadReports(Context context) {
        List<MeditationReportData> list = new ArrayList<>();
        try {
            File file = new File(context.getFilesDir(), FILENAME);
            if (!file.exists()) return list;

            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            fis.close();

            JSONArray array = new JSONArray(sb.toString());
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                MeditationReportData d = new MeditationReportData();
                d.reportId = obj.optString("id");
                d.title = obj.optString("title");

                // ✅ FIXED: Using isYearly
                d.isYearly = obj.optBoolean("isYearly");

                d.totalHours = (float) obj.optDouble("totalHours");
                d.consistencyScore = obj.optInt("consistency");
                d.bestStreak = obj.optInt("bestStreak");
                d.avgSessionLength = obj.optInt("avgSession");
                d.daysNotMeditated = obj.optInt("daysWithout");
                d.weeksNotMeditated = obj.optInt("weeksWithout");
                d.streakStability = (float) obj.optDouble("streakStability");
                d.totalSessions = obj.optInt("totalSessions");

                // Maps
                JSONObject timeObj = obj.optJSONObject("preferredTimes");
                if(timeObj != null) {
                    Iterator<String> keys = timeObj.keys();
                    while(keys.hasNext()) {
                        String key = keys.next();
                        d.preferredTimes.put(key, timeObj.getInt(key));
                    }
                }

                JSONObject freqObj = obj.optJSONObject("sessionFrequency");
                if(freqObj != null) {
                    Iterator<String> keys = freqObj.keys();
                    while(keys.hasNext()) {
                        String key = keys.next();
                        d.sessionFrequency.put(key, freqObj.getInt(key));
                    }
                }

                list.add(d);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }
}