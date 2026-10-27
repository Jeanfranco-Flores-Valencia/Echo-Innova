package com.floresvalencia.echoguard.data.sos

import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Qué quiere comunicar la persona a su contacto. */
enum class SosKind(val label: String, val headline: String) {
    SAFE("Estoy a salvo", "ESTOY A SALVO"),
    HELP("Necesito ayuda", "NECESITO AYUDA"),
    INJURED("Estoy herido(a)", "ESTOY HERIDO(A)"),
    TRAPPED("Estoy atrapado(a)", "ESTOY ATRAPADO(A)"),
}

enum class SosStatus(val label: String) {
    SENT("Enviado"),
    PARTIAL("Enviado a algunos"),
    FAILED("No se pudo enviar"),
    HANDED_TO_SMS_APP("Abierto en la app de mensajes"),
}

/** Ubicación del teléfono (GPS o red). Funciona sin internet si el GPS está encendido. */
data class GeoFix(val lat: Double, val lon: Double, val accuracyM: Int?, val timeMillis: Long)

data class BatteryInfo(val percent: Int, val charging: Boolean, val powerSave: Boolean)

/** Todo lo que va en el mensaje. */
data class SosContext(
    val kind: SosKind,
    val name: String,
    val roleLabel: String,
    val classroomId: String?,
    val place: String,
    val location: GeoFix?,
    val battery: BatteryInfo?,
    /** "Evacuación REAL en curso · Fuego", o null si no hay emergencia. */
    val emergency: String?,
    /** Ruta recomendada, si hay evacuación. */
    val route: String?,
    val nowMillis: Long,
)

object SosMessage {
    private val timeFormat = DateTimeFormatter.ofPattern("dd/MM HH:mm", Locale.US)

    /**
     * Mensaje corto y claro. Sin tildes ni símbolos especiales: así entra en menos SMS
     * (alfabeto GSM de 160 caracteres) y se lee bien en cualquier teléfono.
     */
    fun build(c: SosContext, zone: ZoneId = ZoneId.systemDefault()): String {
        val who = buildString {
            append(c.name)
            append(" (").append(c.roleLabel)
            c.classroomId?.let { append(", aula ").append(it) }
            append(")")
        }
        val location = c.location?.let { g ->
            val ago = ((c.nowMillis - g.timeMillis) / 60_000).coerceAtLeast(0)
            val details = listOfNotNull(g.accuracyM?.let { "+-${it} m" }, if (ago >= 1) "hace $ago min" else null)
            "Ubicacion: https://maps.google.com/?q=%.5f,%.5f".format(Locale.US, g.lat, g.lon) +
                if (details.isEmpty()) "" else " (${details.joinToString(", ")})"
        } ?: "Ubicacion GPS: no disponible"
        val battery = c.battery?.let { b ->
            "Bateria: ${b.percent}%" + when {
                b.charging -> " (cargando)"
                b.powerSave -> " (ahorro de energia)"
                b.percent <= 15 -> " (por agotarse)"
                else -> ""
            }
        } ?: "Bateria: desconocida"
        val lines = listOfNotNull(
            "SOS Echo Guard: ${c.kind.headline}",
            who,
            "Lugar: ${c.place}",
            location,
            battery,
            c.emergency,
            c.route?.let { "Ruta: $it" },
            timeFormat.format(Instant.ofEpochMilli(c.nowMillis).atZone(zone)),
        )
        return smsSafe(lines.joinToString("\n"))
    }

    /** Quita tildes y reemplaza símbolos que obligan a usar SMS más cortos (UCS-2). */
    fun smsSafe(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace('·', '-')
            .replace('°', 'o')
            .replace('–', '-')
            .replace('—', '-')

    /** Teléfono válido: 6 a 15 dígitos, opcionalmente con + al inicio. Devuelve el número limpio o null. */
    fun normalizePhone(raw: String): String? {
        val trimmed = raw.trim()
        val plus = trimmed.startsWith("+")
        val digits = trimmed.filter { it.isDigit() }
        if (digits.length !in 6..15) return null
        if (trimmed.any { !it.isDigit() && it !in " +-()." }) return null
        return (if (plus) "+" else "") + digits
    }
}
