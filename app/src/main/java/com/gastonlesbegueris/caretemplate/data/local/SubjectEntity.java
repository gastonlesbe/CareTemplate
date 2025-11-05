package com.gastonlesbegueris.caretemplate.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "subjects")
public class SubjectEntity {
    @PrimaryKey @NonNull public String id;       // UUID
    public String appType;                       // "pets" | "cars" | "family" | "house"
    public String name;
    public Long   birthDate;                     // epoch millis (pets/family) - null si no aplica
    public Double currentMeasure;                // peso (kg) o km (double); null si no aplica
    public String photoUrl;                      // opcional
    public String notes;

    public long updatedAt;
    public int  deleted;                         // 0/1
    public int  dirty;
    public String iconKey;   // p.ej. "cat", "dog", "car", "house", "user"
    public String colorHex;  // p.ej. "#03DAC5"// 0 limpio / 1 pendiente sync
}
