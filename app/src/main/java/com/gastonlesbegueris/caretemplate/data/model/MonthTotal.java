package com.gastonlesbegueris.caretemplate.data.model;

import androidx.room.ColumnInfo;

public class MonthTotal {
    @ColumnInfo(name = "year")  public int year;      // 2025
    @ColumnInfo(name = "month") public int month;     // 1..12
    @ColumnInfo(name = "plannedSum")  public double plannedSum;
    @ColumnInfo(name = "realizedSum") public double realizedSum;
}
