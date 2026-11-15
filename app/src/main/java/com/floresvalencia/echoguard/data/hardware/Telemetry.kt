package com.floresvalencia.echoguard.data.hardware

import com.floresvalencia.echoguard.data.model.MapPoint
import com.floresvalencia.echoguard.data.model.SensorReadings

/**
 * Datos que envía cada rover real (ESP32) a la app, en JSON plano. Ejemplo:
 *
 * {"rover":"R-01","battery":76,"smoke":12,"co":4,"temp":24,"dust":18,"flame":false,"x":20.5,"y":17}
 *
 * Y la estación de pared: {"station":"E1","seismic":0.02}
 * Todos los campos salvo el id son opcionales: lo que no llega no se cambia.
 */
sealed interface Telemetry {
    data class Rover(
        val id: String,
        val battery: Int?,
        val smokePpm: Int?,
        val coPpm: Int?,
        val temperatureC: Int?,
        val dustUg: Int?,
        val flame: Boolean?,
        val position: MapPoint?,
    ) : Telemetry {
        fun applyTo(s: SensorReadings) = s.copy(
            smokePpm = smokePpm ?: s.smokePpm,
            coPpm = coPpm ?: s.coPpm,
            temperatureC = temperatureC ?: s.temperatureC,
            dustUg = dustUg ?: s.dustUg,
            flame = flame ?: s.flame,
        )
    }

    data class Station(val id: String, val seismicG: Float?) : Telemetry

    companion object {
        private val pair = Regex("\"([A-Za-z_]+)\"\\s*:\\s*(\"[^\"]*\"|true|false|-?\\d+(?:\\.\\d+)?)")

        /** Devuelve null si el mensaje no es válido (así un dato corrupto nunca rompe la app). */
        fun parse(json: String): Telemetry? {
            val t = json.trim()
            if (!t.startsWith("{") || !t.endsWith("}")) return null
            val f = pair.findAll(t).associate { it.groupValues[1] to it.groupValues[2] }
            fun str(k: String) = f[k]?.takeIf { it.startsWith("\"") }?.trim('"')
            fun num(k: String) = f[k]?.toDoubleOrNull()
            fun bool(k: String) = when (f[k]) { "true" -> true; "false" -> false; else -> null }
            str("station")?.let { id -> return Station(id, num("seismic")?.toFloat()) }
            val id = str("rover") ?: return null
            val x = num("x"); val y = num("y")
            return Rover(
                id = id,
                battery = num("battery")?.toInt()?.coerceIn(0, 100),
                smokePpm = num("smoke")?.toInt(),
                coPpm = num("co")?.toInt(),
                temperatureC = num("temp")?.toInt(),
                dustUg = num("dust")?.toInt(),
                flame = bool("flame"),
                position = if (x != null && y != null) MapPoint(x.toFloat(), y.toFloat()) else null,
            )
        }
    }
}
