// app/src/main/java/com/gastonlesbegueris/caretemplate/data/local/AppDb.java
package com.gastonlesbegueris.caretemplate.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = { EventEntity.class, SubjectEntity.class },
        version = 9,                // ⬅️ SUBIR número de versión
        exportSchema = false
)
public abstract class AppDb extends RoomDatabase {
    public abstract EventDao eventDao();
    public abstract SubjectDao subjectDao();

    private static volatile AppDb INSTANCE;

    public static AppDb get(Context ctx) {
        if (INSTANCE == null) {
            synchronized (AppDb.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(ctx.getApplicationContext(),
                                    AppDb.class, "caretemplate.db")
                            .fallbackToDestructiveMigration() // simple & rápido mientras iteramos
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
