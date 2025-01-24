package com.gratus.meditationtrakcer;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Disable window animations
        getWindow().setWindowAnimations(0);

        setContentView(R.layout.activity_menu);

        // Home button - Close Menu and return to MainActivity
        TextView homeButton = findViewById(R.id.menu_home);
        // Home button - Always return to MainActivity without history
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Close the MenuActivity
        });

        // Close button to dismiss the menu
        Button closeButton = findViewById(R.id.close_menu);
        closeButton.setOnClickListener(v -> finish());

        // Weekly Summary - Open WeeklyActivity
        TextView weeklyButton = findViewById(R.id.menu_weekly);
        weeklyButton.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, WeeklyActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // Monthly Overview - Open MonthlyActivity
        TextView monthlyButton = findViewById(R.id.menu_monthly);
        monthlyButton.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, MonthlyActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // Yearly Analysis - Open YearlyActivity
        TextView yearlyButton = findViewById(R.id.menu_yearly);
        yearlyButton.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, YearlyActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // Custom Goals - Open GoalsActivity
        TextView goalsButton = findViewById(R.id.menu_goals);
        goalsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, GoalsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // Custom Goals - Open AboutActivity
        TextView aboutButton = findViewById(R.id.menu_about);
        aboutButton.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, AboutActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // Initialize buttons
        Button btnExport = findViewById(R.id.menu_exportButton);
        Button btnImport = findViewById(R.id.menu_importButton);

        // Set up Export button listener
        btnExport.setOnClickListener(v -> exportData());

        // Set up Import button listener
        btnImport.setOnClickListener(v -> importData());
    }

//    private void exportData() {
//        try {
//            // Directory setup for exported file
//            File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
//            File meditationDir = new File(exportDir, "MeditationExports");
//            if (!meditationDir.exists()) {
//                meditationDir.mkdirs();
//            }
//
//            File exportFile = new File(meditationDir, "meditation_data.json");
//
//            // Fetch data from Goals and Logs
//            GoalsDatabaseHelper goalsHelper = new GoalsDatabaseHelper(this);
//            MeditationLogDatabaseHelper logHelper = new MeditationLogDatabaseHelper(this);
//
//            JSONArray goalsData = goalsHelper.getGoalsAsJSONArray(); // Fetch goals data
//            JSONArray logsData = logHelper.getLogsAsJSONArray();     // Fetch meditation logs data
//
//            // Combine both datasets into a single JSON object
//            JSONObject exportData = new JSONObject();
//            exportData.put("goals", goalsData);
//            exportData.put("logs", logsData);
//
//            // Write the combined JSON data to file
//            FileWriter writer = new FileWriter(exportFile);
//            writer.write(exportData.toString());
//            writer.close();
//
//            Toast.makeText(this, "Data exported to: " + exportFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
//        } catch (Exception e) {
//            e.printStackTrace();
//            Toast.makeText(this, "Export encountered an error.", Toast.LENGTH_SHORT).show();
//        }
//    }

    private void exportData() {
        try {
            OutputStream outputStream;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Scoped Storage
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, "meditation_data.json");
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/json");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/MeditationExports");

                Uri fileUri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
                if (fileUri == null) {
                    throw new FileNotFoundException("Failed to create file in Documents directory");
                }
                outputStream = getContentResolver().openOutputStream(fileUri);
            } else {
                // Legacy approach for Android 9 and below
                File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                File meditationDir = new File(exportDir, "MeditationExports");
                if (!meditationDir.exists()) {
                    meditationDir.mkdirs();
                }
                File exportFile = new File(meditationDir, "meditation_data.json");
                outputStream = new FileOutputStream(exportFile);
            }

            // Fetch data from Goals and Logs
            GoalsDatabaseHelper goalsHelper = new GoalsDatabaseHelper(this);
            MeditationLogDatabaseHelper logHelper = new MeditationLogDatabaseHelper(this);

            JSONArray goalsData = goalsHelper.getGoalsAsJSONArray();
            JSONArray logsData = logHelper.getLogsAsJSONArray();

            JSONObject exportData = new JSONObject();
            exportData.put("goals", goalsData);
            exportData.put("logs", logsData);

            // Write JSON data to the file
            assert outputStream != null;
            outputStream.write(exportData.toString().getBytes());
            outputStream.close();

            Toast.makeText(this, "Data exported successfully", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Export encountered an error.", Toast.LENGTH_SHORT).show();
        }
    }


    private void importData() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/json");
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(fileUri);
                byte[] buffer = new byte[inputStream.available()];
                inputStream.read(buffer);
                inputStream.close();

                // Convert file content to JSON
                String jsonString = new String(buffer, "UTF-8");
                JSONObject jsonData = new JSONObject(jsonString);

                // Separate goals and logs
                JSONArray goalsArray = jsonData.optJSONArray("goals");
                JSONArray logsArray = jsonData.optJSONArray("logs");

                GoalsDatabaseHelper goalsHelper = new GoalsDatabaseHelper(this);
                MeditationLogDatabaseHelper logHelper = new MeditationLogDatabaseHelper(this);

                boolean goalsImported = goalsHelper.importDataFromJSONArray(goalsArray);
                boolean logsImported = logHelper.importDataFromJSONArray(logsArray);

                if (goalsImported && logsImported) {
                    Toast.makeText(this, "Data imported successfully.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Import failed.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Import encountered an error.", Toast.LENGTH_SHORT).show();
            }
        }
    }


}
