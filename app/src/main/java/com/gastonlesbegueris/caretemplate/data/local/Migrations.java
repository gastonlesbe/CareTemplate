package com.gastonlesbegueris.caretemplate.data.local;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Migraciones de la base de datos Room
 */
public class Migrations {
    
    /**
     * Migración de versión 11 a 12
     * Puede que no haya cambios específicos, pero necesitamos esta migración para evitar
     * que Room active fallbackToDestructiveMigration() cuando un usuario actualiza desde v11
     */
    public static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // No hay cambios en esta versión, pero la migración es necesaria para preservar datos
            android.util.Log.d("Migrations", "Migración 11→12: Sin cambios de esquema");
        }
    };
    
    /**
     * Migración de versión 12 a 13
     * Puede que no haya cambios específicos, pero necesitamos esta migración para evitar
     * que Room active fallbackToDestructiveMigration() cuando un usuario actualiza desde v12
     */
    public static final Migration MIGRATION_12_13 = new Migration(12, 13) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // No hay cambios en esta versión, pero la migración es necesaria para preservar datos
            android.util.Log.d("Migrations", "Migración 12→13: Sin cambios de esquema");
        }
    };
    
    /**
     * Migración de versión 13 a 14
     * Agrega campos de repetición y notificaciones a la tabla events
     * También asegura que kilometersAtEvent exista (por si se agregó sin migración en versiones anteriores)
     */
    public static final Migration MIGRATION_13_14 = new Migration(13, 14) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Agregar kilometersAtEvent si no existe (puede que se haya agregado sin migración en versiones anteriores)
            try {
                database.execSQL("ALTER TABLE events ADD COLUMN kilometersAtEvent REAL");
            } catch (android.database.SQLException e) {
                // La columna ya existe, ignorar el error
                android.util.Log.d("Migrations", "kilometersAtEvent ya existe, continuando...");
            }
            
            // Agregar columnas de repetición (todas nullable, por defecto NULL)
            database.execSQL("ALTER TABLE events ADD COLUMN repeatType TEXT");
            database.execSQL("ALTER TABLE events ADD COLUMN repeatInterval INTEGER");
            database.execSQL("ALTER TABLE events ADD COLUMN repeatEndDate INTEGER");
            database.execSQL("ALTER TABLE events ADD COLUMN repeatCount INTEGER");
            database.execSQL("ALTER TABLE events ADD COLUMN originalEventId TEXT");
            
            // Agregar columna de notificación (nullable, por defecto NULL)
            database.execSQL("ALTER TABLE events ADD COLUMN notificationMinutesBefore INTEGER");
        }
    };
    
    /**
     * Migración directa de versión 11 a 14
     * Para usuarios que saltan múltiples versiones, aplicamos todos los cambios de una vez
     */
    public static final Migration MIGRATION_11_14 = new Migration(11, 14) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            android.util.Log.d("Migrations", "Migración directa 11→14: Aplicando todos los cambios");
            
            // Aplicar los mismos cambios que MIGRATION_13_14
            try {
                database.execSQL("ALTER TABLE events ADD COLUMN kilometersAtEvent REAL");
            } catch (android.database.SQLException e) {
                android.util.Log.d("Migrations", "kilometersAtEvent ya existe, continuando...");
            }
            
            // Agregar columnas de repetición (todas nullable, por defecto NULL)
            try {
                database.execSQL("ALTER TABLE events ADD COLUMN repeatType TEXT");
            } catch (android.database.SQLException e) {
                android.util.Log.d("Migrations", "repeatType ya existe, continuando...");
            }
            
            try {
                database.execSQL("ALTER TABLE events ADD COLUMN repeatInterval INTEGER");
            } catch (android.database.SQLException e) {
                android.util.Log.d("Migrations", "repeatInterval ya existe, continuando...");
            }
            
            try {
                database.execSQL("ALTER TABLE events ADD COLUMN repeatEndDate INTEGER");
            } catch (android.database.SQLException e) {
                android.util.Log.d("Migrations", "repeatEndDate ya existe, continuando...");
            }
            
            try {
                database.execSQL("ALTER TABLE events ADD COLUMN repeatCount INTEGER");
            } catch (android.database.SQLException e) {
                android.util.Log.d("Migrations", "repeatCount ya existe, continuando...");
            }
            
            try {
                database.execSQL("ALTER TABLE events ADD COLUMN originalEventId TEXT");
            } catch (android.database.SQLException e) {
                android.util.Log.d("Migrations", "originalEventId ya existe, continuando...");
            }
            
            // Agregar columna de notificación (nullable, por defecto NULL)
            try {
                database.execSQL("ALTER TABLE events ADD COLUMN notificationMinutesBefore INTEGER");
            } catch (android.database.SQLException e) {
                android.util.Log.d("Migrations", "notificationMinutesBefore ya existe, continuando...");
            }
        }
    };
    
    /**
     * Migración directa de versión 12 a 14
     * Para usuarios que saltan de v12 a v14, aplicamos todos los cambios de una vez
     */
    public static final Migration MIGRATION_12_14 = new Migration(12, 14) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            android.util.Log.d("Migrations", "Migración directa 12→14: Aplicando todos los cambios");
            
            // Aplicar los mismos cambios que MIGRATION_13_14
            try {
                database.execSQL("ALTER TABLE events ADD COLUMN kilometersAtEvent REAL");
            } catch (android.database.SQLException e) {
                android.util.Log.d("Migrations", "kilometersAtEvent ya existe, continuando...");
            }
            
            // Agregar columnas de repetición (todas nullable, por defecto NULL)
            try {
                database.execSQL("ALTER TABLE events ADD COLUMN repeatType TEXT");
            } catch (android.database.SQLException e) {
                android.util.Log.d("Migrations", "repeatType ya existe, continuando...");
            }
            
            try {
                database.execSQL("ALTER TABLE events ADD COLUMN repeatInterval INTEGER");
            } catch (android.database.SQLException e) {
                android.util.Log.d("Migrations", "repeatInterval ya existe, continuando...");
            }
            
            try {
                database.execSQL("ALTER TABLE events ADD COLUMN repeatEndDate INTEGER");
            } catch (android.database.SQLException e) {
                android.util.Log.d("Migrations", "repeatEndDate ya existe, continuando...");
            }
            
            try {
                database.execSQL("ALTER TABLE events ADD COLUMN repeatCount INTEGER");
            } catch (android.database.SQLException e) {
                android.util.Log.d("Migrations", "repeatCount ya existe, continuando...");
            }
            
            try {
                database.execSQL("ALTER TABLE events ADD COLUMN originalEventId TEXT");
            } catch (android.database.SQLException e) {
                android.util.Log.d("Migrations", "originalEventId ya existe, continuando...");
            }
            
            // Agregar columna de notificación (nullable, por defecto NULL)
            try {
                database.execSQL("ALTER TABLE events ADD COLUMN notificationMinutesBefore INTEGER");
            } catch (android.database.SQLException e) {
                android.util.Log.d("Migrations", "notificationMinutesBefore ya existe, continuando...");
            }
        }
    };
    
    /**
     * Migración de versión 14 a 15
     * Agrega campo uid a la tabla subjects para asociar cada sujeto con su usuario propietario
     */
    public static final Migration MIGRATION_14_15 = new Migration(14, 15) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            android.util.Log.d("Migrations", "Migración 14→15: Agregando campo uid a subjects");
            try {
                database.execSQL("ALTER TABLE subjects ADD COLUMN uid TEXT");
                android.util.Log.d("Migrations", "Campo uid agregado a subjects");
                // Nota: Los sujetos existentes tendrán uid = null, se asignará en la próxima sincronización
            } catch (android.database.SQLException e) {
                android.util.Log.e("Migrations", "Error agregando campo uid: " + e.getMessage());
            }
        }
    };
    
    /**
     * Migración directa de versión 11 a 15
     */
    public static final Migration MIGRATION_11_15 = new Migration(11, 15) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            android.util.Log.d("Migrations", "Migración directa 11→15");
            // Aplicar cambios de 11→14
            MIGRATION_11_14.migrate(database);
            // Aplicar cambios de 14→15
            MIGRATION_14_15.migrate(database);
        }
    };
    
    /**
     * Migración directa de versión 12 a 15
     */
    public static final Migration MIGRATION_12_15 = new Migration(12, 15) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            android.util.Log.d("Migrations", "Migración directa 12→15");
            // Aplicar cambios de 12→14
            MIGRATION_12_14.migrate(database);
            // Aplicar cambios de 14→15
            MIGRATION_14_15.migrate(database);
        }
    };
    
    /**
     * Migración directa de versión 13 a 15
     */
    public static final Migration MIGRATION_13_15 = new Migration(13, 15) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            android.util.Log.d("Migrations", "Migración directa 13→15");
            // Aplicar cambios de 13→14
            MIGRATION_13_14.migrate(database);
            // Aplicar cambios de 14→15
            MIGRATION_14_15.migrate(database);
        }
    };
}

