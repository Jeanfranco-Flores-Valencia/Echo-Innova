package com.floresvalencia.echoguard.data.repository

import com.floresvalencia.echoguard.data.db.AuditEntity
import com.floresvalencia.echoguard.data.db.EchoDatabase
import com.floresvalencia.echoguard.data.db.StaffAccountEntity
import com.floresvalencia.echoguard.data.model.AuditEntry
import com.floresvalencia.echoguard.data.model.Permission
import com.floresvalencia.echoguard.data.model.Role
import com.floresvalencia.echoguard.data.model.StaffAccount
import com.floresvalencia.echoguard.data.model.validatePin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Cuentas de personal y registro de auditoría guardados en la base de datos.
 * Solo quien tiene [Permission.MANAGE_STAFF] (dirección) puede cambiar cuentas.
 */
class StaffRepository(private val db: EchoDatabase, private val session: EchoRepository = EchoRepository) :
    StaffDirectory, AuditLog {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val staff get() = db.staffDao()

    /** La primera vez crea las cuentas demo (1111 brigadista · 2222 dirección · 3333 docente). */
    suspend fun seedIfEmpty() {
        if (staff.count() > 0) return
        DemoStaff.seeds.forEach { s ->
            staff.insert(
                StaffAccountEntity(
                    s.account.username, s.account.name, s.account.role.name, s.account.classroomId, Pins.hash(s.pin),
                )
            )
        }
    }

    override suspend fun findActiveByPinHash(pinHash: String): StaffAccount? =
        staff.findActiveByPinHash(pinHash)?.toModel()

    override fun record(entry: AuditEntry) {
        scope.launch {
            db.auditDao().insert(AuditEntity(0, entry.timeMillis, entry.actor, entry.role, entry.action, entry.detail))
            db.auditDao().trim()
        }
    }

    val accounts: Flow<List<StaffAccount>> = staff.observeAll().map { l -> l.map { it.toModel() } }

    val audit: Flow<List<AuditEntry>> = db.auditDao().observeRecent().map { l ->
        l.map { AuditEntry(it.id, it.timeMillis, it.actor, it.role, it.action, it.detail) }
    }

    private fun canManage() = session.can(Permission.MANAGE_STAFF)

    /** Crea una cuenta. Devuelve el motivo del error, o null si se creó. */
    suspend fun create(name: String, role: Role, classroomId: String?, pin: String): String? {
        if (!canManage()) return EchoRepository.NO_PERMISSION
        val cleanName = name.trim()
        if (cleanName.length < 2) return "Escribe el nombre"
        if (!role.isStaff) return "Elige un rol de personal"
        if (role == Role.DOCENTE && classroomId == null) return "Elige el aula del docente"
        validatePin(pin)?.let { return it }
        val hash = Pins.hash(pin)
        if (staff.countWithPin(hash) > 0) return "Ese PIN ya lo usa otra persona"
        val username = "staff-" + UUID.randomUUID().toString().take(8)
        staff.insert(StaffAccountEntity(username, cleanName, role.name, if (role == Role.DOCENTE) classroomId else null, hash))
        session.recordAudit("Cuenta creada", "$cleanName · ${role.label}")
        return null
    }

    /** Activa o desactiva una cuenta. No permite desactivarse a sí mismo ni dejar sin dirección. */
    suspend fun setActive(username: String, active: Boolean): String? {
        if (!canManage()) return EchoRepository.NO_PERMISSION
        val acc = staff.find(username) ?: return "Cuenta no encontrada"
        if (!active) {
            if (username == session.session.value?.username) return "No puedes desactivar tu propia cuenta"
            if (acc.role == Role.DIRECTOR.name && staff.countActive(Role.DIRECTOR.name) <= 1) {
                return "Debe quedar al menos una cuenta de dirección activa"
            }
        }
        staff.update(acc.copy(active = active))
        session.recordAudit(if (active) "Cuenta activada" else "Cuenta desactivada", acc.name)
        return null
    }

    suspend fun changePin(username: String, pin: String): String? {
        if (!canManage()) return EchoRepository.NO_PERMISSION
        val acc = staff.find(username) ?: return "Cuenta no encontrada"
        validatePin(pin)?.let { return it }
        val hash = Pins.hash(pin)
        if (staff.countWithPin(hash, exceptUsername = username) > 0) return "Ese PIN ya lo usa otra persona"
        staff.update(acc.copy(pinHash = hash))
        session.recordAudit("PIN cambiado", acc.name)
        return null
    }

    private fun StaffAccountEntity.toModel() =
        StaffAccount(username, name, Role.valueOf(role), classroomId, active)
}
