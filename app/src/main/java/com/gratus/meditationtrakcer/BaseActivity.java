package com.gratus.meditationtrakcer;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import android.os.Build;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.customview.widget.ViewDragHelper;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.annotation.LayoutRes;

import com.google.android.material.button.MaterialButton;
import com.gratus.meditationtrakcer.databasehelpers.GoalsDatabaseHelper;
import com.gratus.meditationtrakcer.databasehelpers.MeditationLogDatabaseHelper;
import com.gratus.meditationtrakcer.databasehelpers.StreakDatabaseHelper;

import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BaseActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private static final int PICK_FILE_REQUEST_CODE = 1;

    private static final String PREFS_NAME = "MeditationTrackerPrefs"; // Consolidated Prefs File
    public static final String SHARED_PREFS_NAME = PREFS_NAME; // Public accessor
    private static final String THEME_KEY = "SelectedTheme";

    // Legacy Prefs Files (for migration)
    private static final String LEGACY_THEME_PREFS = "AppThemeSettings";
    private static final String LEGACY_GOALS_PREFS = "goals_Prefs";
    private static final String LEGACY_SUMMARY_PREFS = "summary_prefs";
    private static final String KEY_MIGRATION_COMPLETE = "prefs_migration_complete_v1";
    private boolean doubleBackToExitPressedOnce = false;
    private final Handler backPressHandler = new Handler(Looper.getMainLooper());
    private boolean useSystemFont = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Read the preference before any layouts are inflated
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);
        useSystemFont = prefs.getBoolean("use_system_font", false);

        migrateLegacyPreferences(); // Run migration before applying theme
        applyTheme();
        super.onCreate(savedInstanceState);

        setupOnDoubleBackPressed();
    }

    private void setupOnDoubleBackPressed() {
        // Register the modern back press callback
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 1. Handle Drawer first
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return;
                }

                // 2. Handle "Double Tap to Exit" logic
                if (isTaskRoot() && isMainActivity()) {
                    if (doubleBackToExitPressedOnce) {
                        finish(); // Or use finishAffinity() to close the whole app
                        return;
                    }

                    doubleBackToExitPressedOnce = true;
                    Toast.makeText(BaseActivity.this, "Press BACK again to exit", Toast.LENGTH_SHORT).show();

                    // Reset the flag after 2 seconds
                    backPressHandler.postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
                } else {
                    // 3. Default behavior (pop backstack or finish child activity)
                    setEnabled(false); // Temporarily disable this callback
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true); // Re-enable it
                }
            }
        });
    }

    // Help avoid 'instanceof' by using a helper method
    protected boolean isMainActivity() {
        return false;
    }

    /**
     * Apply the saved theme or default to 'auto'.
     */
    private void applyTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String theme = prefs.getString(THEME_KEY, "auto"); // Default to 'auto'

        switch (theme) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "auto":
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    /**
     * Set up the theme toggle buttons.
     */
    private void setupThemeButtons() {
        ImageButton lightButton = findViewById(R.id.btn_light);
        ImageButton darkButton = findViewById(R.id.btn_dark);
        ImageButton autoButton = findViewById(R.id.btn_auto);

        if (lightButton != null && darkButton != null && autoButton != null) {
            // Get the current theme from SharedPreferences
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String currentTheme = prefs.getString(THEME_KEY, "auto");

            // Update button visibility based on the current theme
            updateButtonVisibility(currentTheme, lightButton, darkButton, autoButton);

            // Set click listeners for the buttons
            lightButton.setOnClickListener(v -> {
                setThemeAndSave("light");
                updateButtonVisibility("light", lightButton, darkButton, autoButton);
            });

            darkButton.setOnClickListener(v -> {
                setThemeAndSave("dark");
                updateButtonVisibility("dark", lightButton, darkButton, autoButton);
            });

            autoButton.setOnClickListener(v -> {
                setThemeAndSave("auto");
                updateButtonVisibility("auto", lightButton, darkButton, autoButton);
            });
        }
    }

    /**
     * Update the visibility of the theme buttons based on the current theme.
     */
    private void updateButtonVisibility(String currentTheme, ImageButton lightButton, ImageButton darkButton,
            ImageButton autoButton) {
        switch (currentTheme) {
            case "light":
                lightButton.setVisibility(View.GONE);
                darkButton.setVisibility(View.VISIBLE);
                autoButton.setVisibility(View.GONE);
                System.out.println("LIGHT mode - GONE (Light button and Auto button), VISIBLE (Dark button)");
                break;
            case "dark":
                lightButton.setVisibility(View.GONE);
                darkButton.setVisibility(View.GONE);
                autoButton.setVisibility(View.VISIBLE);
                System.out.println("DARK mode - GONE (Light button and Dark button), VISIBLE (Auto button)");
                break;
            case "auto":
                lightButton.setVisibility(View.VISIBLE);
                darkButton.setVisibility(View.GONE);
                autoButton.setVisibility(View.GONE);
                System.out.println("AUTO mode - GONE (Dark button and Auto button), VISIBLE (Light button)");
                break;
        }
    }

    /**
     * Set the theme and save the selection to SharedPreferences.
     */
    private void setThemeAndSave(String theme) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(THEME_KEY, theme).apply();

        // Log the saved theme
        System.out.println("Saved theme: " + theme);

        // Apply the new theme
        applyTheme();

        // Restart the activity to reflect the theme change
        recreate();
    }

    public void setDrawerLeftEdgeSize(DrawerLayout drawerLayout, float displayWidthPercentage) {
        if (drawerLayout == null)
            return;
        try {
            // Access the private mLeftDragger field in DrawerLayout
            Field leftDraggerField = drawerLayout.getClass().getDeclaredField("mLeftDragger");
            leftDraggerField.setAccessible(true);
            ViewDragHelper leftDragger = (ViewDragHelper) leftDraggerField.get(drawerLayout);

            // Access the private mEdgeSize field within ViewDragHelper
            assert leftDragger != null;
            Field edgeSizeField = leftDragger.getClass().getDeclaredField("mEdgeSize");
            edgeSizeField.setAccessible(true);
            int edgeSize = edgeSizeField.getInt(leftDragger);

            // Increase the edge size to a percentage of the screen width.
            // For example, using 0.20 (20%) instead of the default (usually around 10%).
            DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            int newEdgeSize = Math.max(edgeSize, (int) (dm.widthPixels * displayWidthPercentage));
            edgeSizeField.setInt(leftDragger, newEdgeSize);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Override setContentView so that the child layout is inflated into the
     * content_frame
     * of our activity_base.xml.
     */
    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        // Inflate the base layout which includes the DrawerLayout
        DrawerLayout fullView = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_base, null);
        fullView.setScrimColor(Color.parseColor("#33000000")); // 20% black

        setDrawerLeftEdgeSize(drawerLayout, 0.30f); // sets edge swipe area to 20% of screen width

        // Find the container into which we will inflate the child layout
        // (child layouts like activity_main.xml, etc.)
        FrameLayout activityContainer = fullView.findViewById(R.id.content_frame);
        // Inflate the child layout into the container
        getLayoutInflater().inflate(layoutResID, activityContainer, true);
        // Set the full view as the content view
        EdgeToEdge.enable(this);
        super.setContentView(fullView);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.menu_drawer), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get the DrawerLayout so we can open/close it later
        drawerLayout = fullView.findViewById(R.id.drawer_layout);

        // Set up drawer items listeners (including close button, export/import, etc.)
        setupDrawer();

        // Initialize theme buttons
        setupThemeButtons();
        // ---> ADD THIS RIGHT AT THE END <---
        // Now that the layout is fully built and font styles are settled, strip them.
        applySystemFontToView(fullView);
    }

    // ==========================================
    // MANUAL HELPER FOR DIALOGS AND RECYCLERVIEWS
    // ==========================================

    /**
     * Recursively applies the system font. Call this on Dialog windows or RecyclerView items.
     */
    public void applySystemFontToView(View view) {
        if (!useSystemFont || view == null) return;

        // --- NEW: EXCEPTION FOR CUSTOM FONT PREVIEW BUTTON ---
        if (view.getId() == R.id.set_custom_font_btn) {
            return; // Skip overriding this specific view so it keeps the app font
        }

        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applySystemFontToView(vg.getChildAt(i));
            }
        } else if (view instanceof android.widget.TextView) {
            android.widget.TextView tv = (android.widget.TextView) view;
            android.graphics.Typeface current = tv.getTypeface();
            int style = android.graphics.Typeface.NORMAL;

            if (current != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    boolean isBold = current.getWeight() >= 600 || current.isBold();
                    if (isBold && current.isItalic()) style = android.graphics.Typeface.BOLD_ITALIC;
                    else if (isBold) style = android.graphics.Typeface.BOLD;
                    else if (current.isItalic()) style = android.graphics.Typeface.ITALIC;
                } else {
                    style = current.getStyle();
                }
            }
            tv.setTypeface(android.graphics.Typeface.DEFAULT, style);
        }
    }

    // Disable all activity transition animations
    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    /**
     * Set up the toolbar and menu button.
     * Call this from your child activities (e.g., in onCreate after setContentView)
     */
    protected void setupToolbar(int toolbarId, int menuButtonId) {
        Toolbar toolbar = findViewById(toolbarId);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        ImageButton menuButton = findViewById(menuButtonId);
        menuButton.setOnClickListener(v -> {
            // Toggle the drawer when the menu button is clicked
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    /**
     * Set up the drawer’s internal button listeners.
     */
    private void setupDrawer() {
        // Example: set up a listener on the close button inside the drawer.
        ImageButton closeMenu = findViewById(R.id.close_menu);
        if (closeMenu != null) {
            closeMenu.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        }

        // Set up other drawer buttons to navigate to your activities.
        // For example:
        findViewById(R.id.menu_home).setOnClickListener(v -> navigateTo(MainActivity.class));
        findViewById(R.id.menu_summary).setOnClickListener(v -> navigateTo(SummaryActivity.class));
        findViewById(R.id.menu_reports).setOnClickListener(v -> navigateTo(ReportsActivity.class));
        findViewById(R.id.menu_goals).setOnClickListener(v -> navigateTo(GoalsActivity.class));
        findViewById(R.id.menu_about).setOnClickListener(v -> navigateTo(AboutActivity.class));
        findViewById(R.id.menu_releasenotes).setOnClickListener(v -> navigateTo(ReleaseNotesActivity.class));
        findViewById(R.id.menu_app_settings).setOnClickListener(v -> navigateTo(SettingsInfoActivity.class));

        // Export/Import buttons can be set up here too.
        findViewById(R.id.menu_exportButton).setOnClickListener(v -> exportData());
        findViewById(R.id.menu_importButton).setOnClickListener(v -> showFileChooser());

        // === ADD THIS LINE ===
        updateActiveMenuItem();
    }

    /**
     * Highlights the menu item corresponding to the current activity. (27/01/26)
     */
    private void updateActiveMenuItem() {
        // 1. Determine which ID corresponds to the current Activity
        int activeId = -1;

        if (this instanceof MainActivity) {
            activeId = R.id.menu_home;
        } else if (this instanceof SummaryActivity) {
            activeId = R.id.menu_summary;
        } else if (this instanceof ReportsActivity) { // Assuming you have this class
            activeId = R.id.menu_reports;
        } else if (this instanceof GoalsActivity) {
            activeId = R.id.menu_goals;
        } else if (this instanceof AboutActivity) { // Assuming you have this class
            activeId = R.id.menu_about;
        } else if (this instanceof ReleaseNotesActivity) { // Assuming you have this class
            activeId = R.id.menu_releasenotes;
        }

        // 2. List of all menu button IDs to iterate through
        int[] allMenuIds = {
                R.id.menu_home,
                R.id.menu_summary,
                R.id.menu_reports,
                R.id.menu_goals,
                R.id.menu_about,
                R.id.menu_releasenotes
        };

        // 3. Loop through buttons and set Icon Tint
        for (int id : allMenuIds) {
            MaterialButton btn = findViewById(id);
            if (btn == null)
                continue;

            if (id == activeId) {
                // ACTIVE: Set tint to your green color (or fetch from resources)
                // Use R.color.success_green if defined, or parse the color manually if you
                // don't have a resource handle handy
                btn.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.active_dot_color)));
                // If you don't have R.color.success_green defined in java, use:
                // Color.parseColor("#YOUR_HEX_CODE")
            } else {
                // INACTIVE: Set tint to Transparent (hides the dot but keeps layout stable)
                btn.setIconTint(ColorStateList.valueOf(Color.TRANSPARENT));
            }
        }
    }

    /**
     * Navigation helper that starts the given activity.
     */
    private void navigateTo(Class<?> activityClass) {
        // Already there? Nothing to do
        if (getClass() == activityClass) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        // Bring an existing instance forward (if any) or create a new one
        Intent intent = new Intent(this, activityClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);

        // If we *weren’t* on MainActivity, kill the screen we just left.
        // This collapses any Main → Child-A → Child-B chain to just
        // Main → Child-B
        if (!(this instanceof MainActivity)) {
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
    }

    private void exportData() {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());

            // Export JSON
            exportFile("meditation_data_" + timestamp + ".json", "application/json", true);

            // Export .db files
            exportFile("meditation_logs_" + timestamp + ".db", "application/octet-stream", false);
            exportFile("goals_" + timestamp + ".db", "application/octet-stream", false);
            exportFile("streaks_" + timestamp + ".db", "application/octet-stream", false);

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
            String dbName;
            if (fileName.contains("meditation_logs")) {
                dbName = "meditation_logs.db";
            } else if (fileName.contains("goals")) { // Assuming you meant to check for "goals"
                dbName = "goals.db";
            } else if (fileName.contains("streaks")) { // Assuming you meant to check for "streaks"
                dbName = "streaks.db";
            } else {
                // Handle the case where none of the conditions are met (optional but good
                // practice)
                dbName = "default.db"; // Or throw an exception, or assign null
            }
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
        StreakDatabaseHelper streakHelper = new StreakDatabaseHelper(this);

        JSONArray goalsData = goalsHelper.getGoalsAsJSONArray();
        JSONArray logsData = logHelper.getLogsAsJSONArray();
        JSONArray streakData = streakHelper.getStreaksAsJSONArray();

        JSONObject exportData = new JSONObject();
        exportData.put("goals", goalsData);
        exportData.put("logs", logsData);
        exportData.put("streaks", streakData);

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
                } else if (fileName.contains("streaks")) {
                    importDatabase(fileUri, "streaks.db");
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
        JSONArray streaksArray = jsonData.optJSONArray("streaks");

        GoalsDatabaseHelper goalsHelper = new GoalsDatabaseHelper(this);
        MeditationLogDatabaseHelper logHelper = new MeditationLogDatabaseHelper(this);
        StreakDatabaseHelper streaksHelper = new StreakDatabaseHelper(this);

        if (goalsArray != null) {
            goalsHelper.importDataFromJSONArray(goalsArray);
        }
        if (logsArray != null) {
            logHelper.importDataFromJSONArray(logsArray);
        }
        if (streaksArray != null) {
            streaksHelper.importDataFromJSONArray(streaksArray);
        }
    }

    private String getFileName(Uri uri) {
        // 1. Use try-with-resources to ensure the cursor closes automatically
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                // 2. Use getColumnIndexOrThrow to catch issues early
                // or check if index > -1
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    return cursor.getString(nameIndex);
                }
            }
        } catch (Exception e) {
            Log.e("FileUtil", "Error retrieving file name", e);
        }

        // 3. Fallback: If query fails, try to get the name from the URI path
        return uri.getLastPathSegment();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Prevent memory leaks by removing callbacks when activity is destroyed
        backPressHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Migrates preferences from old files to the new consolidated file.
     * Runs only once.
     */
    private void migrateLegacyPreferences() {
        SharedPreferences newPrefs = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);

        // Check if migration is already done
        if (newPrefs.getBoolean(KEY_MIGRATION_COMPLETE, false)) {
            return;
        }

        Log.d("BaseActivity", "Starting Preferences Migration...");
        SharedPreferences.Editor editor = newPrefs.edit();

        // 1. Migrate Theme Settings
        migrateFile(LEGACY_THEME_PREFS, editor);

        // 2. Migrate Goals Settings
        migrateFile(LEGACY_GOALS_PREFS, editor);

        // 3. Migrate Summary Settings
        migrateFile(LEGACY_SUMMARY_PREFS, editor);

        // Mark migration as complete
        editor.putBoolean(KEY_MIGRATION_COMPLETE, true);
        editor.apply();

        Log.d("BaseActivity", "Preferences Migration Completed Successfully.");
    }

    private void migrateFile(String legacyFileName, SharedPreferences.Editor newEditor) {
        SharedPreferences legacyPrefs = getSharedPreferences(legacyFileName, MODE_PRIVATE);
        java.util.Map<String, ?> allEntries = legacyPrefs.getAll();

        if (allEntries.isEmpty())
            return;

        for (java.util.Map.Entry<String, ?> entry : allEntries.entrySet()) {
            Object value = entry.getValue();
            String key = entry.getKey();

            if (value instanceof String)
                newEditor.putString(key, (String) value);
            else if (value instanceof Integer)
                newEditor.putInt(key, (Integer) value);
            else if (value instanceof Boolean)
                newEditor.putBoolean(key, (Boolean) value);
            else if (value instanceof Float)
                newEditor.putFloat(key, (Float) value);
            else if (value instanceof Long)
                newEditor.putLong(key, (Long) value);
            else if (value instanceof java.util.Set)
                newEditor.putStringSet(key, (java.util.Set<String>) value);
        }

        // Clear and remove the old file references if possible
        legacyPrefs.edit().clear().apply();

        // On API 24+, we can delete the file. For older versions, clearing is
        // sufficient.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            deleteSharedPreferences(legacyFileName);
        }
    }

}
