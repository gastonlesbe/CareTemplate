package com.gastonlesbegueris.caretemplate.data.local;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

@Database(
        entities = { EventEntity.class, SubjectEntity.class },
        version = 6,                      // ⬅️ incrementá si ya usaste 6
        exportSchema = false              // ⬅️ saca el warning
)
public abstract class AppDb extends RoomDatabase {

    public abstract EventDao eventDao();
    public abstract SubjectDao subjectDao();

    private static volatile AppDb INSTANCE;

    public static AppDb get(Context ctx) {
        if (INSTANCE == null) {
            synchronized (AppDb.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    ctx.getApplicationContext(),
                                    AppDb.class, "caretemplate.db")
                            .fallbackToDestructiveMigration() // rápido para dev; luego migraciones reales
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
