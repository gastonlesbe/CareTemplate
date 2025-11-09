// app/src/main/java/com/gastonlesbegueris/caretemplate/data/local/EventEntity.java
package com.gastonlesbegueris.caretemplate.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "events")
public class EventEntity {

    @PrimaryKey @NonNull
    public String id;

    public String uid;
    public String appType;

    public String subjectId;

    public String title;
    public String note;

    public Double cost;
    public int realized;      // 0 = pendiente, 1 = realizado
    public Long realizedAt;   // <-- puede ser NULL

    public long dueAt;
    public long updatedAt;
    public int deleted;
    public int dirty;
}
