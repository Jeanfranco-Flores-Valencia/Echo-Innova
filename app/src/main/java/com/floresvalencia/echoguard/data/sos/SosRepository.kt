package com.floresvalencia.echoguard.data.sos

import android.content.Context
import com.floresvalencia.echoguard.data.db.EchoDatabase
import com.floresvalencia.echoguard.data.db.EmergencyContactEntity
import com.floresvalencia.echoguard.data.db.SosLogEntity
import com.floresvalencia.echoguard.data.model.EmergencyMode
import com.floresvalencia.echoguard.data.model.Permission
import com.floresvalencia.echoguard.data.model.recommended
import com.floresvalencia.echoguard.data.repository.EchoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Contactos de emergencia del teléfono y envío del SOS por SMS (sin internet). */
class SosRepository(
    private val context: Context,
    private val db: EchoDatabase,
    private val repo: EchoRepository = EchoRepository,
) {
    val contacts: Flow<List<EmergencyContactEntity>> = db.contactDao().observeAll()
    val history: Flow<List<SosLogEntity>> = db.sosDao().observeRecent()

    /** Máximo de contactos: el SOS debe salir rápido y sin costo excesivo de SMS. */
    val maxContacts = 5

    /** Devuelve el motivo del error, o null si se guardó. */
    suspend fun addContact(name: String, phone: String, relation: String): String? = withContext(Dispatchers.IO) {
        val n = name.trim()
        if (n.isEmpty()) return@withContext "Escribe el nombre del contacto"
        val p = SosMessage.normalizePhone(phone) ?: return@withContext "Número no válido (6 a 15 dígitos)"
        val current = db.contactDao().all()
        if (current.size >= maxContacts) return@withContext "Máximo $maxContacts contactos"
        if (current.any { it.phone == p }) return@withContext "Ese número ya está en tu lista"
        db.contactDao().insert(EmergencyContactEntity(name = n.take(40), phone = p, relation = relation.trim().take(30)))
        null
    }

    suspend fun deleteContact(id: Long) = withContext(Dispatchers.IO) { db.contactDao().delete(id) }

    suspend fun contactsNow(): List<EmergencyContactEntity> = withContext(Dispatchers.IO) { db.contactDao().all() }

    /** Arma el mensaje con los datos actuales de la persona, la emergencia, el GPS y la batería. */
    fun buildMessage(kind: SosKind, location: GeoFix?, battery: BatteryInfo?): String {
        val user = repo.session.value
        val e = repo.emergency.value
        val emergency = if (!e.active) null else buildString {
            append(if (e.mode == EmergencyMode.REAL) "Evacuacion REAL en curso" else "Simulacro en curso")
            e.cause?.let { append(" - ").append(it.label) }
        }
        val route = if (e.active) repo.routes.value.recommended()?.let { "${it.name} - ${it.path}" } else null
        return SosMessage.build(
            SosContext(
                kind = kind,
                name = user?.name ?: "Persona sin registrar",
                roleLabel = user?.role?.label ?: "Visitante",
                classroomId = user?.classroomId,
                place = EchoRepository.SITE_NAME,
                location = location,
                battery = battery,
                emergency = emergency,
                route = route,
                nowMillis = System.currentTimeMillis(),
            )
        )
    }

    /** Envía por SMS directo (requiere permiso). Guarda el resultado en el historial. */
    suspend fun sendDirect(kind: SosKind, text: String, recipients: List<EmergencyContactEntity>): SosStatus {
        if (repo.session.value != null && !repo.can(Permission.SEND_SOS)) return SosStatus.FAILED
        val results = SmsSender.send(context, recipients.map { it.phone }, text)
        val status = when {
            results.isNotEmpty() && results.all { it } -> SosStatus.SENT
            results.any { it } -> SosStatus.PARTIAL
            else -> SosStatus.FAILED
        }
        log(kind, text, recipients, status)
        return status
    }

    /** Se abrió la app de mensajes con el texto listo (sin permiso de SMS o por elección). */
    suspend fun logHandoff(kind: SosKind, text: String, recipients: List<EmergencyContactEntity>) =
        log(kind, text, recipients, SosStatus.HANDED_TO_SMS_APP)

    private suspend fun log(kind: SosKind, text: String, recipients: List<EmergencyContactEntity>, status: SosStatus) {
        withContext(Dispatchers.IO) {
            db.sosDao().insert(
                SosLogEntity(
                    timeMillis = System.currentTimeMillis(), kind = kind.name,
                    recipients = recipients.joinToString(", ") { it.name }, status = status.name, message = text,
                )
            )
        }
        repo.recordAudit("SOS: ${kind.label}", "${status.label} · ${recipients.size} contacto(s)")
    }
}
