package com.gastonlesbegueris.caretemplate.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "subjects")
public class SubjectEntity {

    @PrimaryKey @NonNull
    public String id;

    public String appType;
    public String name;
    public Long birthDate;
    public Double currentMeasure;
    public String notes;
    public String iconKey;
    public String colorHex;

    public long updatedAt;
    public int deleted;
    public int dirty;
}
