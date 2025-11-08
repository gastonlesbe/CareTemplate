package com.gastonlesbegueris.caretemplate.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.*;

import java.util.List;

@Dao
public interface EventDao {

    // ===== CRUD básicos =====
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EventEntity e);

    // --- Soporte de Sync con filtro por appType ---
    @Query("SELECT * FROM events WHERE dirty = 1 AND deleted = 0 AND appType = :appType")
    List<EventEntity> listDirty(String appType);



    @Update
    void update(EventEntity e);

    @Query("UPDATE events SET deleted = 1, dirty = 1, updatedAt = :now WHERE id = :id")
    void softDelete(String id, long now);

    // ===== Observaciones / listados =====
    @Query("SELECT * FROM events WHERE appType = :appType AND deleted = 0 ORDER BY dueAt ASC")
    LiveData<List<EventEntity>> observeActive(String appType);

    @Query("SELECT * FROM events " +
            "WHERE appType = :appType AND deleted = 0 AND dueAt >= :start AND dueAt < :end " +
            "ORDER BY dueAt ASC")
    LiveData<List<EventEntity>> observeByDay(String appType, long start, long end);

    @Query("SELECT * FROM events " +
            "WHERE appType = :appType AND deleted = 0 AND dueAt >= :start AND dueAt < :end " +
            "ORDER BY dueAt ASC")
    List<EventEntity> listInRange(String appType, long start, long end);

    @Query("SELECT * FROM events " +
            "WHERE appType = :appType AND deleted = 0 AND realized = 0 AND dueAt < :now " +
            "ORDER BY dueAt ASC")
    List<EventEntity> listDueUnrealized(String appType, long now);

    // Próximo evento global o por sujeto
    @Query("SELECT * FROM events " +
            "WHERE appType = :appType AND deleted = 0 AND (:subjectId IS NULL OR subjectId = :subjectId) " +
            "AND dueAt >= :now " +
            "ORDER BY dueAt ASC LIMIT 1")
    EventEntity nextEvent(String appType, String subjectId, long now);

    // ===== Totales / sumas =====
    @Query("SELECT SUM(cost) FROM events " +
            "WHERE appType = :appType AND deleted = 0 AND realized = 0 " +
            "AND dueAt >= :start AND dueAt < :end")
    Double sumPlannedCostInRange(String appType, long start, long end);

    @Query("SELECT SUM(cost) FROM events " +
            "WHERE appType = :appType AND deleted = 0 AND realized = 1 " +
            "AND dueAt >= :start AND dueAt < :end")
    Double sumRealizedCostInRange(String appType, long start, long end);

    // ===== Realizado / costo =====
    @Query("UPDATE events SET realized = 1, updatedAt = :now, dirty = 1 WHERE id IN (:ids)")
    void markRealized(List<String> ids, long now);

    @Query("UPDATE events SET realized = 1, updatedAt = :now, dirty = 1 WHERE id = :id")
    void markRealizedOne(String id, long now);

    @Query("UPDATE events SET realized = 0, updatedAt = :now, dirty = 1 WHERE id = :id")
    void markUnrealizedOne(String id, long now);

    @Query("UPDATE events SET cost = :cost, updatedAt = :now, dirty = 1 WHERE id = :id")
    void setCost(String id, Double cost, long now);

    // ===== Soporte de Sync (CloudSync) =====
    @Query("SELECT * FROM events WHERE dirty = 1 AND deleted = 0")
    List<EventEntity> listDirty();

    @Query("UPDATE events SET dirty = 0 WHERE id IN (:ids)")
    void markClean(List<String> ids);

    @Query("SELECT MAX(updatedAt) FROM events WHERE appType = :appType")
    Long lastUpdatedForApp(String appType);

    // (Opcional) por sujeto para la pantalla principal
    @Query("SELECT * FROM events " +
            "WHERE appType = :appType AND deleted = 0 AND subjectId = :subjectId " +
            "ORDER BY dueAt ASC")
    LiveData<List<EventEntity>> observeBySubject(String appType, String subjectId);

    // Eventos en un rango (para agenda diaria/mes)


    // Lista de eventos realizados en el rango (para Expenses)
    @Query("SELECT * FROM events " +
            "WHERE appType = :appType AND deleted = 0 AND realized = 1 " +
            "AND dueAt >= :start AND dueAt < :end " +
            "ORDER BY dueAt DESC")
    java.util.List<EventEntity> listRealizedInRange(String appType, long start, long end);

    @Query("SELECT * FROM events " +
            "WHERE appType = :app AND deleted = 0 AND realized = 1 " +
            "AND (:subjectId IS NULL OR subjectId = :subjectId) " +
            "AND dueAt BETWEEN :start AND :end " +
            "ORDER BY dueAt DESC")
    List<EventEntity> listExpensesRealized(String app, String subjectId, long start, long end);

    @Query("SELECT SUM(cost) FROM events " +
            "WHERE appType = :app AND deleted = 0 AND realized = 1 " +
            "AND (:subjectId IS NULL OR subjectId = :subjectId) " +
            "AND dueAt BETWEEN :start AND :end")
    Double sumExpensesRealized(String app, String subjectId, long start, long end);
    @androidx.room.Query(
            "SELECT strftime('%Y-%m', datetime(realizedAt/1000,'unixepoch','localtime')) AS ym, " +
                    "       SUM(cost) AS total " +
                    "FROM events " +
                    "WHERE appType = :appType AND realized = 1 " +
                    "  AND realizedAt BETWEEN :fromMillis AND :toMillis " +
                    "GROUP BY ym " +
                    "ORDER BY ym"
    )
    java.util.List<com.gastonlesbegueris.caretemplate.data.model.MonthTotal> listMonthlyTotals(
            String appType, long fromMillis, long toMillis
    );


    @Query("SELECT COUNT(*) FROM events WHERE appType=:appType")
    int countEventsForApp(String appType);




}
