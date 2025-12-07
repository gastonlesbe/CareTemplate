package com.gastonlesbegueris.caretemplate.data.model;

/**
 * Modelo mínimo para un "Evento" guardado en Firestore.
 * Firestore puede mapearlo automáticamente porque:
 *  - Tiene un constructor vacío (requerido)
 *  - Los campos son públicos (o podrías usar getters/setters)
 */
public class Event {

    // ID del documento en Firestore (no se guarda dentro del doc; lo completamos al leer)
    public String id;

    // -------- Datos de negocio --------
    public String title;     // Título del evento (p.ej. "Vacuna", "Cambio de aceite")
    public long dueAt;       // Fecha/hora (epoch millis) del evento
    public String note;      // (opcional) detalle/nota

    // -------- Metadatos para control --------
    public long updatedAt;   // Última actualización (epoch millis) - útil para ordenar / conflictos
    public int deleted;      // 0 = activo, 1 = borrado lógico (soft delete)

    // Para distinguir qué app/flavor creó el documento: "pets" | "cars" | "family"
    public String appType;
    
    // Repetición de eventos (opcional)
    public String repeatType;      // null, "hourly", "daily", "monthly", "yearly"
    public Integer repeatInterval; // cada cuántas horas/días/meses/años (default: 1)
    public Long repeatEndDate;     // fecha de fin de repetición (opcional)
    public Integer repeatCount;    // número de repeticiones (opcional, alternativo a repeatEndDate)
    public String originalEventId;  // ID del evento original si es una repetición (null si es el original)

    // Constructor vacío requerido por Firestore
    public Event() {}

    // Constructor de conveniencia para crear nuevos eventos en código
    public Event(String title, long dueAt, String note, String appType) {
        this.title = title;
        this.dueAt = dueAt;
        this.note = note;
        this.appType = appType;

        this.updatedAt = System.currentTimeMillis();
        this.deleted = 0;
    }
}

