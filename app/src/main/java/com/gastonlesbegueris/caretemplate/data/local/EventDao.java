package com.gastonlesbegueris.caretemplate.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.gastonlesbegueris.caretemplate.data.model.MonthTotal;

import java.util.List;

@Dao
public interface EventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EventEntity e);

    @Update
    void update(EventEntity e);

    @Query("UPDATE events SET deleted=1, dirty=1, updatedAt=:now WHERE id=:id")
    void softDelete(String id, long now);

    // Observa eventos activos por appType
    @Query("SELECT * FROM events WHERE appType=:appType AND deleted=0 ORDER BY dueAt ASC")
    LiveData<List<EventEntity>> observeActive(String appType);

    // Próximo evento por sujeto (para header)
    @Query("SELECT * FROM events " +
            "WHERE appType=:appType AND deleted=0 AND subjectId=:subjectId AND realized=0 AND dueAt>=:from " +
            "ORDER BY dueAt ASC LIMIT 1")
    EventEntity nextEvent(String appType, String subjectId, long from);

    // Listar vencidos no realizados (auto-realize)
    @Query("SELECT * FROM events WHERE appType=:appType AND deleted=0 AND realized=0 AND dueAt<:now")
    List<EventEntity> listDueUnrealized(String appType, long now);

    @Query("UPDATE events SET realized=1, realizedAt=:now, dirty=1, updatedAt=:now WHERE id IN (:ids)")
    void markRealized(List<String> ids, long now);

    @Query("UPDATE events SET realized=1, realizedAt=:now, dirty=1, updatedAt=:now WHERE id=:id")
    void markRealizedOne(String id, long now);

    @Query("UPDATE events SET realized=0, realizedAt=NULL, dirty=1, updatedAt=:now WHERE id=:id")
    void markUnrealizedOne(String id, long now);

    @Query("UPDATE events SET cost=:cost, dirty=1, updatedAt=:now WHERE id=:id")
    void setCost(String id, Double cost, long now);

    // ====== TOTALES MENSUALES (para Expenses/Resumen) ======
    // Agrupa por año/mes de dueAt. planned = realized=0; realized = realized=1
    @Query("""
           SELECT 
             CAST(strftime('%Y', datetime(dueAt/1000,'unixepoch')) AS INTEGER) AS year,
             CAST(strftime('%m', datetime(dueAt/1000,'unixepoch')) AS INTEGER) AS month,
             SUM(CASE WHEN realized=0 THEN COALESCE(cost,0) ELSE 0 END) AS plannedSum,
             SUM(CASE WHEN realized=1 THEN COALESCE(cost,0) ELSE 0 END) AS realizedSum
           FROM events
           WHERE appType=:appType AND deleted=0
           GROUP BY year, month
           ORDER BY year DESC, month DESC
           """)
    List<MonthTotal> listMonthlyTotals(String appType);

    // Totales en un rango (útiles si luego mostrás mes actual)
    @Query("""
           SELECT 
             SUM(CASE WHEN realized=0 THEN COALESCE(cost,0) ELSE 0 END)
           FROM events
           WHERE appType=:appType AND deleted=0 
             AND dueAt BETWEEN :from AND :to
           """)
    Double sumPlannedCostInRange(String appType, long from, long to);

    @Query("""
           SELECT 
             SUM(CASE WHEN realized=1 THEN COALESCE(cost,0) ELSE 0 END)
           FROM events
           WHERE appType=:appType AND deleted=0 
             AND dueAt BETWEEN :from AND :to
           """)
    Double sumRealizedCostInRange(String appType, long from, long to);

    // (Opcional) listar por día para Agenda
    @Query("""
           SELECT * FROM events
           WHERE appType=:appType AND deleted=0 
             AND dueAt BETWEEN :from AND :to
           ORDER BY dueAt ASC
           """)
    LiveData<List<EventEntity>> observeByDay(String appType, long from, long to);
}
