package com.gastonlesbegueris.caretemplate.data.model;

import androidx.room.ColumnInfo;

public class MonthTotal {
    @ColumnInfo(name = "monthStart")
    public String monthStart;      // "YYYY-MM-01" (texto)

    @ColumnInfo(name = "plannedSum")
    public Double plannedSum;

    @ColumnInfo(name = "realizedSum")
    public Double realizedSum;
}
