package com.floresvalencia.baymaxecho.ui.screens.evacuation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floresvalencia.baymaxecho.data.model.Classroom
import com.floresvalencia.baymaxecho.data.model.ExitRoute
import com.floresvalencia.baymaxecho.data.model.Occupancy
import com.floresvalencia.baymaxecho.data.model.recommended
import com.floresvalencia.baymaxecho.data.repository.EchoRepository
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
)

class EvacuationViewModel(private val repo: EchoRepository = EchoRepository) : ViewModel() {
    val ui: StateFlow<EvacuationUiState> = combine(repo.occupancy, repo.routes, repo.classrooms, repo.session) { o, routes, rooms, user ->
        EvacuationUiState(
            occupancy = o, routes = routes, recommended = routes.recommended(), classrooms = rooms,
            canReportRoutes = user?.role?.canReportRoutes == true,
            canSeeAllClassrooms = user?.role?.canSeeAllClassrooms == true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EvacuationUiState())

    fun registerExit(): Boolean = repo.registerExit()
    fun registerEntry(): Boolean = repo.registerEntry()
    fun reset() = repo.resetOccupancy()
    fun cycleRoute(id: String) {
        if (ui.value.canReportRoutes) repo.cycleRoute(id)
    }
    fun toggleAssist(classroomId: String, personId: Int) {
        if (ui.value.canSeeAllClassrooms) repo.toggleAssistLocated(classroomId, personId)
    }
}
