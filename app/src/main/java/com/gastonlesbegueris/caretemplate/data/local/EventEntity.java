package com.gastonlesbegueris.caretemplate.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "events")
public class EventEntity {
    @PrimaryKey @NonNull public String id;   // UUID local
    public String uid;                       // por ahora opcional
    public String appType;
    public String subjectId;  // "pets"/"cars"/"family"
    public String title;
    public String note;
    public long dueAt;
    public long updatedAt;
    public int deleted;
    public int dirty;
    // 0/1
    // 👇 NUEVO
    @Nullable
    public Double cost;   // costo planeado/real
    public int realized;
}
