package com.floresvalencia.echoguard.ui.screens.evacuation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
)

class EvacuationViewModel(private val repo: EchoRepository = EchoRepository) : ViewModel() {
    private val live = combine(repo.emergency, repo.now) { e, now -> e.active to now }

    val ui: StateFlow<EvacuationUiState> = combine(repo.occupancy, repo.routes, repo.classrooms, repo.session, repo.safe) { o, routes, rooms, user, safe ->
        EvacuationUiState(
            occupancy = o, routes = routes, recommended = routes.recommended(), classrooms = rooms,
            canReportRoutes = user?.role?.canReportRoutes == true,
            canSeeAllClassrooms = user?.role?.canSeeAllClassrooms == true,
            safe = if (user?.role?.canSeeAllClassrooms == true) safe.sortedByDescending { it.timeMillis } else emptyList(),
        )
    }.combine(live) { s, (active, now) ->
        s.copy(emergencyActive = active, entries5m = s.occupancy.entriesLast5Min(now), exits5m = s.occupancy.exitsLast5Min(now))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EvacuationUiState())

    fun registerExit(): Boolean = repo.registerExit()
    fun registerEntry(rescue: Boolean = false): EntryResult = repo.registerEntry(rescue)
    fun reset() = repo.resetOccupancy()
    fun cycleRoute(id: String) {
        if (ui.value.canReportRoutes) repo.cycleRoute(id)
    }
    fun toggleAssist(classroomId: String, personId: Int) {
        if (ui.value.canSeeAllClassrooms) repo.toggleAssistLocated(classroomId, personId)
    }
}
