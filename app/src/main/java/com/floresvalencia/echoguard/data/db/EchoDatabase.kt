package com.floresvalencia.echoguard.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos local del teléfono. Funciona sin conexión.
 * Personal y auditoría · opiniones de las pruebas · contactos de emergencia y SOS ·
 * historial de emergencias y teléfonos de responsables (v2).
 */
@Database(
    entities = [
        StaffAccountEntity::class,
        AuditEntity::class,
        FeedbackEntity::class,
        TaskResultEntity::class,
        EmergencyContactEntity::class,
        SosLogEntity::class,
        EmergencyLogEntity::class,
        ResponderPhoneEntity::class,
    ],
    version = 2,
    exportSchema = true,
    // v2 solo agrega tablas: la migración es automática y conserva todos los datos.
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class EchoDatabase : RoomDatabase() {
    abstract fun staffDao(): StaffDao
    abstract fun auditDao(): AuditDao
    abstract fun feedbackDao(): FeedbackDao
    abstract fun contactDao(): ContactDao
    abstract fun sosDao(): SosDao
    abstract fun emergencyLogDao(): EmergencyLogDao
    abstract fun responderPhoneDao(): ResponderPhoneDao

    companion object {
        @Volatile private var instance: EchoDatabase? = null

        fun get(context: Context): EchoDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, EchoDatabase::class.java, "echo_guard.db")
                .build()
                .also { instance = it }
        }
    }
}
