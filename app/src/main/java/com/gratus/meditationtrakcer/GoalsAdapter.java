package com.gratus.meditationtrakcer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
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
            deleteButton = itemView.findViewById(R.id.delete_goal_button); // Initialize deleteButton
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

        Log.d("GoalsAdapter", "Start Date: " + goal.getStartDate() + ", End Date: " + goal.getEndDate());

        holder.goalTitle.setText(goal.getDescription());
        holder.durationView.setText("Target: " + goal.getTargetHours() + " hours | " +
                startDate + " - " + endDate);
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
