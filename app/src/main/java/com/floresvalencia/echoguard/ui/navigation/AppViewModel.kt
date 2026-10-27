package com.floresvalencia.echoguard.ui.navigation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.floresvalencia.echoguard.data.model.Alert
import com.floresvalencia.echoguard.data.model.AlertKind
import com.floresvalencia.echoguard.data.model.ArrowDir
import com.floresvalencia.echoguard.data.model.EmergencyMode
import com.floresvalencia.echoguard.data.model.EmergencyState
import com.floresvalencia.echoguard.data.model.ExitRoute
import com.floresvalencia.echoguard.data.model.HazardType
import com.floresvalencia.echoguard.data.model.Role
import com.floresvalencia.echoguard.data.model.User
import com.floresvalencia.echoguard.data.model.formatElapsed
import com.floresvalencia.echoguard.data.model.isVisibleTo
import com.floresvalencia.echoguard.data.model.recommended
import com.floresvalencia.echoguard.data.repository.EchoRepository
import com.floresvalencia.echoguard.notification.AlarmNotifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Contenido de la alarma a pantalla completa, ya adaptado al rol. */
data class AlarmUi(
    val alertId: Int,
    val title: String,
    val location: String,
    val instruction: String,
    val primaryText: String?,
    val cause: HazardType?,
    val sign: ArrowDir? = null,
    val signText: String? = null,
)

data class StartRequest(val cause: HazardType?)

data class AppUiState(
    val user: User? = null,
    val emergency: EmergencyState = EmergencyState(),
    val elapsed: String = "00:00",
    val instruction: String = "",
    val activeAlerts: Int = 0,
    val alarm: AlarmUi? = null,
    val startRequest: StartRequest? = null,
)

/** Estado global: sesión, banner, alarma, diálogo de inicio y notificaciones. */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = EchoRepository
    private val acknowledged = MutableStateFlow<Set<Int>>(emptySet())
    private val startRequest = MutableStateFlow<StartRequest?>(null)
    private val notified = mutableSetOf<Int>()

    private val extras = combine(repo.routes, acknowledged, startRequest) { r, a, s -> Triple(r, a, s) }

    val ui: StateFlow<AppUiState> = combine(repo.session, repo.emergency, repo.now, repo.alerts, extras) { user, e, now, alerts, (routes, ack, start) ->
        val route = routes.recommended()
        val visible = alerts.filter { user != null && isRelevant(it, user) }
        AppUiState(
            user = user,
            emergency = e,
            elapsed = if (e.active) formatElapsed(now - e.startedAtMillis) else "00:00",
            instruction = route?.let { "${it.name} · ${it.path}" } ?: "Quédate en zona segura",
            activeAlerts = visible.count { it.active },
            alarm = if (user == null) null else visible
                .filter { it.active && it.critical && it.id !in ack && it.createdBy != user.username }
                .maxByOrNull { it.id }
                ?.let { buildAlarm(it, user, route, e) },
            startRequest = start,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState(user = repo.session.value))

    /** Avisos breves del sistema (p. ej., cierre del modo personal por inactividad). */
    val notices = repo.notices

    /** Si ya hay perfil guardado, la app abre directo en Inicio (sin pasar por el registro). */
    val startDestination: String = if (repo.session.value != null) Routes.HOME else Routes.REGISTER

    init {
        AlarmNotifier.ensureChannel(app)
        notified += repo.alerts.value.map { it.id }
        // Notificación del sistema para alertas críticas nuevas (sirve con la app en segundo plano).
        viewModelScope.launch {
            combine(repo.alerts, repo.session) { a, u -> a to u }.collect { (alerts, user) ->
                if (user == null) return@collect
                alerts.filter { it.active && it.critical && it.id !in notified && it.createdBy != user.username && isRelevant(it, user) }
                    .forEach { a ->
                        notified += a.id
                        AlarmNotifier.notify(getApplication<Application>(), a.id, a.title, a.detail)
                    }
            }
        }
    }

    private fun isRelevant(a: Alert, user: User): Boolean = a.isVisibleTo(user)

    private fun buildAlarm(a: Alert, user: User, route: ExitRoute?, e: EmergencyState): AlarmUi {
        val canStart = user.role.canStartEvacuation && !e.active
        return when (a.kind) {
            AlertKind.HAZARD, AlertKind.REQUEST -> AlarmUi(
                alertId = a.id,
                title = a.title,
                location = a.source,
                instruction = a.detail,
                primaryText = if (canStart) "Iniciar evacuación" else null,
                cause = a.hazard,
            )
            AlertKind.EVACUATION -> {
                val real = e.mode == EmergencyMode.REAL
                val roleTip = when (user.role) {
                    Role.ALUMNO -> "Sigue a tu docente. Camina sin correr y no regreses por tus cosas."
                    Role.GENERAL -> "Sigue la señalética y al personal. Camina sin correr y no regreses."
                    Role.DOCENTE -> "Guía a tu aula y pasa lista en el punto de reunión."
                    else -> a.hazard?.instruction ?: "Coordina la salida y el conteo."
                }
                AlarmUi(
                    alertId = a.id,
                    title = if (real) "Evacuar ahora" else "Simulacro",
                    location = "Activado por ${e.startedBy.ifBlank { a.source }}" + (e.cause?.let { " · ${it.label}" } ?: ""),
                    instruction = roleTip,
                    primaryText = null,
                    cause = e.cause,
                    sign = route?.arrow,
                    signText = route?.let { "${it.name} · ${it.path}" } ?: "Quédate en zona segura",
                )
            }
            AlertKind.SYSTEM -> AlarmUi(a.id, a.title, a.source, a.detail, null, null)
        }
    }

    fun acknowledge(alertId: Int) { acknowledged.value = acknowledged.value + alertId }

    fun requestStart(cause: HazardType?) {
        if (repo.session.value?.role?.canStartEvacuation == true) startRequest.value = StartRequest(cause)
    }

    fun cancelStart() { startRequest.value = null }

    fun start(mode: EmergencyMode): Boolean {
        val cause = startRequest.value?.cause
        startRequest.value = null
        return repo.startEmergency(repo.session.value, mode, cause)
    }

    /** Resumen para mostrar al finalizar, o null si el rol no puede finalizar. */
    fun stopEmergency(): String? {
        val mode = repo.emergency.value.mode
        val ms = repo.stopEmergency(repo.session.value) ?: return null
        val label = if (mode == EmergencyMode.REAL) "Evacuación finalizada" else "Simulacro finalizado"
        return "$label · ${formatElapsed(ms)}"
    }

    /** Volver del modo personal al perfil regular (no borra el registro). */
    fun exitStaffMode() {
        startRequest.value = null
        repo.exitStaffMode()
    }

    /** Borrar el perfil guardado en este teléfono. */
    fun clearProfile() {
        acknowledged.value = emptySet()
        repo.clearProfile()
    }
}
