package com.gastonlesbegueris.caretemplate.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.gastonlesbegueris.caretemplate.data.local.EventDao;
import com.gastonlesbegueris.caretemplate.data.local.EventEntity;
import com.gastonlesbegueris.caretemplate.data.local.SubjectDao;
import com.gastonlesbegueris.caretemplate.data.local.SubjectEntity;

@Database(
        entities = { EventEntity.class, SubjectEntity.class },
        version = 10,          // 👈 SUBÍ el número
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
                            .fallbackToDestructiveMigration()   // 👈 rápido para dev
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
