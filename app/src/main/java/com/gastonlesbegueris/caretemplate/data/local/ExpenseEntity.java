package com.gastonlesbegueris.caretemplate.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "expenses")
public class ExpenseEntity {
    @PrimaryKey @NonNull public String id;  // UUID
    public String appType;
    public String subjectId;
    public double amount;
    public String currency;  // "USD", "ARS", etc.
    public String category;  // "Vet", "Service", "Food", etc.
    public String note;
    public long   atDate;    // fecha del gasto
    public long   updatedAt;
    public int    deleted;
    public int    dirty;
}
