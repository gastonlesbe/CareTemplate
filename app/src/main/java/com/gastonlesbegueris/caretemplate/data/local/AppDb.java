package com.gastonlesbegueris.caretemplate.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.gastonlesbegueris.caretemplate.data.local.EventDao;
import com.gastonlesbegueris.caretemplate.data.local.EventEntity;
import com.gastonlesbegueris.caretemplate.data.local.SubjectDao;
import com.gastonlesbegueris.caretemplate.data.local.SubjectEntity;

@Database(entities = { EventEntity.class, SubjectEntity.class }, version = 11, exportSchema = false)
public abstract class AppDb extends RoomDatabase {
    private static volatile AppDb I;
    public abstract EventDao eventDao();
    public abstract SubjectDao subjectDao();

    public static AppDb get(Context c) {
        if (I == null) {
            synchronized (AppDb.class) {
                if (I == null) {
                    I = Room.databaseBuilder(c.getApplicationContext(), AppDb.class, "caretemplate.db")
                            .fallbackToDestructiveMigration()   // SOLO dev
                            .build();
                }
            }
        }
        return I;
    }
}
