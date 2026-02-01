package com.gratus.meditationtrakcer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gratus.meditationtrakcer.datamodels.Goal;
import com.gratus.meditationtrakcer.GoalsActivity;
import com.gratus.meditationtrakcer.R;

import java.util.List;
import java.util.Locale;

public class GoalsAdapter extends RecyclerView.Adapter<GoalsAdapter.GoalViewHolder> {
    private List<Goal> goals;

    public GoalsAdapter(List<Goal> goals) {
        this.goals = goals;
    }

    @NonNull
    @Override
    public GoalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.goal_item, parent, false);
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT; // Set to wrap content
        view.setLayoutParams(layoutParams);
        return new GoalViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return goals.size();
    }

    static class GoalViewHolder extends RecyclerView.ViewHolder {
        TextView goalTitle, durationView, percentageView;
        ProgressBar progressBar;
        Button deleteButton; // declare deleteButton

        public GoalViewHolder(@NonNull View itemView) {
            super(itemView);
            goalTitle = itemView.findViewById(R.id.goal_title);
            durationView = itemView.findViewById(R.id.goal_duration);
            progressBar = itemView.findViewById(R.id.goal_progress_bar);
            percentageView = itemView.findViewById(R.id.goal_progress_percentage);
            deleteButton = itemView.findViewById(R.id.delete_report_button); // Initialize deleteButton
        }

        public void bind(Goal goal) {

            // Format dates
            String startDate = goal.getStartDate(); // Already formatted
            String endDate = goal.getEndDate(); // Already formatted

            goalTitle.setText(goal.getDescription());
            durationView.setText("Target: " + goal.getTargetHours() + " hours | " + startDate + " - " + endDate);
            progressBar.setProgress(goal.getProgressPercent());
            percentageView.setText(goal.getProgressPercent() + "% completed");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull GoalViewHolder holder, int position) {
        Goal goal = goals.get(position);

        // Format dates
        String startDate = goal.getStartDate(); // Already formatted
        String endDate = goal.getEndDate(); // Already formatted
        //String dateRange = goal.getDateRange(); // New field

        // ✅ Format targetHours to 1 decimal place (e.g. 8.5)
        double hours = goal.getTargetHours();
        String targetFormatted;
        if (hours == Math.floor(hours)) {
            // It's an integer, no decimals
            targetFormatted = String.format(Locale.US, "%.0f", hours);
        } else {
            // Show one decimal
            targetFormatted = String.format(Locale.US, "%.1f", hours);
        }


        Log.d("GoalsAdapter", "Start Date: " + goal.getStartDate() + ", End Date: " + goal.getEndDate());

        holder.goalTitle.setText(goal.getDescription());
        holder.durationView.setText("Target: " + goal.getDailyTarget() + " | " + targetFormatted + "h | " + goal.getDateRange());
        holder.progressBar.setProgress(goal.getProgressPercent());
        holder.percentageView.setText(goal.getProgressPercent() + "% completed");

        // Simplified delete logic
        holder.deleteButton.setOnClickListener(v -> {
            ((GoalsActivity) holder.itemView.getContext()).deleteGoal(goal.getId());
        });
    }

    public void updateGoals(List<Goal> newGoals) {
        this.goals = newGoals;
        notifyDataSetChanged(); // Notify RecyclerView to refresh the data
    }

}
