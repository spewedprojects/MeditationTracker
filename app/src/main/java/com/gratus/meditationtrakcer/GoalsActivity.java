package com.gratus.meditationtrakcer;

import static com.gratus.meditationtrakcer.utils.ClearFocusUtils.clearFocusOnKeyboardHide;

import android.app.DatePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.app.TimePickerDialog;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.widget.TimePicker;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.gratus.meditationtrakcer.adapters.GoalsAdapter;
import com.gratus.meditationtrakcer.databasehelpers.GoalsDatabaseHelper;
import com.gratus.meditationtrakcer.databasehelpers.MeditationLogDatabaseHelper;
import com.gratus.meditationtrakcer.datamodels.Goal;
import com.gratus.meditationtrakcer.widgets.GoalWidgetProvider;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class GoalsActivity extends BaseActivity {

    // UI Components for Collapse/Expand
    private ImageButton collapseExpandButton;
    private ViewGroup expandableContainer;
    private ViewGroup cardConstraintLayout; // The parent container for animation

    // inside GoalsActivity class
    private static final String PREFS_NAME = "goals_Prefs";
    private static final String KEY_GOALS_EXPANDED = "goals_card_expanded";
    private boolean isExpanded = true;

    private EditText goalDescription, targetHours, startDateInput, endDateInput;
    private Button addGoalButton;
    private GoalsDatabaseHelper dbHelper;
    private MeditationLogDatabaseHelper meditationLogDatabaseHelper;
    private RecyclerView goalsRecyclerView;
    private GoalsAdapter goalsAdapter;

    // ── mode toggle ───────────────────────────────
    private MaterialButtonToggleGroup modeGroup;
    private MaterialButton btnMethodA, btnMethodB;
    private enum GoalInputMode { A, B }
    private GoalInputMode currentMode = GoalInputMode.A;

    // ── Method-A views ────────────────────────────
    private List<View> methodAViews;

    // ── Method-B views ────────────────────────────
    private EditText durationPerDayInput, numDaysInput, bStartDateInput;
    private List<View> methodBViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_goals);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Call setupToolbar to bind toolbar and menu button
        setupToolbar(R.id.toolbar2, R.id.menubutton);

        // Existing initializations
        goalDescription = findViewById(R.id.goal_input);
        targetHours = findViewById(R.id.target_input);
        startDateInput = findViewById(R.id.start_date_input);
        endDateInput = findViewById(R.id.end_date_input);
        addGoalButton = findViewById(R.id.add_goal);
        // ── toggle & Method-B fields ─────────────────────────────
        modeGroup   = findViewById(R.id.A_B_group);
        btnMethodA  = findViewById(R.id.method_A_Button);
        btnMethodB  = findViewById(R.id.method_B_Button);
        // B-specific views
        durationPerDayInput = findViewById(R.id.daily_duration_input);
        numDaysInput        = findViewById(R.id.totaldays_input);
        bStartDateInput     = findViewById(R.id.B_startdate_input);

        clearFocusOnKeyboardHide(goalDescription, goalDescription);

        // 1. Initialize the new Views
        collapseExpandButton = findViewById(R.id.col_exp_button);
        expandableContainer = findViewById(R.id.goals_expandable_container);
        cardConstraintLayout = findViewById(R.id.card_constraint_layout); // ID we added to the CL inside CardView

        // --- NEW CODE STARTS HERE --- (13/01/2026)
        // Restore state from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isExpanded = prefs.getBoolean(KEY_GOALS_EXPANDED, true); // Default to true (expanded) if no key exists

        // Apply the initial state immediately (no animation)
        if (isExpanded) {
            expandableContainer.setVisibility(View.VISIBLE);
            collapseExpandButton.setRotation(0f);
        } else {
            expandableContainer.setVisibility(View.GONE);
            collapseExpandButton.setRotation(180f);
        }
        // --- NEW CODE ENDS HERE ---

        // 2. Set the Listener
        collapseExpandButton.setOnClickListener(v -> toggleCardVisibility());

        // Build view lists for quick show / hide
        methodAViews = Arrays.asList(
                findViewById(R.id.target_input_title), targetHours,
                findViewById(R.id.start_date_input_title), startDateInput,
                findViewById(R.id.end_date_input_title),   endDateInput);
        clearFocusOnKeyboardHide(targetHours, targetHours);

        methodBViews = Arrays.asList(
                findViewById(R.id.daily_duration_title),  durationPerDayInput,
                findViewById(R.id.totaldays_input_title), numDaysInput,
                findViewById(R.id.B_startdate_title),     bStartDateInput);
        clearFocusOnKeyboardHide(numDaysInput, numDaysInput);

        // Make group behave like two mutually-exclusive buttons
        modeGroup.setSingleSelection(true);
        modeGroup.check(R.id.method_A_Button);      // default to A
        highlightActiveButton();                    // dim the inactive one
        setMode(GoalInputMode.A);                   // show A-fields first

        modeGroup.addOnButtonCheckedListener((g, id, state) -> {
            if (!state) return;                     // ignore un-checks
            if (id == R.id.method_A_Button) {
                prefillAFromB();                    // copy-over if possible
                setMode(GoalInputMode.A);
            } else {
                prefillBFromA();
                setMode(GoalInputMode.B);
            }
            highlightActiveButton();
        });

        // Initialize RecyclerView
        goalsRecyclerView = findViewById(R.id.goals_recycler_view);
        goalsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        goalsAdapter = new GoalsAdapter(new ArrayList<>());
        goalsRecyclerView.setAdapter(goalsAdapter);

        dbHelper = new GoalsDatabaseHelper(this);
        meditationLogDatabaseHelper = new MeditationLogDatabaseHelper(this);

        // Load Goals
        loadGoals();

        // —— Method-B picker listeners ————————————————————————————
        durationPerDayInput.setOnClickListener(v -> showTimePickerDialog(durationPerDayInput));
        bStartDateInput.setOnClickListener(v -> showDatePickerDialog(bStartDateInput));

        // digits-only for “No. of Days”
        numDaysInput.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
        numDaysInput.setInputType(InputType.TYPE_CLASS_NUMBER);


        startDateInput.setOnClickListener(v -> showDatePickerDialog(startDateInput));
        endDateInput.setOnClickListener(v -> showDatePickerDialog(endDateInput));
        addGoalButton.setOnClickListener(v -> {
            if (currentMode == GoalInputMode.A) {
                addGoal();               // existing method
            } else {
                addGoalMethodB();        // new one below
            }
            resetInputFields();
            loadGoals(); // Refresh goals
        });
    }

    private void toggleCardVisibility() {
        // Prepare the smooth transition (AutoTransition handles layout changes automatically)
        TransitionManager.beginDelayedTransition(cardConstraintLayout, new AutoTransition());

        if (isExpanded) {
            // COLLAPSE
            expandableContainer.setVisibility(View.GONE);
            // Rotate 180 (Arrow points down to indicate "click to open")
            collapseExpandButton.animate().rotation(180f).setDuration(300).start();
        } else {
            // EXPAND
            expandableContainer.setVisibility(View.VISIBLE);
            // Rotate back to 0 (Arrow points up)
            collapseExpandButton.animate().rotation(0f).setDuration(300).start();
        }

        // Toggle state
        isExpanded = !isExpanded;

        // --- NEW CODE: Save the new state --- (13/01/2026)
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(KEY_GOALS_EXPANDED, isExpanded);
        editor.apply();
    }

    public void deleteGoal(int goalId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted = db.delete(GoalsDatabaseHelper.TABLE_GOALS, GoalsDatabaseHelper.COLUMN_ID + "=?", new String[]{String.valueOf(goalId)});

        if (rowsDeleted > 0) {
            Log.d("GoalsActivity", "Goal deleted successfully.");
            updateHomeWidgets(); // <--- ADD THIS
        } else {
            Log.d("GoalsActivity", "Failed to delete goal.");
        }
        db.close();

        // Refresh RecyclerView
        loadGoals();
    }

    private void addGoal() {
        String description = goalDescription.getText().toString().trim();
        String target = targetHours.getText().toString().trim();
        String startDate = startDateInput.getText().toString().trim();
        String endDate = endDateInput.getText().toString().trim();

        if (description.isEmpty() || target.isEmpty() || startDate.isEmpty() || endDate.isEmpty()) {
            toastInvalidInput();
            return;
        }
            try {
                // Parse input date and append a timestamp
                SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                Date parsedStartDate = inputFormat.parse(startDate);
                Date parsedEndDate = inputFormat.parse(endDate);

                String formattedStartDate = outputFormat.format(parsedStartDate);
                String formattedEndDate = outputFormat.format(parsedEndDate);

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("description", description);
                values.put("target_hours", target);
                values.put("start_date", formattedStartDate);  // Save with timestamp
                values.put("end_date", formattedEndDate);      // Save with timestamp
                db.insert("goals", null, values);

                Log.d("GoalsActivity", "Added Goal - Start Date: " + formattedStartDate + ", End Date: " + formattedEndDate);
                updateHomeWidgets(); // <--- ADD THIS
            } catch (Exception e) {
                Log.e("GoalsActivity", "Error formatting dates", e);
            }

        // Dismiss keyboard after adding goal
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        goalDescription.clearFocus();
        targetHours.clearFocus();
        numDaysInput.clearFocus();
    }

    private String formatDate(String dateTime) {
        try {
            // Parse the original date format
            SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = originalFormat.parse(dateTime);

            // Format to "MMM dd"
            SimpleDateFormat newFormat = new SimpleDateFormat("MMM dd, " + "yy", Locale.getDefault());
            return newFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return ""; // Return empty string if parsing fails
        }
    }


    // Updated (27/01/26)
    private void loadGoals() {
        List<Goal> goals = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // 1. PERFORMANCE: Sort via SQL instead of Collections.reverse()
        // This avoids iterating through the list a second time to reverse it.
        Cursor cursor = db.rawQuery("SELECT * FROM " + GoalsDatabaseHelper.TABLE_GOALS +
                " ORDER BY " + GoalsDatabaseHelper.COLUMN_START_DATE + " DESC", null);

        // 2. PERFORMANCE: Initialize Formatters ONCE outside the loop
        // Creating SimpleDateFormat instances is expensive. Doing it inside a loop is bad practice.
        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat fmtFull = new SimpleDateFormat("MMM dd, yy", Locale.getDefault());
        SimpleDateFormat fmtMonthDay = new SimpleDateFormat("MMM d", Locale.getDefault());
        SimpleDateFormat fmtDay = new SimpleDateFormat("d", Locale.getDefault());
        SimpleDateFormat fmtYear = new SimpleDateFormat("yy", Locale.getDefault());

        // 3. PERFORMANCE: Get Column Indices once
        int idxDesc = cursor.getColumnIndex("description");
        int idxTarget = cursor.getColumnIndex("target_hours");
        int idxStart = cursor.getColumnIndex("start_date");
        int idxEnd = cursor.getColumnIndex("end_date");
        int idxId = cursor.getColumnIndex(GoalsDatabaseHelper.COLUMN_ID);

        while (cursor.moveToNext()) {
            String description = cursor.getString(idxDesc);
            double targetHours = cursor.getDouble(idxTarget);
            String startDateTime = cursor.getString(idxStart);
            String endDateTime = cursor.getString(idxEnd);
            int goalId = cursor.getInt(idxId);

            // Retrieve total seconds (Consider optimizing this N+1 query in the future)
            int totalSeconds = meditationLogDatabaseHelper.getTotalSecondsForRange(startDateTime, endDateTime);
            double loggedHours = totalSeconds / 3600.0;

            int progressPercent = (int) ((loggedHours / targetHours) * 100);
            //if (progressPercent > 100) progressPercent = 100;

            // Parse dates once
            Date sDate, eDate;
            try {
                sDate = parser.parse(startDateTime);
                eDate = parser.parse(endDateTime);
            } catch (Exception e) {
                e.printStackTrace();
                continue; // Skip malformed rows
            }

            // 4. CLEANER: Extracted logic to helper methods
            String dailyTargetStr = calculateDailyTarget(sDate, eDate, targetHours);
            String dateRangeStr = formatDateRange(sDate, eDate, fmtFull, fmtMonthDay, fmtDay, fmtYear);

            goals.add(new Goal(goalId, description, targetHours, loggedHours,
                    formatDate(startDateTime), formatDate(endDateTime),
                    progressPercent, dailyTargetStr, dateRangeStr));
        }
        cursor.close();
        db.close();

        goalsAdapter.updateGoals(goals);
    }

    // Helper: Calculate Daily Target
    private String calculateDailyTarget(Date sDate, Date eDate, double targetHours) {
        long diff = eDate.getTime() - sDate.getTime();
        long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) + 1;
        if (days < 1) days = 1;

        double totalMinutesNeeded = targetHours * 60.0;
        double dailyMinutes = totalMinutesNeeded / days;

        int h = (int) (dailyMinutes / 60);
        int m = (int) Math.round(dailyMinutes % 60);

        if (h > 0) {
            return (m > 0) ? h + "h " + m + "m/d" : h + "h/d";
        } else {
            return m + "m/d";
        }
    }

    // Helper: Format Date Range
    private String formatDateRange(Date sDate, Date eDate, SimpleDateFormat fmtFull,
                                   SimpleDateFormat fmtMonth, SimpleDateFormat fmtDay,
                                   SimpleDateFormat fmtYear) {
        Calendar sCal = Calendar.getInstance(); sCal.setTime(sDate);
        Calendar eCal = Calendar.getInstance(); eCal.setTime(eDate);

        if (sCal.get(Calendar.YEAR) != eCal.get(Calendar.YEAR)) {
            // Different Years
            return fmtFull.format(sDate) + " - " + fmtFull.format(eDate);
        } else {
            // Same Year
            if (sCal.get(Calendar.MONTH) != eCal.get(Calendar.MONTH)) {
                // Different Months: "MMM d-MMM d, yy"
                return fmtMonth.format(sDate) + " - " + fmtMonth.format(eDate) + ", " + fmtYear.format(eDate);
            } else {
                // Same Month: "MMM d–d, yy"
                return fmtMonth.format(sDate) + " – " + fmtDay.format(eDate) + ", " + fmtYear.format(eDate);
            }
        }
    }

    private void showDatePickerDialog(EditText dateInput) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dPDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            String selectedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
            dateInput.setText(selectedDate);
        }, year, month, day);

        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(dateInput.getWindowToken(), 0);
        }

        // Customize button colors when the dialog is shown
        dPDialog.setOnShowListener(dialog -> {
            Button positiveButton = dPDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            Button negativeButton = dPDialog.getButton(DialogInterface.BUTTON_NEGATIVE);

            // Determine the current effective theme
            boolean isDarkMode = isDarkMode();

            // Apply colors based on the theme
            int positiveColor = isDarkMode ? ContextCompat.getColor(this, R.color.inverseprimary) : ContextCompat.getColor(this, R.color.inverseprimary);
            int negativeColor = isDarkMode ? ContextCompat.getColor(this, R.color.inverseprimary) : ContextCompat.getColor(this, R.color.inverseprimary);

            positiveButton.setTextColor(positiveColor);
            negativeButton.setTextColor(negativeColor);

            // Apply rounded background
            Objects.requireNonNull(dPDialog.getWindow()).setBackgroundDrawableResource(R.drawable.datepicker_rounded_corners);
        });

        dPDialog.show();
    }

    private void showTimePickerDialog(EditText timeInput) {
        // ── derive a sensible default ──────────────────────────────
        int hour = 0, minute = 0;                      // default 00:00
        String existing = timeInput.getText().toString();
        if (existing.matches("\\d{2}:\\d{2}")) {       // “HH:MM”
            String[] p = existing.split(":");
            hour   = Integer.parseInt(p[0]);
            minute = Integer.parseInt(p[1]);
        }

        TimePickerDialog tPDialog = new TimePickerDialog(
                this,
                (TimePicker view, int selectedHour, int selectedMinute) -> {
                    String hh = String.format(Locale.US, "%02d", selectedHour);
                    String mm = String.format(Locale.US, "%02d", selectedMinute);
                    timeInput.setText(hh + ":" + mm);
                },
                hour, minute, true);                     // 24-hour

        // match the colour-tint logic you already use for DatePicker
        tPDialog.setOnShowListener(d -> {
            boolean dark = isDarkMode();
            int colour   = ContextCompat.getColor(this,
                    dark ? R.color.inverseprimary : R.color.inverseprimary);
            tPDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(colour);
            tPDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(colour);
            // Apply rounded background
            Objects.requireNonNull(tPDialog.getWindow()).setBackgroundDrawableResource(R.drawable.datepicker_rounded_corners);
        });

        tPDialog.show();
    }

    private boolean isDarkMode() {
        boolean isDarkMode;
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            // Check the system's current theme
            int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            isDarkMode = (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES);
        } else {
            // Use the app's explicitly set theme
            isDarkMode = (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES);
        }
        return isDarkMode;
    }

    // ──────────────────────────────────────────────────────────
    //  Visibility toggle
    // ──────────────────────────────────────────────────────────

    private void setMode(GoalInputMode mode) {
        currentMode = mode;
        for (View v : methodAViews) v.setVisibility(mode == GoalInputMode.A ? View.VISIBLE : View.INVISIBLE);
        for (View v : methodBViews) v.setVisibility(mode == GoalInputMode.B ? View.VISIBLE : View.INVISIBLE);
    }

    // 70 % dim for inactive button
    private void highlightActiveButton() {
        btnMethodA.setAlpha(currentMode == GoalInputMode.A ? 1f : 0.3f);
        btnMethodB.setAlpha(currentMode == GoalInputMode.B ? 1f : 0.3f);
    }

    // ── Quick pre-fill when switching ─────────────────────────
    private void prefillBFromA() {
        String hrsStr = targetHours.getText().toString().trim();
        String sDate  = startDateInput.getText().toString().trim();
        String eDate  = endDateInput.getText().toString().trim();
        try {
            if (hrsStr.isEmpty() || sDate.isEmpty() || eDate.isEmpty()) return;

            float totalHours = Float.parseFloat(hrsStr);
            SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date s             = fmt.parse(sDate);
            Date e             = fmt.parse(eDate);
            long diffMillis    = e.getTime() - s.getTime();
            int  days          = (int) (diffMillis / (24*60*60*1000)) + 1;
            if (days <= 0) return;

            float hoursPerDay  = totalHours / days;
            int hh = (int) hoursPerDay;
            int mm = Math.round((hoursPerDay - hh) * 60f);

            durationPerDayInput.setText(String.format(Locale.US,"%02d:%02d", hh, mm));
            numDaysInput.setText(String.valueOf(days));
            bStartDateInput.setText(sDate);
        } catch (Exception ignore) {}
    }

    private void prefillAFromB() {
        String dur = durationPerDayInput.getText().toString().trim(); // “HH:MM”
        String nDaysStr = numDaysInput.getText().toString().trim();
        String sDateStr = bStartDateInput.getText().toString().trim();
        try {
            if (dur.isEmpty() || nDaysStr.isEmpty() || sDateStr.isEmpty()) return;

            String[] parts = dur.split(":");
            int hh = Integer.parseInt(parts[0]);
            int mm = Integer.parseInt(parts[1]);
            int numDays = Integer.parseInt(nDaysStr);

            float target = (hh + mm/60f) * numDays;

            SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date start = fmt.parse(sDateStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(start);
            cal.add(Calendar.DATE, numDays - 1);
            String endStr = fmt.format(cal.getTime());

            targetHours.setText(String.format(Locale.US,"%.1f", target));
            startDateInput.setText(sDateStr);
            endDateInput.setText(endStr);
        } catch (Exception ignore) {}
    }

    // ── Method-B “add goal” routine ───────────────────────────
    private void addGoalMethodB() {
        String description = goalDescription.getText().toString().trim();
        String durStr   = durationPerDayInput.getText().toString().trim();  // HH:MM
        String daysStr  = numDaysInput.getText().toString().trim();
        String startStr = bStartDateInput.getText().toString().trim();

        if (description.isEmpty() || durStr.isEmpty() || daysStr.isEmpty() || startStr.isEmpty()) {
            toastInvalidInput();
            return;
        }
        try {
            String[] hhmm = durStr.split(":");
            int hh = Integer.parseInt(hhmm[0]);
            int mm = Integer.parseInt(hhmm[1]);
            if (hh==0 && mm==0) throw new NumberFormatException();

            int numDays = Integer.parseInt(daysStr);
            if (numDays <= 0) throw new NumberFormatException();

            // Derive target-hours & end-date
            float target = (hh + mm/60f) * numDays;

            SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date startDate = fmt.parse(startStr);
            Calendar cal   = Calendar.getInstance();
            cal.setTime(startDate);
            cal.add(Calendar.DATE, numDays - 1);
            String endStr  = fmt.format(cal.getTime());

            // Re-use the existing addGoal() plumbing by filling A-fields then calling it
            targetHours.setText(String.valueOf(target));
            startDateInput.setText(startStr);
            endDateInput.setText(endStr);

            addGoal();                   // existing DB insert
        } catch (Exception e) {
            toastInvalidInput();
        }
    }

    private void toastInvalidInput() {
        Toast.makeText(this,"Invalid input", Toast.LENGTH_SHORT).show();
    }

    private void resetInputFields() {
        goalDescription.setText("");
        // ── Method A fields ──
        targetHours.setText("");
        startDateInput.setText("");
        endDateInput.setText("");

        // ── Method B fields ──
        durationPerDayInput.setText("");
        numDaysInput.setText("");
        bStartDateInput.setText("");
    }

    private void updateHomeWidgets() {
        Intent intent = new Intent(this, GoalWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        // Get all IDs for the GoalWidget
        int[] ids = AppWidgetManager.getInstance(getApplication())
                .getAppWidgetIds(new ComponentName(getApplication(), GoalWidgetProvider.class));

        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGoals();  // Refresh goal cards when returning to Goals screen
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
        if (meditationLogDatabaseHelper != null) meditationLogDatabaseHelper.close();
    }
}
