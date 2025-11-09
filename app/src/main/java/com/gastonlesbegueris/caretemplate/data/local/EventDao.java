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

    @Insert
    void insert(EventEntity e);

    @Update
    void update(EventEntity e);

    @Query("UPDATE events SET deleted=1, updatedAt=:now, dirty=1 WHERE id=:id")
    void softDelete(String id, long now);

    @Query("SELECT * FROM events WHERE appType=:appType AND deleted=0 ORDER BY dueAt ASC")
    LiveData<List<EventEntity>> observeActive(String appType);

    // Para header (siguiente evento)
    @Query("SELECT * FROM events WHERE appType=:appType AND deleted=0 AND (:subjectId IS NULL OR subjectId=:subjectId) AND dueAt >= :from ORDER BY dueAt ASC LIMIT 1")
    EventEntity nextEvent(String appType, String subjectId, long from);

    // ====== NUEVOS, usados por Agenda & Expenses ======

    // Eventos en rango (pendientes o realizados, no borrados)
    @Query("SELECT * FROM events WHERE appType=:appType AND deleted=0 AND dueAt BETWEEN :start AND :end ORDER BY dueAt ASC")
    List<EventEntity> listInRange(String appType, long start, long end);

    // Realizados por realizedAt (por si difiere de dueAt)


    // Helpers de toggle
    @Query("UPDATE events SET realized=1, realizedAt=:now, updatedAt=:now, dirty=1 WHERE id IN (:ids)")
    void markRealized(List<String> ids, long now);

    @Query("UPDATE events SET realized=1, realizedAt=:now, updatedAt=:now, dirty=1 WHERE id=:id")
    void markRealizedOne(String id, long now);

    @Query("UPDATE events SET realized=0, realizedAt=NULL, updatedAt=:now, dirty=1 WHERE id=:id")
    void markUnrealizedOne(String id, long now);

    @Query("UPDATE events SET cost=:cost, updatedAt=:now, dirty=1 WHERE id=:id")
    void setCost(String id, Double cost, long now);

    @Query("SELECT * FROM events WHERE appType=:appType AND deleted=0 AND realized=0 AND dueAt <= :now ORDER BY dueAt ASC")
    List<EventEntity> listDueUnrealized(String appType, long now);

    // ====== Agendas por día ======

    // Observa eventos planificados (pendientes o realizados) en un rango de días por dueAt
    @Query("SELECT * FROM events WHERE appType=:appType AND deleted=0 AND dueAt BETWEEN :start AND :end ORDER BY dueAt ASC")
    LiveData<List<EventEntity>> observeByDay(String appType, long start, long end);

    // Suma de costos planificados (eventos no realizados) por dueAt
    @Query("SELECT SUM(cost) FROM events WHERE appType=:appType AND deleted=0 AND realized=0 AND dueAt BETWEEN :start AND :end")
    Double sumPlannedCostInRange(String appType, long start, long end);

    // Suma de costos realizados por realizedAt
    @Query("SELECT SUM(cost) FROM events WHERE appType=:appType AND deleted=0 AND realized=1 AND realizedAt BETWEEN :start AND :end")
    Double sumRealizedCostInRange(String appType, long start, long end);


    // Totales por mes (usa dueAt para agrupar, y realized para separar planificado vs realizado)
    @Query("""
        SELECT
          (strftime('%s', date(strftime('%Y-%m-01', datetime(dueAt/1000, 'unixepoch')))) * 1000) AS monthStart,
          SUM(CASE WHEN realized = 0 THEN COALESCE(cost, 0) ELSE 0 END) AS plannedSum,
          SUM(CASE WHEN realized = 1 THEN COALESCE(cost, 0) ELSE 0 END) AS realizedSum
        FROM events
        WHERE appType = :appType AND deleted = 0
        GROUP BY strftime('%Y-%m', datetime(dueAt/1000, 'unixepoch'))
        ORDER BY monthStart DESC
    """)
    List<com.gastonlesbegueris.caretemplate.data.model.MonthTotal> listMonthTotals(String appType);

    // Realizados en rango (para “Gastos” por período, si lo necesitás luego)
    @Query("""
        SELECT * FROM events
        WHERE appType = :appType
          AND deleted = 0
          AND realized = 1
          AND realizedAt BETWEEN :from AND :to
        ORDER BY realizedAt DESC
    """)
    List<com.gastonlesbegueris.caretemplate.data.local.EventEntity> listRealizedInRange(
            String appType, long from, long to
    );
}


