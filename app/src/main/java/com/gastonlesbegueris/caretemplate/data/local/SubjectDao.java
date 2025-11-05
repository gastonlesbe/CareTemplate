package com.gastonlesbegueris.caretemplate.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;

@Dao
public interface SubjectDao {
    @Query("SELECT * FROM subjects WHERE appType=:app AND deleted=0 ORDER BY updatedAt DESC")
    LiveData<List<SubjectEntity>> observeActive(String app);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SubjectEntity s);

    @Update
    void update(SubjectEntity s);

    @Query("UPDATE subjects SET deleted=1, updatedAt=:ts, dirty=1 WHERE id=:id")
    void softDelete(String id, long ts);

    @Query("SELECT * FROM subjects WHERE id=:id LIMIT 1")
    SubjectEntity findOne(String id);



    @Query("SELECT * FROM subjects WHERE dirty=1")
    List<SubjectEntity> listDirty();

    @Query("SELECT COUNT(*) FROM subjects WHERE appType=:appType AND deleted=0")
    int countForApp(String appType);


    @Query("SELECT * FROM subjects WHERE appType=:appType AND deleted=0 ORDER BY name ASC")
    List<SubjectEntity> listActiveNow(String appType); // lo usás en el diálogo

    // ---- requeridos por CloudSync ----
    @Query("SELECT * FROM subjects WHERE appType=:appType AND dirty=1")
    List<SubjectEntity> listDirty(String appType);

    @Query("UPDATE subjects SET dirty=0 WHERE id IN (:ids)")
    void markClean(List<String> ids);

    @Query("SELECT COALESCE(MAX(updatedAt), 0) FROM subjects WHERE appType=:appType")
    long lastUpdatedForApp(String appType);
}
