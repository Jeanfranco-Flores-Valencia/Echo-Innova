package com.floresvalencia.echoguard.ui.screens.evacuation

import android.app.Application
import android.content.ActivityNotFoundException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.floresvalencia.echoguard.AppGraph
import com.floresvalencia.echoguard.data.model.AckStatus
import com.floresvalencia.echoguard.data.model.AlertKind
import com.floresvalencia.echoguard.data.model.EmergencyMode
import com.floresvalencia.echoguard.data.model.Responder
import com.floresvalencia.echoguard.data.sos.SmsSender
import com.floresvalencia.echoguard.data.sos.SosMessage
import kotlinx.coroutines.launch
import com.floresvalencia.echoguard.data.model.Classroom
import com.floresvalencia.echoguard.data.model.EntryResult
import com.floresvalencia.echoguard.data.model.ExitRoute
import com.floresvalencia.echoguard.data.model.Occupancy
import com.floresvalencia.echoguard.data.model.SafeMark
import com.floresvalencia.echoguard.data.model.recommended
import com.floresvalencia.echoguard.data.repository.EchoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class EvacuationUiState(
    val occupancy: Occupancy = Occupancy(),
    val routes: List<ExitRoute> = emptyList(),
    val recommended: ExitRoute? = null,
    val classrooms: List<Classroom> = emptyList(),
    val canReportRoutes: Boolean = false,
    val canSeeAllClassrooms: Boolean = false,
    /** Quienes confirmaron "Estoy a salvo" (solo lo ve brigada / dirección). */
    val safe: List<SafeMark> = emptyList(),
    val emergencyActive: Boolean = false,
    val entries5m: Int = 0,
    val exits5m: Int = 0,
    /** Confirmaciones de la alerta de evacuación en curso. */
    val acks: Map<String, AckStatus> = emptyMap(),
    val evacAlertId: Int? = null,
    val responders: List<Responder> = emptyList(),
    /** Teléfonos de responsables guardados por dirección (para el SMS sin internet). */
    val responderPhones: Int = 0,
)

class EvacuationViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = EchoRepository
    private val history = AppGraph.history
    private val live = combine(repo.emergency, repo.now) { e, now -> e.active to now }
    private val delivery = combine(repo.alerts, repo.acks, history.responderPhones) { alerts, acks, phones ->
        val evac = alerts.lastOrNull { it.kind == AlertKind.EVACUATION && it.active }
        Triple(evac?.id, evac?.let { acks[it.id] }.orEmpty(), phones.size)
    }

    val ui: StateFlow<EvacuationUiState> = combine(repo.occupancy, repo.routes, repo.classrooms, repo.session, repo.safe) { o, routes, rooms, user, safe ->
        EvacuationUiState(
            occupancy = o, routes = routes, recommended = routes.recommended(), classrooms = rooms,
            canReportRoutes = user?.role?.canReportRoutes == true,
            canSeeAllClassrooms = user?.role?.canSeeAllClassrooms == true,
            safe = if (user?.role?.canSeeAllClassrooms == true) safe.sortedByDescending { it.timeMillis } else emptyList(),
        )
    }.combine(live) { s, (active, now) ->
        s.copy(emergencyActive = active, entries5m = s.occupancy.entriesLast5Min(now), exits5m = s.occupancy.exitsLast5Min(now))
    }.combine(delivery) { s, (id, acks, phones) ->
        if (!s.canSeeAllClassrooms) s
        else s.copy(evacAlertId = id, acks = acks, responders = repo.responders, responderPhones = phones)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), current())

    /** Estado inicial con los datos reales (sin parpadeo de "sin salida libre" al abrir). */
    private fun current(): EvacuationUiState {
        val user = repo.session.value
        val routes = repo.routes.value
        val o = repo.occupancy.value
        val manager = user?.role?.canSeeAllClassrooms == true
        val evac = repo.alerts.value.lastOrNull { it.kind == AlertKind.EVACUATION && it.active }
        val now = repo.now.value
        return EvacuationUiState(
            occupancy = o, routes = routes, recommended = routes.recommended(), classrooms = repo.classrooms.value,
            canReportRoutes = user?.role?.canReportRoutes == true,
            canSeeAllClassrooms = manager,
            safe = if (manager) repo.safe.value.sortedByDescending { it.timeMillis } else emptyList(),
            emergencyActive = repo.emergency.value.active,
            entries5m = o.entriesLast5Min(now), exits5m = o.exitsLast5Min(now),
            acks = if (manager) evac?.let { repo.acks.value[it.id] }.orEmpty() else emptyMap(),
            evacAlertId = if (manager) evac?.id else null,
            responders = if (manager) repo.responders else emptyList(),
        )
    }

    fun registerExit(): Boolean = repo.registerExit()
    fun registerEntry(rescue: Boolean = false): EntryResult = repo.registerEntry(rescue)
    fun reset() = repo.resetOccupancy()
    fun cycleRoute(id: String) {
        if (ui.value.canReportRoutes) repo.cycleRoute(id)
    }
    fun resendPending(): Int = ui.value.evacAlertId?.let { repo.resendPending(it).size } ?: 0

    /** Texto corto, sin tildes: entra en un solo SMS y se lee en cualquier teléfono. */
    fun responderSms(): String {
        val e = repo.emergency.value
        val route = ui.value.recommended
        return SosMessage.smsSafe(
            "ECHO GUARD: " + (if (e.mode == EmergencyMode.REAL) "EVACUACION REAL" else "SIMULACRO") + " en curso" +
                (e.cause?.let { " (${it.label})" } ?: "") + ". " +
                (route?.let { "Ruta: ${it.name} - ${it.path}. " } ?: "Sin salidas libres: esperar indicaciones. ") +
                "Confirma en la app o responde este SMS. ${repo.clock()}"
        )
    }

    fun canSendSmsDirectly(): Boolean = SmsSender.canSendDirectly(getApplication())

    /** Aviso por SMS a todos los responsables (no necesita internet, solo señal). */
    fun sendSmsToResponders(onDone: (String) -> Unit) {
        viewModelScope.launch {
            val phones = history.responderPhonesNow()
            if (phones.isEmpty()) { onDone("Dirección aún no registró teléfonos de responsables"); return@launch }
            val text = responderSms()
            val results = SmsSender.send(getApplication(), phones.map { it.phone }, text)
            val ok = results.count { it }
            repo.recordAudit("SMS de evacuación a responsables", "$ok de ${phones.size} enviados")
            onDone(if (ok == phones.size) "SMS enviado a $ok responsables" else "SMS enviado a $ok de ${phones.size} (revisa la señal)")
        }
    }

    /** Sin permiso de SMS: abre la app de mensajes con todo escrito. */
    fun openSmsApp(onError: (String) -> Unit) {
        viewModelScope.launch {
            val phones = history.responderPhonesNow()
            if (phones.isEmpty()) { onError("Dirección aún no registró teléfonos de responsables"); return@launch }
            try {
                getApplication<Application>().startActivity(SmsSender.composeIntent(phones.map { it.phone }, responderSms()))
                repo.recordAudit("SMS de evacuación a responsables", "Abierto en la app de mensajes")
            } catch (_: ActivityNotFoundException) {
                onError("Este teléfono no tiene app de mensajes")
            }
        }
    }

    fun toggleAssist(classroomId: String, personId: Int) {
        if (ui.value.canSeeAllClassrooms) repo.toggleAssistLocated(classroomId, personId)
    }
}
