package com.floresvalencia.echoguard.ui.screens.monitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floresvalencia.echoguard.data.model.Rover
import com.floresvalencia.echoguard.data.repository.EchoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class MonitorUiState(
    val rovers: List<Rover> = emptyList(),
    val selected: Rover? = null,
    val clock: String = "--:--",
    val canControl: Boolean = false,
) {
    val connected: Boolean get() = selected?.online == true
}

class MonitorViewModel(private val repo: EchoRepository = EchoRepository) : ViewModel() {
    private val fmt = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

    val ui: StateFlow<MonitorUiState> = combine(repo.rovers, repo.selectedRoverId, repo.now, repo.session) { list, id, now, user ->
        MonitorUiState(
            rovers = list,
            selected = list.firstOrNull { it.id == id } ?: list.firstOrNull(),
            clock = fmt.format(Instant.ofEpochMilli(now)),
            canControl = user?.role?.canControlRovers == true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonitorUiState())

    fun select(id: String) = repo.selectRover(id)
    fun setLightRoute(on: Boolean) {
        ui.value.selected?.let { repo.setLightRoute(it.id, on) }
    }
}
