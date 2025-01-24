package com.gratus.meditationtrakcer;

import android.app.DatePickerDialog;
import android.content.ContentValues;
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

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
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

        // Initialize RecyclerView
        goalsRecyclerView = findViewById(R.id.goals_recycler_view);
        goalsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        goalsAdapter = new GoalsAdapter(new ArrayList<>());
        goalsRecyclerView.setAdapter(goalsAdapter);

        dbHelper = new GoalsDatabaseHelper(this);
        meditationLogDatabaseHelper = new MeditationLogDatabaseHelper(this);

        // Load Goals
        loadGoals();

        startDateInput.setOnClickListener(v -> showDatePickerDialog(startDateInput));
        endDateInput.setOnClickListener(v -> showDatePickerDialog(endDateInput));
        addGoalButton.setOnClickListener(v -> {
            addGoal();
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

        // Refresh RecyclerView
        loadGoals();
    }

    private void addGoal() {
        String description = goalDescription.getText().toString().trim();
        String target = targetHours.getText().toString().trim();
        String startDate = startDateInput.getText().toString().trim();
        String endDate = endDateInput.getText().toString().trim();

        if (!description.isEmpty() && !target.isEmpty() && !startDate.isEmpty() && !endDate.isEmpty()) {
            try {
                // Parse input date and append a timestamp
                SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());

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
        }

        // Dismiss keyboard after adding goal
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        goalDescription.clearFocus();
        targetHours.clearFocus();
    }


    private double getLoggedHours(String startDateTime, String endDateTime) {
        SQLiteDatabase db = meditationLogDatabaseHelper.getReadableDatabase();

        // Fetch total logged seconds between startDateTime and endDateTime
        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + MeditationLogDatabaseHelper.COLUMN_TOTAL_SECONDS + ") FROM " +
                        MeditationLogDatabaseHelper.TABLE_LOGS +
                        " WHERE datetime(" + MeditationLogDatabaseHelper.COLUMN_DATE + ") BETWEEN datetime(?) AND datetime(?)",
                new String[]{startDateTime, endDateTime}
        );

        double totalHours = 0;
        if (cursor.moveToFirst()) {
            totalHours = cursor.getDouble(0) / 3600.0; // Convert seconds to hours
        }
        cursor.close();

        Log.d("GoalsActivity", "Logged Hours between " + startDateTime + " and " + endDateTime + ": " + totalHours);
        return totalHours;
    }

    private String formatDate(String dateTime) {
        try {
            // Parse the original date format
            SimpleDateFormat originalFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            Date date = originalFormat.parse(dateTime);

            // Format to "MMM dd"
            SimpleDateFormat newFormat = new SimpleDateFormat("MMM dd, " + "yy", Locale.getDefault());
            return newFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return ""; // Return empty string if parsing fails
        }
    }

//    private void loadGoals() {
//        List<Goal> goals = new ArrayList<>();
//        SQLiteDatabase db = dbHelper.getReadableDatabase();
//        Cursor cursor = db.rawQuery("SELECT * FROM " + GoalsDatabaseHelper.TABLE_GOALS, null);
//
//        while (cursor.moveToNext()) {
//            String description = cursor.getString(cursor.getColumnIndex("description"));
//            int targetHours = cursor.getInt(cursor.getColumnIndex("target_hours"));
//            String startDateTime = cursor.getString(cursor.getColumnIndex("start_date"));
//            String endDateTime = cursor.getString(cursor.getColumnIndex("end_date"));
//            int goalId = cursor.getInt(cursor.getColumnIndex(GoalsDatabaseHelper.COLUMN_ID));
//
//            // Calculate progress dynamically
//            double loggedHours = meditationLogDatabaseHelper.getLoggedHours(startDateTime, endDateTime);
//            int progressPercent = (int) ((loggedHours / targetHours) * 100);
//            if (progressPercent > 100) progressPercent = 100;
//
//            // Format dates
//            String formattedStartDate = formatDate(startDateTime);
//            String formattedEndDate = formatDate(endDateTime);
//
//            // Add goal to the list
//            goals.add(new Goal(goalId, description, targetHours, loggedHours, formattedStartDate, formattedEndDate, progressPercent));
//        }
//        cursor.close();
//
//        goalsAdapter.updateGoals(goals); // Refresh RecyclerView
//    }

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

        datePickerDialog.show();
    }

    private void resetInputFields() {
        goalDescription.setText("");
        targetHours.setText("");
        startDateInput.setText("");
        endDateInput.setText("");
        //goalDescription.requestFocus();  // Focus on goal description
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(GoalsActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGoals();  // Refresh goal cards when returning to Goals screen
    }

}
