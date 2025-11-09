package com.gastonlesbegueris.caretemplate.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.gastonlesbegueris.caretemplate.data.model.MonthTotal;

import java.util.List;

@Dao
public interface EventDao {

    // -------- CRUD básicos ya existentes (mantén los tuyos) --------
    @Insert
    void insert(EventEntity e);

    @Update
    void update(EventEntity e);

    @Query("UPDATE events SET deleted=1, dirty=1, updatedAt=:now WHERE id=:id")
    void softDelete(String id, long now);

    @Query("SELECT * FROM events WHERE appType=:app AND deleted=0 ORDER BY dueAt ASC")
    LiveData<List<EventEntity>> observeActive(String app);

    // Realizado / no realizado
    @Query("UPDATE events SET realized=1, realizedAt=:now, updatedAt=:now, dirty=1 WHERE id=:id")
    void markRealizedOne(String id, long now);

    @Query("UPDATE events SET realized=0, realizedAt=NULL, updatedAt=:now, dirty=1 WHERE id=:id")
    void markUnrealizedOne(String id, long now);

    // Costo directo sobre un evento
    @Query("UPDATE events SET cost=:cost, updatedAt=:now, dirty=1 WHERE id=:id")
    void setCost(String id, Double cost, long now);

    // Próximo evento (para header)
    @Query("SELECT * FROM events " +
            "WHERE appType=:app AND deleted=0 " +
            "AND (:subjectId IS NULL OR subjectId=:subjectId) " +
            "AND dueAt >= :fromMillis " +
            "ORDER BY dueAt ASC LIMIT 1")
    EventEntity nextEvent(String app, String subjectId, long fromMillis);

    // Listar vencidos y no realizados (para auto-realizar)
    @Query("SELECT * FROM events WHERE appType=:app AND deleted=0 AND realized=0 AND dueAt < :now ORDER BY dueAt ASC")
    List<EventEntity> listDueUnrealized(String app, long now);

    // Marcar varios como realizados
    @Query("UPDATE events SET realized=1, realizedAt=:now, updatedAt=:now, dirty=1 WHERE id IN (:ids)")
    void markRealized(List<String> ids, long now);

    // ---------- NUEVO: Totales mensuales alineados a MonthTotal ----------
    // monthStart en millis = strftime('%Y-%m-01 00:00:00') -> epoch sec -> *1000
    @Query("" +
            "SELECT " +
            "   (strftime('%s', strftime('%Y-%m-01 00:00:00', datetime(dueAt/1000,'unixepoch'))) * 1000) AS monthStart, " +
            "   SUM(CASE WHEN realized=0 THEN COALESCE(cost,0) ELSE 0 END) AS plannedSum, " +
            "   SUM(CASE WHEN realized=1 THEN COALESCE(cost,0) ELSE 0 END) AS realizedSum " +
            "FROM events " +
            "WHERE appType=:app AND deleted=0 " +
            "GROUP BY strftime('%Y', datetime(dueAt/1000,'unixepoch')), strftime('%m', datetime(dueAt/1000,'unixepoch')) " +
            "ORDER BY monthStart DESC")
    List<MonthTotal> listMonthTotals(String app);

    // ---------- Helpers opcionales que pedían Activities ----------
    @Query("SELECT SUM(COALESCE(cost,0)) FROM events " +
            "WHERE appType=:app AND deleted=0 AND realized=0 AND dueAt BETWEEN :from AND :to")
    Double sumPlannedCostInRange(String app, long from, long to);

    @Query("SELECT SUM(COALESCE(cost,0)) FROM events " +
            "WHERE appType=:app AND deleted=0 AND realized=1 AND dueAt BETWEEN :from AND :to")
    Double sumRealizedCostInRange(String app, long from, long to);

    // Observación por rango (si alguna Activity pide por día)
    @Query("SELECT * FROM events " +
            "WHERE appType=:app AND deleted=0 AND dueAt BETWEEN :from AND :to " +
            "ORDER BY dueAt ASC")
    LiveData<List<EventEntity>> observeByDay(String app, long from, long to);
}
