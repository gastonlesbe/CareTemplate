package com.gastonlesbegueris.caretemplate.ui;

public class DaySummary {
    long dayStartMillis;
    int eventsCount;
    int realizedCount;
    Double expensesSum;

    // 👇 nuevo
    public String mainColorHex;  // puede ser "#FF0000" o null

    public DaySummary(long dayStartMillis, int eventsCount, int realizedCount, Double expensesSum, String mainColorHex) {
        this.dayStartMillis = dayStartMillis;
        this.eventsCount = eventsCount;
        this.realizedCount = realizedCount;
        this.expensesSum = expensesSum;
        this.mainColorHex = mainColorHex;
    }

    public long getDayStartMillis() { return dayStartMillis; }
    public int getEventsCount() { return eventsCount; }
    public int getRealizedCount() { return realizedCount; }
    public Double getExpensesSum() { return expensesSum; }
}
