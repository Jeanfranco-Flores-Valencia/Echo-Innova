package com.floresvalencia.baymaxecho.ui.screens.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floresvalencia.baymaxecho.data.model.Alert
import com.floresvalencia.baymaxecho.data.model.Role
import com.floresvalencia.baymaxecho.data.model.isVisibleTo
import com.floresvalencia.baymaxecho.data.repository.EchoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AlertsUiState(
    val alerts: List<Alert> = emptyList(),
    val active: Int = 0,
    val critical: Int = 0,
    val role: Role = Role.ALUMNO,
    val emergencyActive: Boolean = false,
)

class AlertsViewModel(private val repo: EchoRepository = EchoRepository) : ViewModel() {
    val ui: StateFlow<AlertsUiState> = combine(repo.alerts, repo.session, repo.emergency) { list, user, e ->
        val visible = if (user == null) emptyList() else list.filter { it.isVisibleTo(user) }
        AlertsUiState(
            alerts = visible.sortedWith(
                compareByDescending<Alert> { it.active && it.critical }
                    .thenByDescending { it.active }
                    .thenByDescending { it.id }
            ),
            active = visible.count { it.active },
            critical = visible.count { it.active && it.critical },
            role = user?.role ?: Role.ALUMNO,
            emergencyActive = e.active,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertsUiState())

    /** Solo quien puede decidir resuelve o descarta alertas. */
    fun resolve(id: Int) {
        if (ui.value.role.canStartEvacuation || ui.value.role.canSeeFleet) repo.resolveAlert(id)
    }
}
