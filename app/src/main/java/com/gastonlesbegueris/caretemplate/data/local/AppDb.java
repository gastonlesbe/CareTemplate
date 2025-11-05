package com.gastonlesbegueris.caretemplate.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = { EventEntity.class, SubjectEntity.class },
        version = 7,               // ⬅️ subilo si cambiaste schema
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
                            .fallbackToDestructiveMigration() // o usá migraciones si ya tenés
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
