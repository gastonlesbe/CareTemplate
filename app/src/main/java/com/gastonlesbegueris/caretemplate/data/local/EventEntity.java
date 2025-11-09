package com.gastonlesbegueris.caretemplate.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "events")
public class EventEntity {
    @PrimaryKey @NonNull public String id;

    @ColumnInfo(name = "uid") public String uid;
    @ColumnInfo(name = "appType") public String appType;
    @ColumnInfo(name = "subjectId") public String subjectId;

    @ColumnInfo(name = "title") public String title;
    @ColumnInfo(name = "note")  public String note;

    @ColumnInfo(name = "cost")     public Double cost;     // puede ser null
    @ColumnInfo(name = "realized") public int realized;    // 0/1
    @ColumnInfo(name = "realizedAt") public Long realizedAt; // 👈 NUEVA COLUMNA (nullable)

    @ColumnInfo(name = "dueAt")     public long dueAt;
    @ColumnInfo(name = "updatedAt") public long updatedAt;
    @ColumnInfo(name = "deleted")   public int deleted;    // 0/1
    @ColumnInfo(name = "dirty")     public int dirty;      // 0/1
}
