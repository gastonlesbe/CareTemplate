package com.gastonlesbegueris.caretemplate.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "subjects")
public class SubjectEntity {
    @PrimaryKey @NonNull public String id;

    @ColumnInfo(name = "appType") public String appType;
    @ColumnInfo(name = "name")    public String name;

    @ColumnInfo(name = "birthDate")     public Long birthDate;      // nullable
    @ColumnInfo(name = "currentMeasure") public Double currentMeasure; // peso/km, nullable
    @ColumnInfo(name = "notes")         public String notes;

    @ColumnInfo(name = "iconKey") public String iconKey; // "dog","car",...
    @ColumnInfo(name = "colorHex") public String colorHex;

    @ColumnInfo(name = "updatedAt") public long updatedAt;
    @ColumnInfo(name = "deleted")   public int deleted; // 0/1
    @ColumnInfo(name = "dirty")     public int dirty;   // 0/1
}
