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
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MenuActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setWindowAnimations(0); // Disable window animations
        setContentView(R.layout.activity_menu);

        // Navigation buttons
        findViewById(R.id.menu_home).setOnClickListener(v -> navigateTo(MainActivity.class));
        findViewById(R.id.menu_weekly).setOnClickListener(v -> navigateTo(WeeklyActivity.class));
        findViewById(R.id.menu_monthly).setOnClickListener(v -> navigateTo(MonthlyActivity.class));
        findViewById(R.id.menu_yearly).setOnClickListener(v -> navigateTo(YearlyActivity.class));
        findViewById(R.id.menu_goals).setOnClickListener(v -> navigateTo(GoalsActivity.class));
        findViewById(R.id.menu_about).setOnClickListener(v -> navigateTo(AboutActivity.class));

        // Export and import buttons
        findViewById(R.id.menu_exportButton).setOnClickListener(v -> exportData());
        findViewById(R.id.menu_importButton).setOnClickListener(v -> showFileChooser());
    }

    private void navigateTo(Class<?> activityClass) {
        Intent intent = new Intent(MenuActivity.this, activityClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void exportData() {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());

            // Export JSON
            exportFile("meditation_data_" + timestamp + ".json", "application/json", true);

            // Export .db files
            exportFile("meditation_logs_" + timestamp + ".db", "application/octet-stream", false);
            exportFile("goals_" + timestamp + ".db", "application/octet-stream", false);

            Toast.makeText(this, "Export completed successfully!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Export encountered an error.", Toast.LENGTH_SHORT).show();
        }
    }

    private void exportFile(String fileName, String mimeType, boolean isJson) throws Exception {
        OutputStream outputStream;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use MediaStore for Android 10+
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/MeditationExports");

            Uri fileUri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
            if (fileUri == null) {
                throw new FileNotFoundException("Failed to create file in Documents directory");
            }
            outputStream = getContentResolver().openOutputStream(fileUri);
        } else {
            // Use legacy storage for Android 9 and below
            File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            File meditationDir = new File(exportDir, "MeditationExports");
            if (!meditationDir.exists() && !meditationDir.mkdirs()) {
                throw new Exception("Failed to create MeditationExports directory.");
            }
            File exportFile = new File(meditationDir, fileName);
            outputStream = new FileOutputStream(exportFile);
        }

        if (isJson) {
            // Write JSON data
            exportDataAsJson(outputStream);
        } else {
            // Export database file
            String dbName = fileName.contains("meditation_logs") ? "meditation_logs.db" : "goals.db";
            exportDatabase(dbName, outputStream);
        }

        outputStream.close();
    }

    private void exportDatabase(String dbName, OutputStream outputStream) throws Exception {
        String databasePath = getDatabasePath(dbName).getAbsolutePath();
        try (InputStream inputStream = new FileInputStream(databasePath)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }
    }

    private void exportDataAsJson(OutputStream outputStream) throws Exception {
        GoalsDatabaseHelper goalsHelper = new GoalsDatabaseHelper(this);
        MeditationLogDatabaseHelper logHelper = new MeditationLogDatabaseHelper(this);

        JSONArray goalsData = goalsHelper.getGoalsAsJSONArray();
        JSONArray logsData = logHelper.getLogsAsJSONArray();

        JSONObject exportData = new JSONObject();
        exportData.put("goals", goalsData);
        exportData.put("logs", logsData);

        outputStream.write(exportData.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select File"), PICK_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            importData(fileUri);
        }
    }

    private void importData(Uri fileUri) {
        try {
            String fileName = getFileName(fileUri);

            if (fileName.endsWith(".json")) {
                importDataFromJson(fileUri);
            } else if (fileName.endsWith(".db")) {
                if (fileName.contains("meditation_logs")) {
                    importDatabase(fileUri, "meditation_logs.db");
                } else if (fileName.contains("goals")) {
                    importDatabase(fileUri, "goals.db");
                } else {
                    throw new IllegalArgumentException("Unsupported .db file");
                }
            } else {
                throw new IllegalArgumentException("Unsupported file type");
            }

            Toast.makeText(this, "Import completed successfully!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Import encountered an error.", Toast.LENGTH_SHORT).show();
        }
    }

    private void importDatabase(Uri fileUri, String targetDbName) throws Exception {
        File targetFile = getDatabasePath(targetDbName);
        try (InputStream inputStream = getContentResolver().openInputStream(fileUri);
             OutputStream outputStream = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }
    }

    private void importDataFromJson(Uri fileUri) throws Exception {
        InputStream inputStream = getContentResolver().openInputStream(fileUri);
        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        inputStream.close();

        String jsonString = new String(buffer, StandardCharsets.UTF_8);
        JSONObject jsonData = new JSONObject(jsonString);

        JSONArray goalsArray = jsonData.optJSONArray("goals");
        JSONArray logsArray = jsonData.optJSONArray("logs");

        GoalsDatabaseHelper goalsHelper = new GoalsDatabaseHelper(this);
        MeditationLogDatabaseHelper logHelper = new MeditationLogDatabaseHelper(this);

        if (goalsArray != null) {
            goalsHelper.importDataFromJSONArray(goalsArray);
        }
        if (logsArray != null) {
            logHelper.importDataFromJSONArray(logsArray);
        }
    }

    private String getFileName(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            cursor.close();
            return name;
        }
        return null;
    }


}
