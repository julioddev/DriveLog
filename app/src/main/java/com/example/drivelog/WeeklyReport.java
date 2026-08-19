package com.example.drivelog;

public class WeeklyReport {
    public String period;
    public double totalEarnings;
    public double totalFuel;

    public WeeklyReport(String period, double totalEarnings, double totalFuel) {
        this.period = period;
        this.totalEarnings = totalEarnings;
        this.totalFuel = totalFuel;
    }
}