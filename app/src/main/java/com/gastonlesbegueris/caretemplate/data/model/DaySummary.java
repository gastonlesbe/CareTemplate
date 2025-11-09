package com.gastonlesbegueris.caretemplate.data.model;

public class DaySummary {
    private final long   dayStartMillis;
    private final int    eventsCount;
    private final int    realizedCount;
    private final Double expensesSum;   // suma de costos realizados ese día (o null)

    public DaySummary(long dayStartMillis, int eventsCount, int realizedCount, Double expensesSum) {
        this.dayStartMillis = dayStartMillis;
        this.eventsCount    = eventsCount;
        this.realizedCount  = realizedCount;
        this.expensesSum    = expensesSum;
    }

    public long   getDayStartMillis() { return dayStartMillis; }
    public int    getEventsCount()    { return eventsCount; }
    public int    getRealizedCount()  { return realizedCount; }
    public Double getExpensesSum()    { return expensesSum; }
}
