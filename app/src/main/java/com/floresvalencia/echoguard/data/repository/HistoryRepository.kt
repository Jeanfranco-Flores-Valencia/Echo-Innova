package com.floresvalencia.echoguard.data.repository

import com.floresvalencia.echoguard.data.db.EchoDatabase
import com.floresvalencia.echoguard.data.db.EmergencyLogEntity
import com.floresvalencia.echoguard.data.db.ResponderPhoneEntity
import com.floresvalencia.echoguard.data.model.EvacuationSummary
import com.floresvalencia.echoguard.data.model.Permission
import com.floresvalencia.echoguard.data.model.Role
import com.floresvalencia.echoguard.data.sos.SosMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Registro de emergencias (cada evacuación o simulacro terminado) y teléfonos de los
 * responsables a quienes se avisa por SMS cuando no hay internet.
 */
class HistoryRepository(private val db: EchoDatabase, private val session: EchoRepository = EchoRepository) : EmergencyLog {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val emergencies: Flow<List<EmergencyLogEntity>> = db.emergencyLogDao().observeRecent()
    val responderPhones: Flow<List<ResponderPhoneEntity>> = db.responderPhoneDao().observeAll()

    override fun record(summary: EvacuationSummary) {
        scope.launch {
            db.emergencyLogDao().insert(
                EmergencyLogEntity(
                    startedAt = summary.startedAtMillis,
                    endedAt = summary.startedAtMillis + summary.durationMs,
                    mode = summary.mode.name,
                    cause = summary.cause?.name,
                    startedBy = summary.startedBy,
                    endedBy = summary.endedBy,
                    atAlarm = summary.atAlarm,
                    evacuated = summary.evacuated,
                    stillInside = summary.stillInside,
                    markedSafe = summary.markedSafe,
                    confirmed = summary.confirmed,
                    responders = summary.responders,
                )
            )
        }
    }

    suspend fun responderPhonesNow(): List<ResponderPhoneEntity> = withContext(Dispatchers.IO) { db.responderPhoneDao().all() }

    /** Solo dirección (quien administra al personal). Devuelve el motivo del error, o null si se guardó. */
    suspend fun addResponderPhone(name: String, role: Role, phone: String): String? = withContext(Dispatchers.IO) {
        if (!session.can(Permission.MANAGE_STAFF)) return@withContext EchoRepository.NO_PERMISSION
        val n = name.trim()
        if (n.length < 2) return@withContext "Escribe el nombre"
        val p = SosMessage.normalizePhone(phone) ?: return@withContext "Número no válido (6 a 15 dígitos)"
        val current = db.responderPhoneDao().all()
        if (current.any { it.phone == p }) return@withContext "Ese número ya está en la lista"
        if (current.size >= 30) return@withContext "Máximo 30 responsables"
        db.responderPhoneDao().insert(ResponderPhoneEntity(name = n.take(40), role = role.name, phone = p))
        session.recordAudit("Teléfono de responsable agregado", "$n · ${role.label}")
        null
    }

    suspend fun deleteResponderPhone(id: Long) = withContext(Dispatchers.IO) {
        if (!session.can(Permission.MANAGE_STAFF)) return@withContext
        db.responderPhoneDao().delete(id)
        session.recordAudit("Teléfono de responsable eliminado")
    }
}
