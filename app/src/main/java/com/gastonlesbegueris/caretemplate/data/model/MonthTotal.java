package com.gastonlesbegueris.caretemplate.data.model;

import androidx.room.ColumnInfo;

public class MonthTotal {
    @ColumnInfo(name = "monthStart") public long monthStart;
    @ColumnInfo(name = "plannedSum") public Double plannedSum;
    @ColumnInfo(name = "realizedSum") public Double realizedSum;

    public MonthTotal(long monthStart, Double plannedSum, Double realizedSum) {
        this.monthStart = monthStart;
        this.plannedSum = plannedSum;
        this.realizedSum = realizedSum;
    }
}
