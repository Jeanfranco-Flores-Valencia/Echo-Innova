package com.floresvalencia.echoguard.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.floresvalencia.echoguard.data.model.Alert
import com.floresvalencia.echoguard.data.model.AlertKind
import com.floresvalencia.echoguard.data.model.ArrowDir
import com.floresvalencia.echoguard.data.model.AssistPerson
import com.floresvalencia.echoguard.data.model.AuditEntry
import com.floresvalencia.echoguard.data.model.Classroom
import com.floresvalencia.echoguard.data.model.EmergencyMode
import com.floresvalencia.echoguard.data.model.EmergencyState
import com.floresvalencia.echoguard.data.model.EntryResult
import com.floresvalencia.echoguard.data.model.EvacuationSummary
import com.floresvalencia.echoguard.data.model.ExitRoute
import com.floresvalencia.echoguard.data.model.HazardType
import com.floresvalencia.echoguard.data.model.MapPoint
import com.floresvalencia.echoguard.data.model.Occupancy
import com.floresvalencia.echoguard.data.model.Permission
import com.floresvalencia.echoguard.data.model.Role
import com.floresvalencia.echoguard.data.model.RollStatus
import com.floresvalencia.echoguard.data.model.RouteStatus
import com.floresvalencia.echoguard.data.model.Rover
import com.floresvalencia.echoguard.data.model.RoverMode
import com.floresvalencia.echoguard.data.model.SafeMark
import com.floresvalencia.echoguard.data.model.SensorReadings
import com.floresvalencia.echoguard.data.model.StaffLogin
import com.floresvalencia.echoguard.data.model.Station
import com.floresvalencia.echoguard.data.model.User
import com.floresvalencia.echoguard.data.model.canBeResolvedBy
import com.floresvalencia.echoguard.data.model.recommended
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    const val NO_PERMISSION = "Tu rol no tiene permiso para esta acción"
    const val SITE_NAME = "Centro educativo · Bloque A"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val clockFormat = DateTimeFormatter.ofPattern("HH:mm")
    private val alertIds = AtomicInteger(1000)
    private val initialOccupancy = Occupancy()

    // ---------------- Sesión ----------------
    /** Cuentas de personal. EchoApplication lo reemplaza por la base de datos. */
    var staffDirectory: StaffDirectory = InMemoryStaffDirectory()

    /** Registro de acciones sensibles. EchoApplication lo conecta a la base de datos. */
    var auditLog: AuditLog = AuditLog { }

    private const val MAX_PIN_ATTEMPTS = 5
    private const val PIN_LOCK_MS = 30_000L

    /** El modo personal se cierra solo tras este tiempo sin tocar la pantalla (nunca durante una emergencia). */
    const val STAFF_IDLE_MS = 10 * 60_000L

    private var failedPins = 0
    private var pinLockedUntil = 0L
    @Volatile private var lastInteraction = System.currentTimeMillis()

    /** Avisos breves para mostrar al usuario (p. ej., "sesión cerrada por inactividad"). */
    private val _notices = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val notices: SharedFlow<String> = _notices.asSharedFlow()

    /** ¿El usuario de la sesión actual tiene este permiso? Toda acción sensible pasa por aquí. */
    fun can(permission: Permission): Boolean = _session.value?.role?.has(permission) == true

    /** Para otros repositorios (p. ej., administración del personal o SOS). */
    fun recordAudit(action: String, detail: String = "") = audit(action, detail)

    private fun audit(action: String, detail: String = "", user: User? = _session.value) {
        auditLog.record(
            AuditEntry(
                timeMillis = System.currentTimeMillis(),
                actor = user?.name ?: "Sin sesión",
                role = user?.role?.label ?: "—",
                action = action,
                detail = detail,
            )
        )
    }

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

    private val initialRoutes =
        listOf(
            ExitRoute("A", "Salida A", "Pasillo este", 31, RouteStatus.FREE, MapPoint(60f, 17f), ArrowDir.RIGHT),
            ExitRoute("B", "Salida B", "Escalera norte", 24, RouteStatus.CONGESTED, MapPoint(30f, 0f), ArrowDir.UP),
            ExitRoute("C", "Salida C", "Patio central", 45, RouteStatus.FREE, MapPoint(30f, 34f), ArrowDir.DOWN),
        )
    private val _routes = MutableStateFlow(initialRoutes)
    val routes: StateFlow<List<ExitRoute>> = _routes.asStateFlow()

    // ---------------- Flota ----------------
    private val start = System.currentTimeMillis()
    private val initialRovers =
        listOf(
            Rover("R-01", "Rover R-01", "E1", RoverMode.DOCKED, 87, MapPoint(8f, 17f), "Estación E1 · Ingreso", start),
            Rover("R-02", "Rover R-02", "E1", RoverMode.PATROLLING, 64, MapPoint(40f, 17f), zoneLabel(MapPoint(40f, 17f)), start, lightRoute = true),
            Rover("R-03", "Rover R-03", "E2", RoverMode.CHARGING, 31, MapPoint(44f, 8f), "Estación E2 · Ala norte", start),
            // Se quedó sin batería: se conserva su ÚLTIMA ubicación registrada.
            Rover("R-04", "Rover R-04", "E2", RoverMode.OFFLINE, 0, MapPoint(22f, 27f), zoneLabel(MapPoint(22f, 27f)), start - 7 * 60_000),
        )
    private val _rovers = MutableStateFlow(initialRovers)
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
    private val initialAlerts =
        listOf(
            Alert(1, AlertKind.SYSTEM, "Revisión del sistema completada", "Estación E1", "08:42",
                critical = false, active = false, detail = "Sensores, cámaras y enlaces verificados."),
            Alert(2, AlertKind.SYSTEM, "Rover R-04 sin batería", "Rover R-04", "08:51",
                critical = false, active = true, roverId = "R-04",
                detail = "Última ubicación registrada: ${zoneLabel(MapPoint(22f, 27f))}. Recuperarlo y ponerlo a cargar."),
        )
    private val _alerts = MutableStateFlow(initialAlerts)
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
                checkStaffIdle()
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

    /**
     * Entrar como docente, brigadista o dirección.
     * Tras [MAX_PIN_ATTEMPTS] intentos fallidos se bloquea durante 30 s.
     */
    suspend fun enterStaffMode(pin: String, nowMillis: Long = System.currentTimeMillis()): StaffLogin {
        if (nowMillis < pinLockedUntil) return StaffLogin.Locked(((pinLockedUntil - nowMillis + 999) / 1000).toInt())
        val account = staffDirectory.findActiveByPinHash(Pins.hash(pin))
        if (account == null) {
            failedPins++
            if (failedPins >= MAX_PIN_ATTEMPTS) {
                failedPins = 0
                pinLockedUntil = nowMillis + PIN_LOCK_MS
                audit("PIN bloqueado", "$MAX_PIN_ATTEMPTS intentos fallidos seguidos")
                return StaffLogin.Locked((PIN_LOCK_MS / 1000).toInt())
            }
            return StaffLogin.WrongPin(MAX_PIN_ATTEMPTS - failedPins)
        }
        failedPins = 0
        val user = account.toUser()
        lastInteraction = nowMillis
        _staff.value = user
        refreshSession()
        audit("Ingreso de personal", user.role.label, user)
        return StaffLogin.Success(user)
    }

    /** La pantalla fue tocada: reinicia el contador de inactividad del modo personal. */
    fun touch() { lastInteraction = System.currentTimeMillis() }

    /** Cierra el modo personal si lleva demasiado tiempo inactivo. Devuelve true si lo cerró. */
    fun checkStaffIdle(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val staff = _staff.value ?: return false
        if (_emergency.value.active || nowMillis - lastInteraction < STAFF_IDLE_MS) return false
        audit("Sesión cerrada por inactividad", "", staff)
        _staff.value = null
        refreshSession()
        _notices.tryEmit("Modo personal cerrado por inactividad")
        return true
    }

    /** Volver al perfil regular sin perder el registro. */
    fun exitStaffMode() {
        _staff.value?.let { audit("Salida del modo personal", "", it) }
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
        if (!can(Permission.CONTROL_ROVERS)) return NO_PERMISSION
        val r = _rovers.value.firstOrNull { it.id == id } ?: return "Rover no encontrado"
        if (!r.online) return "${r.id} está sin señal"
        return when (r.mode) {
            RoverMode.DOCKED, RoverMode.CHARGING -> {
                if (r.battery <= 20) return "Batería insuficiente (${r.battery}%)"
                val p = patrolPoint(r)
                updateRover(id) { it.copy(mode = RoverMode.PATROLLING, position = p, locationLabel = zoneLabel(p)) }
                audit("Rover desplegado", r.id)
                null
            }
            else -> {
                val s = stationOf(r)
                updateRover(id) {
                    it.copy(mode = RoverMode.DOCKED, position = s.position, locationLabel = s.name, lightRoute = false)
                }
                audit("Rover recogido", r.id)
                null
            }
        }
    }

    fun setLightRoute(id: String, on: Boolean) {
        if (!can(Permission.CONTROL_ROVERS)) return
        updateRover(id) { it.copy(lightRoute = on) }
        audit(if (on) "Ruta luminosa encendida" else "Ruta luminosa apagada", id)
    }

    fun setFacingBack(id: String, back: Boolean) {
        if (can(Permission.CONTROL_ROVERS)) updateRover(id) { it.copy(facingBack = back) }
    }

    /** Demo: el rover agota la batería y queda su última ubicación registrada. */
    fun simulateBatteryDeath(id: String) {
        if (!can(Permission.CONTROL_ROVERS)) return
        val r = _rovers.value.firstOrNull { it.id == id } ?: return
        updateRover(id) { it.copy(battery = 0, mode = RoverMode.OFFLINE, lastSeenMillis = System.currentTimeMillis()) }
        addAlert(
            AlertKind.SYSTEM, "${r.id} sin batería", r.name, critical = true, roverId = r.id,
            detail = "Última ubicación registrada: ${r.locationLabel}. Recuperarlo cuando sea seguro.",
        )
    }

    /** Demo: pérdida / recuperación del enlace. */
    fun toggleLink(id: String) {
        if (!can(Permission.CONTROL_ROVERS)) return
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
        if (!can(Permission.CONTROL_ROVERS)) return
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
        if (!can(Permission.CONTROL_ROVERS)) return
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
        if (!canResolve(a)) return
        _alerts.update { l -> l.map { if (it.id == id) it.copy(active = false) else it } }
        audit("Alerta resuelta", a.title)
        if (a.kind == AlertKind.HAZARD && a.roverId != null) updateRover(a.roverId) { it.copy(sensors = SensorReadings()) }
    }

    /** La evacuación se cierra al finalizarla; el resto lo decide quien puede evacuar (o quien ve la flota, si es del sistema). */
    fun canResolve(a: Alert): Boolean = _session.value?.role?.let { a.canBeResolvedBy(it) } == true

    // =================================================================
    // Evacuación (solo roles autorizados)
    // =================================================================
    fun startEmergency(user: User?, mode: EmergencyMode, cause: HazardType?): Boolean {
        if (user == null || !user.role.canStartEvacuation || mode == EmergencyMode.NONE) return false
        _emergency.value = EmergencyState(mode, System.currentTimeMillis(), user.name, cause)
        _safe.value = emptyList()
        // Foto del aforo al sonar la alarma: todos los que están dentro quedan pendientes.
        _occupancy.update { it.copy(atAlarm = it.inside, evacuated = 0) }
        audit(if (mode == EmergencyMode.REAL) "Evacuación REAL iniciada" else "Simulacro iniciado", cause?.label ?: "Sin causa indicada", user)
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

    /** Devuelve el resumen, o null si el usuario no tiene permiso o no había evacuación. */
    fun stopEmergency(user: User?): EvacuationSummary? {
        if (user == null || !user.role.canStartEvacuation) return null
        val e = _emergency.value
        if (!e.active) return null
        val o = _occupancy.value
        val summary = EvacuationSummary(
            mode = e.mode,
            durationMs = System.currentTimeMillis() - e.startedAtMillis,
            atAlarm = o.atAlarm, evacuated = o.evacuated, stillInside = o.inside, markedSafe = _safe.value.size,
        )
        _occupancy.value = o.copy(atAlarm = 0, evacuated = 0)
        _emergency.value = EmergencyState()
        audit(
            if (e.mode == EmergencyMode.REAL) "Evacuación finalizada" else "Simulacro finalizado",
            "${summary.evacuated} de ${summary.atAlarm} salieron · ${summary.stillInside} sin contar · ${summary.markedSafe} a salvo en la app", user,
        )
        _alerts.update { l -> l.map { if (it.kind == AlertKind.EVACUATION) it.copy(active = false) else it } }
        // Los rovers desplegados vuelven a su estación (a cargar si les hace falta) y apagan la ruta luminosa.
        _rovers.update { list ->
            list.map { r ->
                if (r.online && (r.mode == RoverMode.PATROLLING || r.mode == RoverMode.LOW_BATTERY)) {
                    val s = stationOf(r)
                    r.copy(
                        mode = if (r.battery < 95) RoverMode.CHARGING else RoverMode.DOCKED,
                        position = s.position, locationLabel = s.name, lightRoute = false,
                    )
                } else r
            }
        }
        return summary
    }

    // =================================================================
    // Aforo, rutas y aulas
    // =================================================================
    /** Solo se guardan los pasos de los últimos 5 minutos. */
    private fun List<Long>.recent(now: Long, add: Boolean): List<Long> =
        filter { now - it <= Occupancy.RECENT_WINDOW_MS } + if (add) listOf(now) else emptyList()

    /** Alguien salió por la puerta. Durante una evacuación cuenta como evacuada. */
    fun registerExit(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!can(Permission.VIEW_ALL_CLASSROOMS)) return false
        val o = _occupancy.value
        if (o.inside <= 0) return false
        _occupancy.value = o.copy(
            inside = o.inside - 1,
            evacuated = if (_emergency.value.active) o.evacuated + 1 else o.evacuated,
            exitTimes = o.exitTimes.recent(nowMillis, add = true),
            entryTimes = o.entryTimes.recent(nowMillis, add = false),
        )
        return true
    }

    /**
     * Alguien entró. Durante una evacuación nadie debería entrar: solo se acepta con
     * [rescue] = true (bomberos o brigada que vuelve a entrar) y queda en el registro.
     */
    fun registerEntry(rescue: Boolean = false, nowMillis: Long = System.currentTimeMillis()): EntryResult {
        if (!can(Permission.VIEW_ALL_CLASSROOMS)) return EntryResult.NO_PERMISSION
        if (_emergency.value.active && !rescue) return EntryResult.DURING_EVACUATION
        val o = _occupancy.value
        if (o.inside >= o.capacity) return EntryResult.FULL
        _occupancy.value = o.copy(
            inside = o.inside + 1,
            entryTimes = o.entryTimes.recent(nowMillis, add = true),
            exitTimes = o.exitTimes.recent(nowMillis, add = false),
        )
        if (rescue) audit("Reingreso durante la evacuación", "Personal de rescate")
        return EntryResult.OK
    }

    /** Vuelve a contar desde cero. En plena evacuación la foto inicial pasa a ser el conteo actual. */
    fun resetOccupancy() {
        if (!can(Permission.VIEW_ALL_CLASSROOMS)) return
        _occupancy.value = if (_emergency.value.active) {
            _occupancy.value.let { it.copy(atAlarm = it.inside, evacuated = 0, entryTimes = emptyList(), exitTimes = emptyList()) }
        } else initialOccupancy
        audit("Contador de aforo reiniciado")
    }

    fun cycleRoute(id: String) {
        if (!can(Permission.REPORT_ROUTES)) return
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
        _routes.value.firstOrNull { it.id == id }?.let { audit("Salida reportada", "${it.name}: ${it.status.label}") }
    }

    // =================================================================
    // "Estoy a salvo" (como el Safety Check de las apps de emergencia)
    // =================================================================
    private val _safe = MutableStateFlow<List<SafeMark>>(emptyList())
    /** Personas que se marcaron a salvo en la evacuación actual. */
    val safe: StateFlow<List<SafeMark>> = _safe.asStateFlow()

    /** Cualquier persona con sesión puede marcarse a salvo mientras hay una evacuación. */
    fun markSafe(): Boolean {
        val u = _session.value ?: return false
        if (!_emergency.value.active) return false
        if (_safe.value.none { it.username == u.username }) {
            _safe.update { it + SafeMark(u.username, u.name, u.role, u.classroomId, System.currentTimeMillis()) }
        }
        return true
    }

    /** Docente: solo su aula. */
    private fun ownsClassroom(classroomId: String): Boolean =
        can(Permission.TAKE_ROLL) && _session.value?.classroomId == classroomId

    fun setRoll(classroomId: String, status: RollStatus, missing: Int) {
        if (!ownsClassroom(classroomId)) return
        audit("Lista del aula $classroomId", if (status == RollStatus.MISSING) "Faltan $missing" else status.label)
        _classrooms.update { l ->
            l.map { if (it.id == classroomId) it.copy(status = status, missing = if (status == RollStatus.MISSING) missing else 0) else it }
        }
    }

    fun toggleAssistLocated(classroomId: String, personId: Int) {
        if (!can(Permission.VIEW_ALL_CLASSROOMS) && !ownsClassroom(classroomId)) return
        _classrooms.update { l ->
            l.map { c ->
                if (c.id != classroomId) c
                else c.copy(assist = c.assist.map { if (it.id == personId) it.copy(located = !it.located) else it })
            }
        }
    }

    /** Vuelve la demo al estado inicial (entre sesiones de prueba con usuarios y en los tests). */
    fun resetDemo() {
        audit("Demo reiniciada")
        _safe.value = emptyList()
        _staff.value = null
        failedPins = 0
        pinLockedUntil = 0L
        refreshSession()
        _emergency.value = EmergencyState()
        _classrooms.value = initialClassrooms
        _occupancy.value = initialOccupancy
        _rovers.value = initialRovers
        _routes.value = initialRoutes
        _alerts.value = initialAlerts
    }
}
