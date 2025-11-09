package com.gastonlesbegueris.caretemplate.data.model;

import androidx.annotation.Nullable;

public class DaySummary {
    private final long dayStartMillis;
    private final int eventsCount;
    private final int realizedCount;
    @Nullable private final Double expensesSum;
    @Nullable private final String label; // opcional (por si algún código lo usa)

    public DaySummary(long dayStartMillis, int eventsCount, int realizedCount, @Nullable Double expensesSum) {
        this(dayStartMillis, eventsCount, realizedCount, expensesSum, null);
    }

    public DaySummary(long dayStartMillis, int eventsCount, int realizedCount, @Nullable Double expensesSum, @Nullable String label) {
        this.dayStartMillis = dayStartMillis;
        this.eventsCount = eventsCount;
        this.realizedCount = realizedCount;
        this.expensesSum = expensesSum;
        this.label = label;
    }

    public long getDayStartMillis() { return dayStartMillis; }
    public int getEventsCount()     { return eventsCount; }
    public int getRealizedCount()   { return realizedCount; }
    @Nullable public Double getExpensesSum() { return expensesSum; }
    @Nullable public String getLabel() { return label; }
}
