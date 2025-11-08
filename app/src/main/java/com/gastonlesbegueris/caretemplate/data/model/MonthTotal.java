package com.gastonlesbegueris.caretemplate.data.model;

import androidx.annotation.Nullable;

public class MonthTotal {
    public String ym;        // "2025-01", "2025-02", ...
    @Nullable public Double total; // puede venir null
}
