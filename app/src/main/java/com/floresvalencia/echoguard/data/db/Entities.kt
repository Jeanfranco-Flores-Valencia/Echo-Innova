package com.floresvalencia.echoguard.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

// ---------------------------------------------------------------------
// Personal y auditoría
// ---------------------------------------------------------------------

/** Cuenta de personal. El PIN se guarda solo como hash y es único entre cuentas. */
@Entity(tableName = "staff_account", indices = [Index(value = ["pinHash"], unique = true)])
data class StaffAccountEntity(
    @PrimaryKey val username: String,
    val name: String,
    /** Nombre del enum Role. */
    val role: String,
    val classroomId: String?,
    val pinHash: String,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "audit_log", indices = [Index("timeMillis")])
data class AuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timeMillis: Long,
    val actor: String,
    val role: String,
    val action: String,
    val detail: String,
)

// ---------------------------------------------------------------------
// Pruebas con usuarios (opiniones)
// ---------------------------------------------------------------------

@Entity(tableName = "feedback")
data class FeedbackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val userId: String,
    val userName: String,
    /** Nombre del enum Role. */
    val role: String,
    val classroomId: String?,
    /** Escalas de 1 a 5. */
    val ease: Int,
    val clarity: Int,
    val confidence: Int,
    val design: Int,
    /** ¿Recomendarías la app? 0 a 10. */
    val recommend: Int,
    val liked: String,
    val improvements: String,
    val bugs: String,
    val appVersion: String,
    val device: String,
    /** Ya enviado al servidor de opiniones (si hay uno configurado). */
    val synced: Boolean = false,
)

/** Resultado de cada tarea de la prueba (fácil / me costó / no pude / no la probé). */
@Entity(
    tableName = "feedback_task",
    foreignKeys = [ForeignKey(FeedbackEntity::class, ["id"], ["feedbackId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("feedbackId")],
)
data class TaskResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val feedbackId: Long = 0,
    val taskId: String,
    /** Nombre del enum TaskOutcome. */
    val outcome: String,
)

data class FeedbackWithTasks(
    @Embedded val feedback: FeedbackEntity,
    @Relation(parentColumn = "id", entityColumn = "feedbackId") val tasks: List<TaskResultEntity>,
)

// ---------------------------------------------------------------------
// Contactos de emergencia y SOS
// ---------------------------------------------------------------------

@Entity(tableName = "emergency_contact")
data class EmergencyContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    /** Mamá, papá, tutor, pareja... (opcional). */
    val relation: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "sos_log", indices = [Index("timeMillis")])
data class SosLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timeMillis: Long,
    /** Nombre del enum SosKind. */
    val kind: String,
    val recipients: String,
    /** Nombre del enum SosStatus. */
    val status: String,
    val message: String,
)
