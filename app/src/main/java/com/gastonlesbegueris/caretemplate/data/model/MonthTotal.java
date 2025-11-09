package com.gastonlesbegueris.caretemplate.data.model;

import androidx.room.ColumnInfo;

public class MonthTotal {
    /** inicio de mes en millis */
    @ColumnInfo(name = "monthStart")
    public long monthStart;

    /** suma de costos planificados (realized=0) */
    @ColumnInfo(name = "plannedSum")
    public Double plannedSum;

    /** suma de costos realizados (realized=1) */
    @ColumnInfo(name = "realizedSum")
    public Double realizedSum;
}
