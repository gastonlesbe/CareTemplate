package com.gastonlesbegueris.caretemplate.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = { EventEntity.class, SubjectEntity.class },
        version = 7,                 // <-- SUBÍ la versión
        exportSchema = false
)
public abstract class AppDb extends RoomDatabase {
    public abstract EventDao eventDao();
    public abstract SubjectDao subjectDao();

    private static volatile AppDb I;

    public static AppDb get(Context c) {
        if (I == null) {
            synchronized (AppDb.class) {
                if (I == null) {
                    I = Room.databaseBuilder(c.getApplicationContext(), AppDb.class, "caretemplate.db")
                            // En desarrollo, esto evita migraciones largas:
                            .fallbackToDestructiveMigration()          // <-- borra y recrea
                            .build();
                }
            }
        }
        return I;
    }
}
