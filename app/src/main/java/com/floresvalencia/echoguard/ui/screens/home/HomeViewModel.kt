package com.floresvalencia.echoguard.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floresvalencia.echoguard.AppGraph
import com.floresvalencia.echoguard.data.model.Alert
import com.floresvalencia.echoguard.data.model.AlertKind
import com.floresvalencia.echoguard.data.model.Classroom
import com.floresvalencia.echoguard.data.model.EmergencyState
import com.floresvalencia.echoguard.data.model.ExitRoute
import com.floresvalencia.echoguard.data.model.HazardType
import com.floresvalencia.echoguard.data.model.Occupancy
import com.floresvalencia.echoguard.data.model.RollStatus
import com.floresvalencia.echoguard.data.model.RoverMode
import com.floresvalencia.echoguard.data.model.User
import com.floresvalencia.echoguard.data.model.formatElapsed
import com.floresvalencia.echoguard.data.model.recommended
import com.floresvalencia.echoguard.data.repository.EchoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class FleetSummary(
    val total: Int = 0,
    val available: Int = 0,
    val deployed: Int = 0,
    val charging: Int = 0,
    val offline: Int = 0,
)

data class HomeUiState(
    val user: User? = null,
    val occupancy: Occupancy = Occupancy(),
    val emergency: EmergencyState = EmergencyState(),
    val elapsed: String = "00:00",
    val fleet: FleetSummary = FleetSummary(),
    val activeAlerts: Int = 0,
    /** Detecciones y reportes que esperan decisión de la brigada. */
    val pendingDecisions: List<Alert> = emptyList(),
    val classroomsReported: Int = 0,
    val classroomsTotal: Int = 0,
    val missingTotal: Int = 0,
    val assistPending: Int = 0,
    val myClassroom: Classroom? = null,
    val myActiveReports: Int = 0,
    val route: ExitRoute? = null,
    /** Personas que confirmaron "Estoy a salvo" en la evacuación actual. */
    val safeCount: Int = 0,
    val iAmSafe: Boolean = false,
    val contacts: Int = 0,
)

class HomeViewModel(private val repo: EchoRepository = EchoRepository) : ViewModel() {

    private val world = combine(repo.rovers, repo.occupancy, repo.classrooms, repo.routes) { r, o, c, routes ->
        Quad(r, o, c, routes)
    }
    private val people = combine(repo.safe, AppGraph.sos.contacts) { safe, contacts -> safe to contacts.size }

    private val base = combine(repo.session, repo.emergency, repo.now, repo.alerts, world) { user, e, now, alerts, w ->
        val rovers = w.a
        val rooms = w.c
        HomeUiState(
            user = user,
            occupancy = w.b,
            emergency = e,
            elapsed = if (e.active) formatElapsed(now - e.startedAtMillis) else "00:00",
            fleet = FleetSummary(
                total = rovers.size,
                available = rovers.count { it.available },
                deployed = rovers.count { it.mode == RoverMode.PATROLLING || it.mode == RoverMode.LOW_BATTERY },
                charging = rovers.count { it.mode == RoverMode.CHARGING },
                offline = rovers.count { !it.online },
            ),
            activeAlerts = alerts.count { it.active },
            pendingDecisions = alerts.filter { it.active && (it.kind == AlertKind.HAZARD || it.kind == AlertKind.REQUEST) }
                .sortedByDescending { it.id },
            classroomsReported = rooms.count { it.status != RollStatus.PENDING },
            classroomsTotal = rooms.size,
            missingTotal = rooms.sumOf { it.missing },
            assistPending = rooms.sumOf { c -> c.assist.count { !it.located } },
            myClassroom = rooms.firstOrNull { it.id == user?.classroomId },
            myActiveReports = alerts.count { it.active && it.createdBy != null && it.createdBy == user?.username && it.kind == AlertKind.REQUEST },
            route = w.d.recommended(),
        )
    }

    val ui: StateFlow<HomeUiState> = combine(base, people) { s, (safe, contacts) ->
        s.copy(safeCount = safe.size, iAmSafe = safe.any { it.username == s.user?.username }, contacts = contacts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun report(type: HazardType) = repo.reportEmergency(repo.session.value, type)
    fun dismissAlert(id: Int) = repo.resolveAlert(id)
    fun markSafe(): Boolean = repo.markSafe()
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
