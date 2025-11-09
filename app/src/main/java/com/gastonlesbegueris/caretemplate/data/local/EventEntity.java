package com.gastonlesbegueris.caretemplate.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "events")
public class EventEntity {
    @PrimaryKey @NonNull public String id;

    public String uid;            // opcional
    public String appType;        // "pets" | "cars" | "family" | "house"
    public String subjectId;      // FK lógico a SubjectEntity.id

    public String title;
    public String note;

    public Double cost;           // costo planeado/real
    public int realized;          // 0 = pendiente, 1 = realizado

    public long dueAt;            // fecha/hora planeada (millis)
    public Long realizedAt;       // <-- NUEVA columna: cuándo se marcó como realizado (millis) - puede ser null

    public long updatedAt;        // auditoría
    public int deleted;           // 0/1 (borrado lógico)
    public int dirty;             // 0/1 (pendiente de sync)
}
