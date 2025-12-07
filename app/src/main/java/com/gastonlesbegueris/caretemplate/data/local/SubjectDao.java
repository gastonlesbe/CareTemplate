package com.gastonlesbegueris.caretemplate.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.*;

import java.util.List;

@Dao
public interface SubjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SubjectEntity s);

    @Update
    void update(SubjectEntity s);
    // --- Soporte de Sync con filtro por appType ---
    // Incluir también los borrados para sincronizar las eliminaciones
    @Query("SELECT * FROM subjects WHERE dirty = 1 AND appType = :appType")
    List<SubjectEntity> listDirty(String appType);


    @Query("UPDATE subjects SET deleted = 1, dirty = 1, updatedAt = :now WHERE id = :id")
    void softDelete(String id, long now);

    @Query("SELECT * FROM subjects WHERE appType = :appType AND deleted = 0 ORDER BY name ASC")
    LiveData<List<SubjectEntity>> observeActive(String appType);

    @Query("SELECT * FROM subjects WHERE appType = :appType AND deleted = 0 ORDER BY name ASC")
    List<SubjectEntity> listActiveNow(String appType);

    @Query("SELECT COUNT(*) FROM subjects WHERE appType = :appType AND deleted = 0")
    int countForApp(String appType);

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    SubjectEntity findOne(String id);

    // Sync helpers
    // Incluir también los borrados para sincronizar las eliminaciones
    @Query("SELECT * FROM subjects WHERE dirty = 1")
    List<SubjectEntity> listDirty();

    @Query("UPDATE subjects SET dirty = 0 WHERE id IN (:ids)")
    void markClean(List<String> ids);

    @Query("DELETE FROM subjects WHERE id = :id")
    void deletePermanently(String id);

    @Query("SELECT MAX(updatedAt) FROM subjects WHERE appType = :appType")
    Long lastUpdatedForApp(String appType);
}
