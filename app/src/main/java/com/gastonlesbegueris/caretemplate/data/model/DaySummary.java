package com.gastonlesbegueris.caretemplate.data.model;

public class DaySummary {
    private long dayStartMillis;   // 00:00 del día
    private int eventsCount;       // cantidad total de eventos del día
    private int realizedCount;     // cantidad de eventos marcados como realizados
    private Double expensesSum;    // suma de costos (puede ser null si no hay)

    public DaySummary(long dayStartMillis, int eventsCount, int realizedCount, Double expensesSum) {
        this.dayStartMillis = dayStartMillis;
        this.eventsCount = eventsCount;
        this.realizedCount = realizedCount;
        this.expensesSum = expensesSum;
    }

    public long getDayStartMillis() { return dayStartMillis; }
    public int getEventsCount() { return eventsCount; }
    public int getRealizedCount() { return realizedCount; }
    public Double getExpensesSum() { return expensesSum; }

    public void setDayStartMillis(long v) { this.dayStartMillis = v; }
    public void setEventsCount(int v) { this.eventsCount = v; }
    public void setRealizedCount(int v) { this.realizedCount = v; }
    public void setExpensesSum(Double v) { this.expensesSum = v; }
}
