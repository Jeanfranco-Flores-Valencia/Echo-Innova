package com.floresvalencia.echoguard.data.repository

import com.floresvalencia.echoguard.data.model.AuditEntry
import com.floresvalencia.echoguard.data.model.Role
import com.floresvalencia.echoguard.data.model.StaffAccount
import java.security.MessageDigest

/** Dónde se buscan las cuentas de personal. En la app: base de datos (Room). En los tests: memoria. */
interface StaffDirectory {
    /** Cuenta ACTIVA cuyo PIN coincide, o null. */
    suspend fun findActiveByPinHash(pinHash: String): StaffAccount?
}

/** Dónde se guardan las acciones sensibles (quién hizo qué y cuándo). */
fun interface AuditLog {
    fun record(entry: AuditEntry)
}

object Pins {
    private const val SALT = "echo-guard:"

    /** Solo se guarda el hash SHA-256 (con sal) del PIN, nunca el PIN. */
    fun hash(pin: String): String =
        MessageDigest.getInstance("SHA-256").digest((SALT + pin).toByteArray())
            .joinToString("") { "%02x".format(it) }
}

/** Cuentas de demostración con las que se crea la base de datos (y que usan los tests). */
object DemoStaff {
    data class Seed(val account: StaffAccount, val pin: String)

    val seeds = listOf(
        Seed(StaffAccount("gerardo", "Gerardo", Role.BRIGADISTA, null, active = true), "1111"),
        Seed(StaffAccount("director", "Patricia", Role.DIRECTOR, null, active = true), "2222"),
        Seed(StaffAccount("docente", "Luis Huamán", Role.DOCENTE, "3B", active = true), "3333"),
    )
}

/** Directorio en memoria con las cuentas demo. */
class InMemoryStaffDirectory(
    private val accounts: List<Pair<StaffAccount, String>> = DemoStaff.seeds.map { it.account to Pins.hash(it.pin) },
) : StaffDirectory {
    override suspend fun findActiveByPinHash(pinHash: String): StaffAccount? =
        accounts.firstOrNull { it.second == pinHash && it.first.active }?.first
}
