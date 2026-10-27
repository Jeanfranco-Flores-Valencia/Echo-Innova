package com.floresvalencia.baymaxecho.data.model

import kotlin.math.hypot

// =====================================================================
// Roles y permisos
// =====================================================================

/**
 * Cada rol tiene permisos explícitos. Un alumno o docente NO puede iniciar una
 * evacuación general: solo puede reportar, y la brigada decide.
 */
enum class Role(val label: String) {
    BRIGADISTA("Brigadista"),
    DIRECTOR("Director(a)"),
    DOCENTE("Docente"),
    ALUMNO("Alumno"),
    /** Persona regular (visitante, personal externo, u otro ámbito fuera del colegio). */
    GENERAL("Usuario");

    /** Roles de personal: requieren PIN y no se guardan al cerrar la app. */
    val isStaff: Boolean get() = this == BRIGADISTA || this == DIRECTOR || this == DOCENTE

    /** Iniciar / finalizar la evacuación general. */
    val canStartEvacuation: Boolean get() = this == BRIGADISTA || this == DIRECTOR
    /** Desplegar rovers, ruta luminosa, orientación. */
    val canControlRovers: Boolean get() = this == BRIGADISTA
    /** Ver flota, mapa, última ubicación y rango operativo. */
    val canSeeFleet: Boolean get() = this == BRIGADISTA || this == DIRECTOR
    val canSeeCameras: Boolean get() = this == BRIGADISTA || this == DIRECTOR
    /** Reportar estado de salidas (libre / congestionada / bloqueada). */
    val canReportRoutes: Boolean get() = this == BRIGADISTA
    /** Ver el tablero de todas las aulas y personas que requieren asistencia. */
    val canSeeAllClassrooms: Boolean get() = this == BRIGADISTA || this == DIRECTOR
    /** Pasar lista de su propia aula. */
    val canReportOwnClassroom: Boolean get() = this == DOCENTE
}

data class User(
    val username: String,
    val name: String,
    val role: Role,
    val pin: String = "",
    val classroomId: String? = null,
)

// =====================================================================
// Edificio: plano, estaciones, salidas
// =====================================================================

/** Punto en el plano del piso, en metros. */
data class MapPoint(val x: Float, val y: Float)

fun MapPoint.distanceTo(o: MapPoint): Float = hypot(x - o.x, y - o.y)

data class Station(
    val id: String,
    val name: String,
    val position: MapPoint,
    /** Radio de enlace confiable con los rovers (m). */
    val rangeM: Int,
)

enum class RouteStatus(val label: String) { FREE("Libre"), CONGESTED("Congestionada"), BLOCKED("Bloqueada") }

enum class ArrowDir { LEFT, RIGHT, UP, DOWN }

data class ExitRoute(
    val id: String,
    val name: String,
    val path: String,
    val distanceM: Int,
    val status: RouteStatus,
    val position: MapPoint,
    val arrow: ArrowDir,
)

/** Ruta libre más corta; si no hay libres, la congestionada más corta. Nunca una bloqueada. */
fun List<ExitRoute>.recommended(): ExitRoute? =
    filter { it.status == RouteStatus.FREE }.minByOrNull { it.distanceM }
        ?: filter { it.status == RouteStatus.CONGESTED }.minByOrNull { it.distanceM }

// =====================================================================
// Flota de rovers y sensores
// =====================================================================

enum class RoverMode(val label: String) {
    DOCKED("En estación"),
    PATROLLING("Desplegado"),
    CHARGING("Cargando"),
    LOW_BATTERY("Batería baja"),
    OFFLINE("Sin señal"),
}

data class SensorReadings(
    val smokePpm: Int = 12,
    val coPpm: Int = 4,
    val temperatureC: Int = 24,
    val seismicG: Float = 0.01f,
    val dustUg: Int = 18,
    val flame: Boolean = false,
) {
    // Umbrales de referencia para la demo (calibrar con los sensores reales).
    val smokeAlarm get() = smokePpm >= 300
    val coAlarm get() = coPpm >= 50
    val heatAlarm get() = temperatureC >= 57
    val seismicAlarm get() = seismicG >= 0.15f
    val dustAlarm get() = dustUg >= 150
}

data class Rover(
    val id: String,
    val name: String,
    val homeStationId: String,
    val mode: RoverMode,
    val battery: Int,
    /** Posición actual, o la ÚLTIMA registrada si está sin señal. */
    val position: MapPoint,
    val locationLabel: String,
    val lastSeenMillis: Long,
    val sensors: SensorReadings = SensorReadings(),
    val lightRoute: Boolean = false,
    val facingBack: Boolean = false,
    val latencyMs: Int = 42,
    val fps: Int = 24,
) {
    val online: Boolean get() = mode != RoverMode.OFFLINE
    /** Disponible para una misión ahora mismo. */
    val available: Boolean get() = online && battery > 20 && mode != RoverMode.CHARGING
    /** Autonomía estimada en minutos (≈ 90 min con batería llena). */
    val autonomyMin: Int get() = battery * 90 / 100
}

// =====================================================================
// Detección, alertas y emergencia
// =====================================================================

enum class HazardType(val label: String, val instruction: String) {
    EARTHQUAKE("Sismo", "Aléjate de ventanas. Al terminar el movimiento, evacúa."),
    SMOKE("Humo", "Agáchate y avanza cerca del suelo hacia la salida."),
    GAS("Gas / CO", "No uses interruptores ni fuego. Evacúa."),
    FIRE("Fuego", "Evacúa por la ruta indicada. No uses ascensores."),
    INJURED("Persona herida", "Mantén la calma. La brigada va en camino."),
    OTHER("Otro", "Mantén la calma y espera indicaciones."),
}

enum class AlertKind { SYSTEM, HAZARD, REQUEST, EVACUATION }

data class Alert(
    val id: Int,
    val kind: AlertKind,
    val title: String,
    val source: String,
    val time: String,
    val critical: Boolean,
    val active: Boolean,
    val detail: String,
    val hazard: HazardType? = null,
    val roverId: String? = null,
    val createdBy: String? = null,
)

enum class EmergencyMode { NONE, DRILL, REAL }

data class EmergencyState(
    val mode: EmergencyMode = EmergencyMode.NONE,
    val startedAtMillis: Long = 0L,
    val startedBy: String = "",
    val cause: HazardType? = null,
) {
    val active: Boolean get() = mode != EmergencyMode.NONE
}

// =====================================================================
// Aforo y aulas
// =====================================================================

enum class OccupancyLevel { NORMAL, HIGH, FULL }

data class Occupancy(
    val inside: Int = 120,
    val capacity: Int = 150,
    val evacuated: Int = 84,
    val pending: Int = 36,
    val entries5m: Int = 3,
    val exits5m: Int = 18,
) {
    val ratio: Float get() = (inside.toFloat() / capacity).coerceIn(0f, 1f)
    val percent: Int get() = (ratio * 100).toInt()
    val level: OccupancyLevel
        get() = when {
            inside >= capacity -> OccupancyLevel.FULL
            inside >= capacity - 10 -> OccupancyLevel.HIGH
            else -> OccupancyLevel.NORMAL
        }
}

enum class RollStatus(val label: String) { PENDING("Sin reportar"), COMPLETE("Completa"), MISSING("Faltan") }

data class AssistPerson(
    val id: Int,
    val name: String,
    val need: String,
    val located: Boolean = false,
)

data class Classroom(
    val id: String,
    val floor: Int,
    val teacher: String,
    val students: Int,
    val status: RollStatus = RollStatus.PENDING,
    val missing: Int = 0,
    val assist: List<AssistPerson> = emptyList(),
)

// =====================================================================
// Cámaras y utilidades
// =====================================================================

enum class CameraDirection(val label: String, val degrees: Int, val focusX: Float) {
    LEFT("Izquierda", -45, 0.30f),
    FRONT("Frontal", 0, 0.50f),
    RIGHT("Derecha", 45, 0.70f),
}

fun formatElapsed(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(total / 60, total % 60)
}

fun formatAgo(ms: Long): String {
    val min = (ms / 60_000).coerceAtLeast(0)
    return when {
        min < 1 -> "hace instantes"
        min < 60 -> "hace $min min"
        else -> "hace ${min / 60} h ${min % 60} min"
    }
}

/**
 * Quién debe ver cada alerta. Detecciones y reportes solo los ve quien decide
 * (evita pánico por falsas alarmas); la evacuación la ven todos.
 */
fun Alert.isVisibleTo(user: User): Boolean = when (kind) {
    AlertKind.HAZARD, AlertKind.REQUEST -> user.role.canStartEvacuation || createdBy == user.username
    AlertKind.SYSTEM -> user.role.canSeeFleet
    AlertKind.EVACUATION -> true
}
