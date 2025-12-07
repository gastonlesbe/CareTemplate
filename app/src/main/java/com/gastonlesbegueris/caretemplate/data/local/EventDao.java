package com.gastonlesbegueris.caretemplate.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import static androidx.room.OnConflictStrategy.REPLACE;
import com.gastonlesbegueris.caretemplate.data.model.MonthTotal;

import java.util.List;

@Dao
public interface EventDao {

    @Insert(onConflict = REPLACE)
    void insert(EventEntity e);

    @Update
    void update(EventEntity e);

    @Query("UPDATE events SET deleted=1, updatedAt=:now, dirty=1 WHERE id=:id")
    void softDelete(String id, long now);

    @Query("SELECT * FROM events WHERE appType=:appType AND deleted=0 AND realized=0 ORDER BY dueAt ASC")
    LiveData<List<EventEntity>> observeActive(String appType);
    
    // Historial de eventos de un sujeto (todos, realizados y no realizados)
    @Query("SELECT * FROM events WHERE appType=:appType AND subjectId=:subjectId AND deleted=0 ORDER BY dueAt DESC")
    LiveData<List<EventEntity>> observeSubjectHistory(String appType, String subjectId);

    // Para header (siguiente evento)
    @Query("SELECT * FROM events WHERE appType=:appType AND deleted=0 AND (:subjectId IS NULL OR subjectId=:subjectId) AND dueAt >= :from ORDER BY dueAt ASC LIMIT 1")
    EventEntity nextEvent(String appType, String subjectId, long from);

    // ====== NUEVOS, usados por Agenda & Expenses ======
// EventDao.java  (dentro de la interface)
    @Query("SELECT COUNT(*) FROM events WHERE appType = :app AND deleted = 0")
    int countEventsForApp(String app);


    // Realizados por realizedAt (por si difiere de dueAt)


    // Helpers de toggle
    @Query("UPDATE events SET realized=1, realizedAt=:now, updatedAt=:now, dirty=1 WHERE id IN (:ids)")
    void markRealized(List<String> ids, long now);


    @Query("SELECT * FROM events WHERE appType=:appType AND deleted=0 AND realized=0 AND dueAt <= :now ORDER BY dueAt ASC")
    List<EventEntity> listDueUnrealized(String appType, long now);

    // Obtener todos los eventos pendientes (futuros) para reprogramar notificaciones
    @Query("SELECT * FROM events WHERE appType=:appType AND deleted=0 AND realized=0 AND dueAt >= :now ORDER BY dueAt ASC")
    List<EventEntity> listPendingEvents(String appType, long now);

    // ====== Agendas por día ======




    /**
     * Totales por mes (planificado vs realizado)
     */


    /**
     * Eventos realizados en rango [from,to] (para detalles si luego querés)
     */
    @Query(
            "SELECT * FROM events " +
                    "WHERE appType = :appType AND deleted = 0 AND realized = 1 " +
                    "AND dueAt BETWEEN :from AND :to " +
                    "ORDER BY dueAt DESC"
    )
    java.util.List<com.gastonlesbegueris.caretemplate.data.local.EventEntity> listRealizedInRange(
            String appType, long from, long to
    );

    // Listado con totales por mes (pendiente vs realizado)
    @Query("""
           SELECT strftime('%Y-%m-01', datetime(dueAt/1000,'unixepoch')) AS monthStart,
                  SUM(CASE WHEN realized=0 THEN COALESCE(cost,0) ELSE 0 END) AS plannedSum,
                  SUM(CASE WHEN realized=1 THEN COALESCE(cost,0) ELSE 0 END) AS realizedSum
           FROM events
           WHERE appType = :appType AND deleted = 0
           GROUP BY monthStart
           ORDER BY monthStart DESC
           """)
    List<com.gastonlesbegueris.caretemplate.data.model.MonthTotal> listMonthTotals(String appType);

    // Para ExpensesActivity (si querés listar eventos en rango)
    @Query("SELECT * FROM events WHERE appType=:appType AND deleted=0 AND dueAt BETWEEN :from AND :to ORDER BY dueAt DESC")
    List<EventEntity> listInRange(String appType, long from, long to);

    // Sumas en rango (si son usadas en Agenda)
    @Query("SELECT SUM(COALESCE(cost,0)) FROM events WHERE appType=:appType AND deleted=0 AND realized=0 AND dueAt BETWEEN :from AND :to")
    Double sumPlannedCostInRange(String appType, long from, long to);

    @Query("SELECT SUM(COALESCE(cost,0)) FROM events WHERE appType=:appType AND deleted=0 AND realized=1 AND realizedAt BETWEEN :from AND :to")
    Double sumRealizedCostInRange(String appType, long from, long to);

    // Observación por día (si la pantalla Agenda la pide). Agrupás por inicio de día.
    @Query("""
           SELECT * FROM events
           WHERE appType=:appType AND deleted=0
             AND dueAt BETWEEN :from AND :to
           ORDER BY dueAt ASC
           """)
    LiveData<List<EventEntity>> observeByDay(String appType, long from, long to);

    // Helpers de toggle
    @Query("UPDATE events SET realized=1, realizedAt=:ts, updatedAt=:ts, dirty=1 WHERE id=:id")
    void markRealizedOne(String id, long ts);

    @Query("UPDATE events SET realized=0, realizedAt=NULL, updatedAt=:ts, dirty=1 WHERE id=:id")
    void markUnrealizedOne(String id, long ts);

    @Query("UPDATE events SET cost=:cost, updatedAt=:ts, dirty=1 WHERE id=:id")
    void setCost(String id, Double cost, long ts);
    // =========== 🔴 MÉTODOS PARA SYNC (lo que te falta) ===========
    // 1) listar registros "sucios" (a subir)
    @Query("SELECT * FROM events WHERE dirty=1")
    List<EventEntity> listDirty();

    // 2) marcarlos como limpios luego del push OK
    @Query("UPDATE events SET dirty=0 WHERE id IN (:ids)")
    void markClean(List<String> ids);

    // 3) último updated local por app (para usar en pull)
    @Query("SELECT IFNULL(MAX(updatedAt), 0) FROM events WHERE appType = :app")
    long lastUpdatedForApp(String app);

    // 4) inserción masiva desde la nube (pull)
    @Insert(onConflict = REPLACE)
    void insertAll(List<EventEntity> list);

    // 5) obtener un evento por ID
    @Query("SELECT * FROM events WHERE id=:id")
    EventEntity findOne(String id);

    // 6) obtener el evento original de un evento repetido
    @Query("SELECT * FROM events WHERE id=:originalEventId AND deleted=0")
    EventEntity findOriginalEvent(String originalEventId);

    // 7) obtener todos los eventos repetidos de un evento original
    @Query("SELECT * FROM events WHERE originalEventId=:originalEventId AND deleted=0")
    List<EventEntity> findRepeatedEvents(String originalEventId);

    // 8) eliminar (soft delete) todos los eventos repetidos de un original
    @Query("UPDATE events SET deleted=1, updatedAt=:now, dirty=1 WHERE originalEventId=:originalEventId AND deleted=0")
    void softDeleteRepeatedEvents(String originalEventId, long now);

}




