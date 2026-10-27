package com.floresvalencia.echoguard.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos local del teléfono. Funciona sin conexión.
 * Personal y auditoría · opiniones de las pruebas · contactos de emergencia y SOS.
 */
@Database(
    entities = [
        StaffAccountEntity::class,
        AuditEntity::class,
        FeedbackEntity::class,
        TaskResultEntity::class,
        EmergencyContactEntity::class,
        SosLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class EchoDatabase : RoomDatabase() {
    abstract fun staffDao(): StaffDao
    abstract fun auditDao(): AuditDao
    abstract fun feedbackDao(): FeedbackDao
    abstract fun contactDao(): ContactDao
    abstract fun sosDao(): SosDao

    companion object {
        @Volatile private var instance: EchoDatabase? = null

        fun get(context: Context): EchoDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, EchoDatabase::class.java, "echo_guard.db")
                .build()
                .also { instance = it }
        }
    }
}
