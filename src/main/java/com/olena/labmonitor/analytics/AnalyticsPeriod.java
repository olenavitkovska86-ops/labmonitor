package com.olena.labmonitor.analytics;

public enum AnalyticsPeriod {
    LAST_24_HOURS(1),
    LAST_7_DAYS(7),
    LAST_30_DAYS(30);

    private final int days;

    AnalyticsPeriod(int days) {
        this.days = days;
    }

    public int days() {
        return days;
    }
}
