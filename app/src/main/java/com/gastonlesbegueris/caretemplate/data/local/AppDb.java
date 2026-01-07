package com.gastonlesbegueris.caretemplate.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.gastonlesbegueris.caretemplate.data.local.EventDao;
import com.gastonlesbegueris.caretemplate.data.local.EventEntity;
import com.gastonlesbegueris.caretemplate.data.local.SubjectDao;
import com.gastonlesbegueris.caretemplate.data.local.SubjectEntity;

@Database(entities = { EventEntity.class, SubjectEntity.class }, version = 15, exportSchema = false)
public abstract class AppDb extends RoomDatabase {
    private static volatile AppDb I;
    public abstract EventDao eventDao();
    public abstract SubjectDao subjectDao();

    @SuppressWarnings("deprecation")
    public static AppDb get(Context c) {
        if (I == null) {
            synchronized (AppDb.class) {
                if (I == null) {
                    I = Room.databaseBuilder(c.getApplicationContext(), AppDb.class, "caretemplate.db")
                            .addMigrations(
                                    Migrations.MIGRATION_11_12,  // 11→12
                                    Migrations.MIGRATION_12_13,  // 12→13
                                    Migrations.MIGRATION_13_14,  // 13→14
                                    Migrations.MIGRATION_14_15,  // 14→15
                                    Migrations.MIGRATION_11_14,   // 11→14 (salto directo)
                                    Migrations.MIGRATION_12_14,   // 12→14 (salto directo)
                                    Migrations.MIGRATION_11_15,   // 11→15 (salto directo)
                                    Migrations.MIGRATION_12_15,   // 12→15 (salto directo)
                                    Migrations.MIGRATION_13_15    // 13→15 (salto directo)
                            )
                            .fallbackToDestructiveMigration()   // Fallback solo si falla la migración
                            .build();
                }
            }
        }
        return I;
    }
}
