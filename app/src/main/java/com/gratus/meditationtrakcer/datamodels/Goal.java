package com.gratus.meditationtrakcer.datamodels;

public class Goal {
    private int id;
    private String description;
    private double targetHours;
    private double loggedHours;
    private String startDate;
    private String endDate;
    private int progressPercent;
    private String dailyTarget; // New field (14/01/26)
    private String dateRange; // New field for the formatted range (14/01/26)

    public Goal(int id, String description, double targetHours, double loggedHours, String startDate, String endDate, int progressPercent, String dailyTarget, String dateRange) {
        this.id = id;
        this.description = description;
        this.targetHours = targetHours;
        this.loggedHours = loggedHours;
        this.startDate = startDate;
        this.endDate = endDate;
        this.progressPercent = progressPercent;
        this.dailyTarget = dailyTarget;
        this.dateRange = dateRange;
    }

    // Getters
    public int getId() { return id; }
    public String getDescription() { return description; }
    public double getTargetHours() { return targetHours; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public int getProgressPercent() { return progressPercent; }
    public String getDailyTarget() { return dailyTarget; } // New Getter
    public String getDateRange() { return dateRange; } // New Getter

    @Override
    public String toString() {
        return "Goal{" +
                "id=" + id +
                ", description='" + description + '\'' +
                ", targetHours=" + targetHours +
                ", loggedHours=" + loggedHours +
                ", startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                ", progressPercent=" + progressPercent +
                ", dailyTarget='" + dailyTarget + '\'' +
                ", dateRange='" + dateRange + '\'' +
                '}';
    }
}
