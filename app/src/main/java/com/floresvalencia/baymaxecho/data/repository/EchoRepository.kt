package com.floresvalencia.baymaxecho.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.floresvalencia.baymaxecho.data.model.Alert
import com.floresvalencia.baymaxecho.data.model.AlertKind
import com.floresvalencia.baymaxecho.data.model.ArrowDir
import com.floresvalencia.baymaxecho.data.model.AssistPerson
import com.floresvalencia.baymaxecho.data.model.Classroom
import com.floresvalencia.baymaxecho.data.model.EmergencyMode
import com.floresvalencia.baymaxecho.data.model.EmergencyState
import com.floresvalencia.baymaxecho.data.model.ExitRoute
import com.floresvalencia.baymaxecho.data.model.HazardType
import com.floresvalencia.baymaxecho.data.model.MapPoint
import com.floresvalencia.baymaxecho.data.model.Occupancy
import com.floresvalencia.baymaxecho.data.model.Role
import com.floresvalencia.baymaxecho.data.model.RollStatus
import com.floresvalencia.baymaxecho.data.model.RouteStatus
import com.floresvalencia.baymaxecho.data.model.Rover
import com.floresvalencia.baymaxecho.data.model.RoverMode
import com.floresvalencia.baymaxecho.data.model.SensorReadings
import com.floresvalencia.baymaxecho.data.model.Station
import com.floresvalencia.baymaxecho.data.model.User
import com.floresvalencia.baymaxecho.data.model.recommended
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * Fuente única de verdad (datos simulados).
 * Para el hardware real se reemplaza el cuerpo de estas funciones (MQTT, Firebase,
 * HTTP al ESP32...). Pantallas y ViewModels no cambian.
 */
object EchoRepository {

    /** Plano del piso en metros (ancho x alto). */
    const val FLOOR_W = 60f
    const val FLOOR_H = 34f

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val clockFormat = DateTimeFormatter.ofPattern("HH:mm")
    private val alertIds = AtomicInteger(1000)
    private val initialOccupancy = Occupancy()

    // ---------------- Sesión ----------------
    /**
     * Personal con PIN (demo). En producción los PIN deben validarse en un servidor
     * y guardarse cifrados, nunca en el código.
     */
    val staffUsers = listOf(
        User("gerardo", "Gerardo", Role.BRIGADISTA, pin = "1111"),
        User("director", "Patricia", Role.DIRECTOR, pin = "2222"),
        User("docente", "Luis Huamán", Role.DOCENTE, pin = "3333", classroomId = "3B"),
    )

    private const val PREFS = "echo_profile"
    private var prefs: SharedPreferences? = null

    /** Perfil regular guardado en el teléfono: alumno o persona regular. Persiste al cerrar la app. */
    private val _profile = MutableStateFlow<User?>(null)
    val profile: StateFlow<User?> = _profile.asStateFlow()

    /** Modo personal (docente / brigadista / dirección): solo en memoria, pide PIN cada vez. */
    private val _staff = MutableStateFlow<User?>(null)

    /** Usuario efectivo: el modo personal si está activo; si no, el perfil regular. */
    private val _session = MutableStateFlow<User?>(null)
    val session: StateFlow<User?> = _session.asStateFlow()

    private fun refreshSession() { _session.value = _staff.value ?: _profile.value }

    /** Llamar una vez al iniciar la app (EchoApplication) para cargar el perfil guardado. */
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        val id = p.getString("id", null)
        val name = p.getString("name", null)
        if (id != null && name != null) {
            val role = if (p.getString("role", Role.GENERAL.name) == Role.ALUMNO.name) Role.ALUMNO else Role.GENERAL
            _profile.value = User(id, name, role, classroomId = p.getString("classroom", null))
        }
        refreshSession()
    }

    // ---------------- Edificio ----------------
    val stations = listOf(
        Station("E1", "Estación E1 · Ingreso", MapPoint(8f, 17f), rangeM = 35),
        Station("E2", "Estación E2 · Ala norte", MapPoint(44f, 8f), rangeM = 30),
    )

    private val _routes = MutableStateFlow(
        listOf(
            ExitRoute("A", "Salida A", "Pasillo este", 31, RouteStatus.FREE, MapPoint(60f, 17f), ArrowDir.RIGHT),
            ExitRoute("B", "Salida B", "Escalera norte", 24, RouteStatus.CONGESTED, MapPoint(30f, 0f), ArrowDir.UP),
            ExitRoute("C", "Salida C", "Patio central", 45, RouteStatus.FREE, MapPoint(30f, 34f), ArrowDir.DOWN),
        )
    )
    val routes: StateFlow<List<ExitRoute>> = _routes.asStateFlow()

    // ---------------- Flota ----------------
    private val start = System.currentTimeMillis()
    private val _rovers = MutableStateFlow(
        listOf(
            Rover("R-01", "Rover R-01", "E1", RoverMode.DOCKED, 87, MapPoint(8f, 17f), "Estación E1 · Ingreso", start),
            Rover("R-02", "Rover R-02", "E1", RoverMode.PATROLLING, 64, MapPoint(40f, 17f), zoneLabel(MapPoint(40f, 17f)), start, lightRoute = true),
            Rover("R-03", "Rover R-03", "E2", RoverMode.CHARGING, 31, MapPoint(44f, 8f), "Estación E2 · Ala norte", start),
            // Se quedó sin batería: se conserva su ÚLTIMA ubicación registrada.
            Rover("R-04", "Rover R-04", "E2", RoverMode.OFFLINE, 0, MapPoint(22f, 27f), zoneLabel(MapPoint(22f, 27f)), start - 7 * 60_000),
        )
    )
    val rovers: StateFlow<List<Rover>> = _rovers.asStateFlow()

    private val _selectedRoverId = MutableStateFlow("R-02")
    val selectedRoverId: StateFlow<String> = _selectedRoverId.asStateFlow()

    // ---------------- Aforo y aulas ----------------
    private val _occupancy = MutableStateFlow(initialOccupancy)
    val occupancy: StateFlow<Occupancy> = _occupancy.asStateFlow()

    private val initialClassrooms = listOf(
        Classroom("3A", 1, "Rosa Quispe", 28, assist = listOf(AssistPerson(1, "Diego M.", "Hipoacusia"))),
        Classroom("3B", 1, "Luis Huamán", 30, assist = listOf(AssistPerson(2, "Mateo R.", "Silla de ruedas"))),
        Classroom("3C", 1, "Carmen Rojas", 27),
        Classroom("4A", 2, "Jorge Salas", 29, assist = listOf(AssistPerson(3, "Valeria C.", "Baja visión"))),
        Classroom("LAB", 2, "Ana Torres", 18),
    )
    private val _classrooms = MutableStateFlow(initialClassrooms)
    val classrooms: StateFlow<List<Classroom>> = _classrooms.asStateFlow()

    // ---------------- Alertas y emergencia ----------------
    private val _alerts = MutableStateFlow(
        listOf(
            Alert(1, AlertKind.SYSTEM, "Revisión del sistema completada", "Estación E1", "08:42",
                critical = false, active = false, detail = "Sensores, cámaras y enlaces verificados."),
            Alert(2, AlertKind.SYSTEM, "Rover R-04 sin batería", "Rover R-04", "08:51",
                critical = false, active = true, roverId = "R-04",
                detail = "Última ubicación registrada: ${zoneLabel(MapPoint(22f, 27f))}. Recuperarlo y ponerlo a cargar."),
        )
    )
    val alerts: StateFlow<List<Alert>> = _alerts.asStateFlow()

    private val _emergency = MutableStateFlow(EmergencyState())
    val emergency: StateFlow<EmergencyState> = _emergency.asStateFlow()

    private val _now = MutableStateFlow(System.currentTimeMillis())
    val now: StateFlow<Long> = _now.asStateFlow()

    init {
        scope.launch {
            var tick = 0
            while (isActive) {
                delay(1_000)
                tick++
                _now.value = System.currentTimeMillis()
                _rovers.update { list -> list.map { simulateTick(it, tick) } }
            }
        }
    }

    private fun simulateTick(r: Rover, tick: Int): Rover {
        if (!r.online) return r
        var next = r.copy(latencyMs = Random.nextInt(38, 47), lastSeenMillis = System.currentTimeMillis())
        // Desplegado: consume 1 % cada 20 s. Cargando: +1 % cada 10 s.
        if ((r.mode == RoverMode.PATROLLING || r.mode == RoverMode.LOW_BATTERY) && tick % 20 == 0) {
            val b = (r.battery - 1).coerceAtLeast(0)
            next = when {
                b == 0 -> next.copy(battery = 0, mode = RoverMode.OFFLINE)
                b <= 15 -> next.copy(battery = b, mode = RoverMode.LOW_BATTERY)
                else -> next.copy(battery = b)
            }
        } else if (r.mode == RoverMode.CHARGING && tick % 10 == 0) {
            val b = (r.battery + 1).coerceAtMost(100)
            next = next.copy(battery = b, mode = if (b >= 95) RoverMode.DOCKED else RoverMode.CHARGING)
        }
        return next
    }

    fun clock(): String = LocalTime.now().format(clockFormat)

    /** Zona legible a partir de coordenadas del plano. */
    fun zoneLabel(p: MapPoint): String = when {
        p.y in 14f..20f -> if (p.x < 30f) "Pasillo central oeste" else "Pasillo central este"
        p.x in 27f..33f -> if (p.y < 15f) "Pasillo norte" else "Pasillo sur"
        p.y < 15f -> if (p.x < 28f) "Aulas 3A · 3B" else "Aulas 3C · Lab"
        else -> if (p.x < 28f) "Aula 4A · Dirección" else "Patio interior"
    }

    fun stationOf(r: Rover): Station = stations.first { it.id == r.homeStationId }

    // =================================================================
    // Sesión
    // =================================================================
    /** Registro único del perfil regular. No pide PIN y queda guardado. */
    fun register(name: String, student: Boolean, classroomId: String?): User {
        val user = User(
            username = "u-" + UUID.randomUUID().toString().take(8),
            name = name.trim(),
            role = if (student) Role.ALUMNO else Role.GENERAL,
            classroomId = if (student) classroomId else null,
        )
        prefs?.edit()
            ?.putString("id", user.username)
            ?.putString("name", user.name)
            ?.putString("role", user.role.name)
            ?.putString("classroom", user.classroomId)
            ?.apply()
        _profile.value = user
        refreshSession()
        return user
    }

    /** Borra el perfil guardado (por ejemplo, si el teléfono cambia de dueño). */
    fun clearProfile() {
        prefs?.edit()?.clear()?.apply()
        _profile.value = null
        _staff.value = null
        refreshSession()
    }

    /** Entrar como docente, brigadista o dirección. */
    fun enterStaffMode(pin: String): User? =
        staffUsers.firstOrNull { it.pin == pin }?.also {
            _staff.value = it
            refreshSession()
        }

    /** Volver al perfil regular sin perder el registro. */
    fun exitStaffMode() {
        _staff.value = null
        refreshSession()
    }

    // =================================================================
    // Flota
    // =================================================================
    fun selectRover(id: String) { _selectedRoverId.value = id }

    private fun updateRover(id: String, block: (Rover) -> Rover) {
        _rovers.update { list -> list.map { if (it.id == id) block(it) else it } }
    }

    private fun patrolPoint(r: Rover) = if (r.homeStationId == "E1") MapPoint(20f, 17f) else MapPoint(40f, 17f)

    /** Desplegar / recoger. Devuelve un mensaje si no se puede. */
    fun toggleDeploy(id: String): String? {
        val r = _rovers.value.firstOrNull { it.id == id } ?: return "Rover no encontrado"
        if (!r.online) return "${r.id} está sin señal"
        return when (r.mode) {
            RoverMode.DOCKED, RoverMode.CHARGING -> {
                if (r.battery <= 20) return "Batería insuficiente (${r.battery}%)"
                val p = patrolPoint(r)
                updateRover(id) { it.copy(mode = RoverMode.PATROLLING, position = p, locationLabel = zoneLabel(p)) }
                null
            }
            else -> {
                val s = stationOf(r)
                updateRover(id) {
                    it.copy(mode = RoverMode.DOCKED, position = s.position, locationLabel = s.name, lightRoute = false)
                }
                null
            }
        }
    }

    fun setLightRoute(id: String, on: Boolean) = updateRover(id) { it.copy(lightRoute = on) }

    fun setFacingBack(id: String, back: Boolean) = updateRover(id) { it.copy(facingBack = back) }

    /** Demo: el rover agota la batería y queda su última ubicación registrada. */
    fun simulateBatteryDeath(id: String) {
        val r = _rovers.value.firstOrNull { it.id == id } ?: return
        updateRover(id) { it.copy(battery = 0, mode = RoverMode.OFFLINE, lastSeenMillis = System.currentTimeMillis()) }
        addAlert(
            AlertKind.SYSTEM, "${r.id} sin batería", r.name, critical = true, roverId = r.id,
            detail = "Última ubicación registrada: ${r.locationLabel}. Recuperarlo cuando sea seguro.",
        )
    }

    /** Demo: pérdida / recuperación del enlace. */
    fun toggleLink(id: String) {
        val r = _rovers.value.firstOrNull { it.id == id } ?: return
        if (r.online) {
            updateRover(id) { it.copy(mode = RoverMode.OFFLINE) }
            addAlert(
                AlertKind.SYSTEM, "${r.id} sin señal", r.name, critical = true, roverId = r.id,
                detail = "Última ubicación registrada: ${r.locationLabel}. Puede estar fuera del rango operativo.",
            )
        } else if (r.battery > 0) {
            val atHome = r.position == stationOf(r).position
            updateRover(id) { it.copy(mode = if (atHome) RoverMode.DOCKED else RoverMode.PATROLLING, lastSeenMillis = System.currentTimeMillis()) }
            resolveRoverAlerts(id)
        }
    }

    /** El brigadista recogió físicamente el rover y lo puso a cargar. */
    fun markRecovered(id: String) {
        val r = _rovers.value.firstOrNull { it.id == id } ?: return
        val s = stationOf(r)
        updateRover(id) {
            it.copy(
                mode = RoverMode.CHARGING, battery = it.battery.coerceAtLeast(3), position = s.position,
                locationLabel = s.name, lastSeenMillis = System.currentTimeMillis(), sensors = SensorReadings(), lightRoute = false,
            )
        }
        resolveRoverAlerts(id)
    }

    private fun resolveRoverAlerts(id: String) {
        _alerts.update { l -> l.map { if (it.roverId == id && it.kind == AlertKind.SYSTEM) it.copy(active = false) else it } }
    }

    // =================================================================
    // Detección por sensores
    // =================================================================
    /** Demo: un rover detecta un peligro con sus sensores y genera una alerta crítica. */
    fun simulateHazard(type: HazardType) {
        val list = _rovers.value
        val r = list.firstOrNull { it.online && it.mode == RoverMode.PATROLLING } ?: list.firstOrNull { it.online }
        val (sensors, reading) = when (type) {
            HazardType.SMOKE -> SensorReadings(smokePpm = 640) to "humo (640 ppm)"
            HazardType.GAS -> SensorReadings(coPpm = 120) to "monóxido de carbono (120 ppm)"
            HazardType.FIRE -> SensorReadings(smokePpm = 420, temperatureC = 68, flame = true) to "llama y 68 °C"
            HazardType.EARTHQUAKE -> SensorReadings(seismicG = 0.32f, dustUg = 210) to "vibración sísmica (0.32 g)"
            else -> SensorReadings() to type.label.lowercase()
        }
        if (r != null) updateRover(r.id) { it.copy(sensors = sensors) }
        addAlert(
            AlertKind.HAZARD, "${type.label} detectado", r?.name ?: "Estación E1", critical = true,
            hazard = type, roverId = r?.id,
            detail = "${r?.id ?: "Sensor fijo"} detectó $reading en ${r?.locationLabel ?: "Ingreso"}.",
        )
    }

    // =================================================================
    // Reportes de alumnos / docentes (NO inician evacuación)
    // =================================================================
    fun reportEmergency(user: User?, type: HazardType) {
        val who = when {
            user == null -> "Reporte sin sesión"
            user.role == Role.GENERAL -> "Usuario · ${user.name}"
            user.classroomId != null -> "${user.role.label} · ${user.name} (${user.classroomId})"
            else -> "${user.role.label} · ${user.name}"
        }
        addAlert(
            AlertKind.REQUEST,
            if (type == HazardType.INJURED) "Solicitud de ayuda" else "Reporte: ${type.label}",
            who, critical = true, hazard = type, createdBy = user?.username,
            detail = if (user?.role == Role.DOCENTE) "El docente solicita evaluar una evacuación." else "Verificar en el lugar antes de evacuar.",
        )
    }

    // =================================================================
    // Alertas
    // =================================================================
    private fun addAlert(
        kind: AlertKind, title: String, source: String, critical: Boolean, detail: String,
        hazard: HazardType? = null, roverId: String? = null, createdBy: String? = null,
    ) {
        val a = Alert(alertIds.incrementAndGet(), kind, title, source, clock(), critical, true, detail, hazard, roverId, createdBy)
        _alerts.update { it + a }
    }

    /** Resolver / descartar. Si era una detección, normaliza los sensores del rover. */
    fun resolveAlert(id: Int) {
        val a = _alerts.value.firstOrNull { it.id == id } ?: return
        _alerts.update { l -> l.map { if (it.id == id) it.copy(active = false) else it } }
        if (a.kind == AlertKind.HAZARD && a.roverId != null) updateRover(a.roverId) { it.copy(sensors = SensorReadings()) }
    }

    // =================================================================
    // Evacuación (solo roles autorizados)
    // =================================================================
    fun startEmergency(user: User?, mode: EmergencyMode, cause: HazardType?): Boolean {
        if (user == null || !user.role.canStartEvacuation || mode == EmergencyMode.NONE) return false
        _emergency.value = EmergencyState(mode, System.currentTimeMillis(), user.name, cause)
        // Despliega todos los rovers disponibles con ruta luminosa.
        _rovers.update { list ->
            list.map { r ->
                if (r.available) {
                    val p = patrolPoint(r)
                    r.copy(mode = RoverMode.PATROLLING, lightRoute = true, position = p, locationLabel = zoneLabel(p))
                } else r
            }
        }
        // Las detecciones y reportes pendientes quedan atendidos por esta evacuación.
        _alerts.update { l -> l.map { if (it.active && (it.kind == AlertKind.HAZARD || it.kind == AlertKind.REQUEST)) it.copy(active = false) else it } }
        // Se reinicia el pase de lista de las aulas.
        _classrooms.value = initialClassrooms
        val route = _routes.value.recommended()
        addAlert(
            AlertKind.EVACUATION,
            if (mode == EmergencyMode.REAL) "Evacuación general" else "Simulacro iniciado",
            "${user.role.label} · ${user.name}", critical = mode == EmergencyMode.REAL, createdBy = user.username,
            hazard = cause,
            detail = route?.let { "Ruta: ${it.name} · ${it.path}" } ?: "Sin rutas libres. Esperar indicaciones.",
        )
        return true
    }

    /** Devuelve la duración, o null si el usuario no tiene permiso. */
    fun stopEmergency(user: User?): Long? {
        if (user == null || !user.role.canStartEvacuation) return null
        val e = _emergency.value
        if (!e.active) return 0L
        _emergency.value = EmergencyState()
        _alerts.update { l -> l.map { if (it.kind == AlertKind.EVACUATION) it.copy(active = false) else it } }
        return System.currentTimeMillis() - e.startedAtMillis
    }

    // =================================================================
    // Aforo, rutas y aulas
    // =================================================================
    fun registerExit(): Boolean {
        val o = _occupancy.value
        if (o.inside <= 0) return false
        _occupancy.value = o.copy(inside = o.inside - 1, evacuated = o.evacuated + 1,
            pending = (o.pending - 1).coerceAtLeast(0), exits5m = o.exits5m + 1)
        return true
    }

    fun registerEntry(): Boolean {
        val o = _occupancy.value
        if (o.inside >= o.capacity) return false
        _occupancy.value = o.copy(inside = o.inside + 1, entries5m = o.entries5m + 1)
        return true
    }

    fun resetOccupancy() { _occupancy.value = initialOccupancy }

    fun cycleRoute(id: String) {
        _routes.update { list ->
            list.map { r ->
                if (r.id != id) r else r.copy(
                    status = when (r.status) {
                        RouteStatus.FREE -> RouteStatus.CONGESTED
                        RouteStatus.CONGESTED -> RouteStatus.BLOCKED
                        RouteStatus.BLOCKED -> RouteStatus.FREE
                    }
                )
            }
        }
    }

    fun setRoll(classroomId: String, status: RollStatus, missing: Int) {
        _classrooms.update { l ->
            l.map { if (it.id == classroomId) it.copy(status = status, missing = if (status == RollStatus.MISSING) missing else 0) else it }
        }
    }

    fun toggleAssistLocated(classroomId: String, personId: Int) {
        _classrooms.update { l ->
            l.map { c ->
                if (c.id != classroomId) c
                else c.copy(assist = c.assist.map { if (it.id == personId) it.copy(located = !it.located) else it })
            }
        }
    }
}
