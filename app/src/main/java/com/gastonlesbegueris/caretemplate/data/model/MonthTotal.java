package com.gastonlesbegueris.caretemplate.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Ignore;

public class MonthTotal {
    @ColumnInfo(name = "year")
    public int year;

    @ColumnInfo(name = "month")
    public int month; // 1..12
    @ColumnInfo(name = "monthStart") public long monthStart;     // inicio del mes (00:00 UTC) en millis
    @ColumnInfo(name = "plannedSum") public double plannedSum;    // suma costos de eventos no realizados
    @ColumnInfo(name = "realizedSum") public double realizedSum;  // suma costos de eventos realizados

    @Ignore
    public void computeMonthStartLocalTZ() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.YEAR, year);
        cal.set(java.util.Calendar.MONTH, month - 1);
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        monthStart = cal.getTimeInMillis();
    }
}
