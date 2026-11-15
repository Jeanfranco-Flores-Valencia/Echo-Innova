package com.floresvalencia.echoguard.ui.screens.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floresvalencia.echoguard.data.model.AckStatus
import com.floresvalencia.echoguard.data.model.Alert
import com.floresvalencia.echoguard.data.model.Permission
import com.floresvalencia.echoguard.data.model.Responder
import com.floresvalencia.echoguard.data.model.Role
import com.floresvalencia.echoguard.data.model.isVisibleTo
import com.floresvalencia.echoguard.data.repository.EchoRepository
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
    /** Confirmaciones por alerta (solo para quien decide). */
    val acks: Map<Int, Map<String, AckStatus>> = emptyMap(),
    val responders: List<Responder> = emptyList(),
)

class AlertsViewModel(private val repo: EchoRepository = EchoRepository) : ViewModel() {
    val ui: StateFlow<AlertsUiState> = combine(repo.alerts, repo.session, repo.emergency, repo.acks) { list, user, e, acks ->
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
            acks = if (user?.role?.has(Permission.VIEW_HAZARD_ALERTS) == true) acks else emptyMap(),
            responders = repo.responders,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertsUiState())

    /** Solo quien puede decidir resuelve o descarta alertas (el repositorio lo valida). */
    fun resolve(id: Int) = repo.resolveAlert(id)

    /** Vuelve a avisar a quienes no confirmaron. Devuelve cuántos eran. */
    fun resend(id: Int): Int = repo.resendPending(id).size
}
