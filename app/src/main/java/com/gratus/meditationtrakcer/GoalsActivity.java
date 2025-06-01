package com.gratus.meditationtrakcer;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.TimePickerDialog;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GoalsActivity extends BaseActivity {

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
        setContentView(R.layout.activity_goals);

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

        // Build view lists for quick show / hide
        methodAViews = Arrays.asList(
                findViewById(R.id.target_input_title), targetHours,
                findViewById(R.id.start_date_input_title), startDateInput,
                findViewById(R.id.end_date_input_title),   endDateInput);

        methodBViews = Arrays.asList(
                findViewById(R.id.daily_duration_title),  durationPerDayInput,
                findViewById(R.id.totaldays_input_title), numDaysInput,
                findViewById(R.id.B_startdate_title),     bStartDateInput);

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
        bStartDateInput   .setOnClickListener(v -> showDatePickerDialog(bStartDateInput));

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

    public void deleteGoal(int goalId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted = db.delete(GoalsDatabaseHelper.TABLE_GOALS, GoalsDatabaseHelper.COLUMN_ID + "=?", new String[]{String.valueOf(goalId)});

        if (rowsDeleted > 0) {
            Log.d("GoalsActivity", "Goal deleted successfully.");
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

    private void loadGoals() {
        List<Goal> goals = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + GoalsDatabaseHelper.TABLE_GOALS, null);

        while (cursor.moveToNext()) {
            String description = cursor.getString(cursor.getColumnIndex("description"));
            int targetHours = cursor.getInt(cursor.getColumnIndex("target_hours"));
            String startDateTime = cursor.getString(cursor.getColumnIndex("start_date"));
            String endDateTime = cursor.getString(cursor.getColumnIndex("end_date"));
            int goalId = cursor.getInt(cursor.getColumnIndex(GoalsDatabaseHelper.COLUMN_ID));

            // Retrieve total seconds for the goal's date range
            int totalSeconds = meditationLogDatabaseHelper.getTotalSecondsForRange(startDateTime, endDateTime);
            double loggedHours = totalSeconds / 3600.0; // Convert to hours

            // Calculate progress
            int progressPercent = (int) ((loggedHours / targetHours) * 100);
            if (progressPercent > 100) progressPercent = 100;

            // Format dates
            String formattedStartDate = formatDate(startDateTime);
            String formattedEndDate = formatDate(endDateTime);

            goals.add(new Goal(goalId, description, targetHours, loggedHours, formattedStartDate, formattedEndDate, progressPercent));
        }
        cursor.close();
        db.close();

        Collections.reverse(goals); // Reverse list order to show newest first
        goalsAdapter.updateGoals(goals); // Refresh RecyclerView
    }

    private void showDatePickerDialog(EditText dateInput) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            String selectedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
            dateInput.setText(selectedDate);
        }, year, month, day);

        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(dateInput.getWindowToken(), 0);
        }

        // Customize button colors when the dialog is shown
        datePickerDialog.setOnShowListener(dialog -> {
            Button positiveButton = datePickerDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            Button negativeButton = datePickerDialog.getButton(DialogInterface.BUTTON_NEGATIVE);

            // Determine the current effective theme
            boolean isDarkMode = isDarkMode();

            // Apply colors based on the theme
            int positiveColor = isDarkMode ? ContextCompat.getColor(this, R.color.light_primaryVariant) : ContextCompat.getColor(this, R.color.dark_primary);
            int negativeColor = isDarkMode ? ContextCompat.getColor(this, R.color.light_primaryVariant) : ContextCompat.getColor(this, R.color.dark_primary);

            positiveButton.setTextColor(positiveColor);
            negativeButton.setTextColor(negativeColor);
        });

        datePickerDialog.show();
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

        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (TimePicker view, int selectedHour, int selectedMinute) -> {
                    String hh = String.format(Locale.US, "%02d", selectedHour);
                    String mm = String.format(Locale.US, "%02d", selectedMinute);
                    timeInput.setText(hh + ":" + mm);
                },
                hour, minute, true);                     // 24-hour

        // match the colour-tint logic you already use for DatePicker
        dialog.setOnShowListener(d -> {
            boolean dark = isDarkMode();
            int colour   = ContextCompat.getColor(this,
                    dark ? R.color.light_primaryVariant : R.color.dark_primary);
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(colour);
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(colour);
        });

        dialog.show();
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

//    @Override
//    public void onBackPressed() {
//        super.onBackPressed();
//        Intent intent = new Intent(GoalsActivity.this, MainActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);
//        finish();
//    }

    // ──────────────────────────────────────────────────────────
    //  Visibility toggle
    // ──────────────────────────────────────────────────────────

    private void setMode(GoalInputMode mode) {
        currentMode = mode;
        for (View v : methodAViews) v.setVisibility(mode == GoalInputMode.A ? View.VISIBLE : View.INVISIBLE);
        for (View v : methodBViews) v.setVisibility(mode == GoalInputMode.B ? View.VISIBLE : View.INVISIBLE);
    }

    // 60 % dim for inactive button
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
