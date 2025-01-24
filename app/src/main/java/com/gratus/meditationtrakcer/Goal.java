package com.gratus.meditationtrakcer;

public class Goal {
    private int id;
    private String description;
    private int targetHours;
    private double loggedHours;
    private String startDate;
    private String endDate;
    private int progressPercent;

    public Goal(int id, String description, int targetHours, double loggedHours, String startDate, String endDate, int progressPercent) {
        this.id = id;
        this.description = description;
        this.targetHours = targetHours;
        this.loggedHours = loggedHours;
        this.startDate = startDate;
        this.endDate = endDate;
        this.progressPercent = progressPercent;
    }

    // Getters
    public int getId() { return id; }
    public String getDescription() { return description; }
    public int getTargetHours() { return targetHours; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public int getProgressPercent() { return progressPercent; }

    // Setters
    public void setLoggedHours(double loggedHours) {
        this.loggedHours = loggedHours;
    }

    public void setProgressPercent(int progressPercent) {
        this.progressPercent = progressPercent;
    }

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
                '}';
    }
}
