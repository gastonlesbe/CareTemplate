package com.gastonlesbegueris.caretemplate.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "events")
public class EventEntity {
    @PrimaryKey @NonNull public String id;   // 👈 obligatorio @NonNull
    public String uid;                       // opcional
    public String appType;

    public String subjectId;
    public String title;
    public String note;

    public Long   dueAt;         // millis
    public Double cost;          // puede ser null
    public int    realized;      // 0/1
    public Long   realizedAt;    // 👈 NUEVO: millis cuando se marca realizado

    public long   updatedAt;     // millis
    public int    deleted;       // 0/1
    public int    dirty;         // 0/1 (pendiente de sync)
}
