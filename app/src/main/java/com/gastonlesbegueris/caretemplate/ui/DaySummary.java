package com.gastonlesbegueris.caretemplate.ui;

public class DaySummary {
    private final long dayStartMillis;   // inicio del día (00:00) en millis
    private final int eventsCount;       // total de eventos del día
    private final int realizedCount;     // cuántos realizados
    private final Double expensesSum;    // suma de costos (solo realizados con costo)

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
}
