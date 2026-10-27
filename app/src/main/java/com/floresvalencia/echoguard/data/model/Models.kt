package com.floresvalencia.echoguard.data.model

import kotlin.math.hypot

// =====================================================================
// Roles y permisos
// =====================================================================

/**
 * Acciones que la app protege. Cada pantalla y cada función del repositorio consulta
 * esta lista: es la única fuente de verdad de "quién puede hacer qué".
 */
enum class Permission(val label: String, val group: PermissionGroup) {
    // Emergencia
    START_EVACUATION("Iniciar y finalizar la evacuación general", PermissionGroup.EMERGENCY),
    REQUEST_EVACUATION("Solicitar a la brigada que evalúe una evacuación", PermissionGroup.EMERGENCY),
    REPORT_EMERGENCY("Reportar una emergencia o pedir ayuda", PermissionGroup.EMERGENCY),
    SEND_SOS("Enviar SOS a sus contactos de emergencia", PermissionGroup.EMERGENCY),
    VIEW_ROUTE("Ver la ruta de evacuación", PermissionGroup.EMERGENCY),

    // Alertas
    VIEW_HAZARD_ALERTS("Ver detecciones de sensores y reportes", PermissionGroup.ALERTS),
    VIEW_SYSTEM_ALERTS("Ver avisos técnicos de la flota", PermissionGroup.ALERTS),
    RESOLVE_ALERTS("Resolver o descartar detecciones y reportes", PermissionGroup.ALERTS),
    RESOLVE_SYSTEM_ALERTS("Resolver avisos técnicos de la flota", PermissionGroup.ALERTS),

    // Rovers
    VIEW_FLEET("Ver flota, mapa, rango y última ubicación", PermissionGroup.ROVERS),
    VIEW_CAMERAS("Ver cámaras en vivo", PermissionGroup.ROVERS),
    CONTROL_ROVERS("Desplegar rovers, ruta luminosa y cámara", PermissionGroup.ROVERS),

    // Personas y salidas
    REPORT_ROUTES("Reportar estado de las salidas", PermissionGroup.PEOPLE),
    VIEW_ALL_CLASSROOMS("Ver tablero de aulas y personas con asistencia", PermissionGroup.PEOPLE),
    TAKE_ROLL("Pasar lista de su aula", PermissionGroup.PEOPLE),

    // Administración
    MANAGE_STAFF("Crear, desactivar y cambiar el PIN del personal", PermissionGroup.ADMIN),
    VIEW_AUDIT_LOG("Ver el registro de acciones", PermissionGroup.ADMIN),
    VIEW_TEST_RESULTS("Ver y exportar los resultados de las pruebas", PermissionGroup.ADMIN),
    RESET_DEMO("Reiniciar la demo para el siguiente participante", PermissionGroup.ADMIN),
}

enum class PermissionGroup(val label: String) {
    EMERGENCY("Emergencia"),
    ALERTS("Alertas"),
    ROVERS("Rovers"),
    PEOPLE("Personas y salidas"),
    ADMIN("Administración"),
}

private val EVERYONE = setOf(Permission.REPORT_EMERGENCY, Permission.SEND_SOS, Permission.VIEW_ROUTE)

/**
 * Cada rol tiene permisos explícitos. Un alumno o docente NO puede iniciar una
 * evacuación general: solo puede reportar o solicitarla, y la brigada decide.
 */
enum class Role(
    val label: String,
    val description: String,
    val permissions: Set<Permission>,
    /** Roles de personal: requieren PIN, no se guardan al cerrar la app y se cierran por inactividad. */
    val isStaff: Boolean,
) {
    BRIGADISTA(
        "Brigadista", "Coordina la evacuación en el lugar y opera los rovers.",
        EVERYONE + setOf(
            Permission.START_EVACUATION, Permission.VIEW_HAZARD_ALERTS, Permission.VIEW_SYSTEM_ALERTS,
            Permission.RESOLVE_ALERTS, Permission.RESOLVE_SYSTEM_ALERTS, Permission.VIEW_FLEET,
            Permission.VIEW_CAMERAS, Permission.CONTROL_ROVERS, Permission.REPORT_ROUTES,
            Permission.VIEW_ALL_CLASSROOMS, Permission.VIEW_TEST_RESULTS, Permission.RESET_DEMO,
        ),
        isStaff = true,
    ),
    DIRECTOR(
        "Director(a)", "Decide la evacuación, supervisa todo y administra al personal.",
        EVERYONE + setOf(
            Permission.START_EVACUATION, Permission.VIEW_HAZARD_ALERTS, Permission.VIEW_SYSTEM_ALERTS,
            Permission.RESOLVE_ALERTS, Permission.RESOLVE_SYSTEM_ALERTS, Permission.VIEW_FLEET,
            Permission.VIEW_CAMERAS, Permission.VIEW_ALL_CLASSROOMS, Permission.MANAGE_STAFF,
            Permission.VIEW_AUDIT_LOG, Permission.VIEW_TEST_RESULTS, Permission.RESET_DEMO,
        ),
        isStaff = true,
    ),
    DOCENTE(
        "Docente", "Guía a su aula, pasa lista y puede solicitar una evacuación.",
        EVERYONE + setOf(Permission.REQUEST_EVACUATION, Permission.TAKE_ROLL),
        isStaff = true,
    ),
    ALUMNO("Alumno", "Sigue la ruta y a su docente. Puede reportar y pedir ayuda.", EVERYONE, isStaff = false),
    /** Persona regular (visitante, personal externo, u otro ámbito fuera del colegio). */
    GENERAL("Usuario", "Visitante o persona externa. Puede reportar y pedir ayuda.", EVERYONE, isStaff = false);

    fun has(p: Permission): Boolean = p in permissions

    // Atajos legibles para las pantallas.
    val canStartEvacuation: Boolean get() = has(Permission.START_EVACUATION)
    val canControlRovers: Boolean get() = has(Permission.CONTROL_ROVERS)
    val canSeeFleet: Boolean get() = has(Permission.VIEW_FLEET)
    val canSeeCameras: Boolean get() = has(Permission.VIEW_CAMERAS)
    val canReportRoutes: Boolean get() = has(Permission.REPORT_ROUTES)
    val canSeeAllClassrooms: Boolean get() = has(Permission.VIEW_ALL_CLASSROOMS)
    val canReportOwnClassroom: Boolean get() = has(Permission.TAKE_ROLL)

    companion object {
        val staffRoles: List<Role> get() = entries.filter { it.isStaff }
    }
}

data class User(
    val username: String,
    val name: String,
    val role: Role,
    /** Solo personal: hash del PIN (nunca el PIN en texto plano). */
    val pinHash: String = "",
    val classroomId: String? = null,
)

/** Resultado de ingresar un PIN de personal. */
sealed interface StaffLogin {
    data class Success(val user: User) : StaffLogin
    data class WrongPin(val attemptsLeft: Int) : StaffLogin
    data class Locked(val seconds: Int) : StaffLogin
}

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

/** Persona que confirmó "Estoy a salvo" durante la evacuación. */
data class SafeMark(
    val username: String,
    val name: String,
    val role: Role,
    val classroomId: String?,
    val timeMillis: Long,
)

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
    AlertKind.HAZARD, AlertKind.REQUEST -> user.role.has(Permission.VIEW_HAZARD_ALERTS) || createdBy == user.username
    AlertKind.SYSTEM -> user.role.has(Permission.VIEW_SYSTEM_ALERTS)
    AlertKind.EVACUATION -> true
}

/** La evacuación solo se cierra al finalizarla; el resto lo resuelve quien tiene el permiso. */
fun Alert.canBeResolvedBy(role: Role): Boolean = when (kind) {
    AlertKind.EVACUATION -> false
    AlertKind.SYSTEM -> role.has(Permission.RESOLVE_SYSTEM_ALERTS)
    AlertKind.HAZARD, AlertKind.REQUEST -> role.has(Permission.RESOLVE_ALERTS)
}

// =====================================================================
// Personal y auditoría
// =====================================================================

/** Reglas para PIN nuevos (las cuentas demo usan 1111/2222/3333): 4 dígitos, no triviales. Devuelve el motivo del rechazo o null si es válido. */
fun validatePin(pin: String): String? = when {
    pin.length != 4 || !pin.all { it.isDigit() } -> "El PIN debe tener 4 dígitos"
    pin.toSet().size == 1 -> "Evita un PIN con el mismo dígito repetido"
    pin in setOf("1234", "4321", "0000") -> "Ese PIN es demasiado fácil de adivinar"
    else -> null
}

/** Cuenta de personal (vive en la base de datos; dirección la administra). */
data class StaffAccount(
    val username: String,
    val name: String,
    val role: Role,
    val classroomId: String?,
    val active: Boolean,
) {
    fun toUser(): User = User(username, name, role, classroomId = classroomId)
}

data class AuditEntry(
    val id: Long = 0,
    val timeMillis: Long,
    val actor: String,
    val role: String,
    val action: String,
    val detail: String,
)
