package com.gratus.meditationtrakcer;

public class Streak {
    private int id;
    private String startDate;
    private String endDate;
    private int targetDays;
    private int achievedDays; // Add this field

    public Streak(int id, String startDate, String endDate, int targetDays, int achievedDays) {
        this.id = id;
        this.startDate = startDate;
        this.endDate = endDate;
        this.targetDays = targetDays;
        this.achievedDays = achievedDays; // Add to constructor
    }

    // Getters
    public int getId() { return id; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public int getTargetDays() { return targetDays; }
    public int getAchievedDays() { return achievedDays; } // Add this getter
}