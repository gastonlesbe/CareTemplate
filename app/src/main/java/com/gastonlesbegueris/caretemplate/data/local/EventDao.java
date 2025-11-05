package com.gastonlesbegueris.caretemplate.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

@Dao
public interface EventDao {
    // ... (tus métodos existentes)

    // Eventos (pendientes o realizados) en un rango de fechas
    @Query("""
           SELECT * FROM events
           WHERE appType = :app
             AND deleted = 0
             AND dueAt BETWEEN :start AND :end
           ORDER BY dueAt ASC
           """)
    List<EventEntity> listInRange(String app, long start, long end);

    // Solo realizados en rango (útil para gastos)
    @Query("""
           SELECT * FROM events
           WHERE appType = :app
             AND realized = 1
             AND deleted = 0
             AND dueAt BETWEEN :start AND :end
           ORDER BY dueAt ASC
           """)
    List<EventEntity> listRealizedInRange(String app, long start, long end);

    // Suma de gastos realizados en rango
    @Query("""
           SELECT SUM(cost) FROM events
           WHERE appType = :app
             AND realized = 1
             AND deleted = 0
             AND cost IS NOT NULL
             AND dueAt BETWEEN :start AND :end
           """)
    Double sumExpensesInRange(String app, long start, long end);

    // (Opcional) versiones LiveData si querés reaccionar a cambios
    @Query("""
           SELECT * FROM events
           WHERE appType = :app
             AND deleted = 0
             AND dueAt BETWEEN :start AND :end
           ORDER BY dueAt ASC
           """)
    LiveData<List<EventEntity>> observeInRange(String app, long start, long end);

    @Query("""
           SELECT * FROM events
           WHERE appType = :app
             AND realized = 1
             AND deleted = 0
             AND dueAt BETWEEN :start AND :end
           ORDER BY dueAt ASC
           """)
    LiveData<List<EventEntity>> observeRealizedInRange(String app, long start, long end);
}
