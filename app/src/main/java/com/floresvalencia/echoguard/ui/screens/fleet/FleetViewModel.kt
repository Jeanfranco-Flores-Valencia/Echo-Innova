package com.floresvalencia.echoguard.ui.screens.fleet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floresvalencia.echoguard.data.model.ExitRoute
import com.floresvalencia.echoguard.data.model.HazardType
import com.floresvalencia.echoguard.data.model.Rover
import com.floresvalencia.echoguard.data.model.Station
import com.floresvalencia.echoguard.data.model.distanceTo
import com.floresvalencia.echoguard.data.repository.EchoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class FleetUiState(
    val rovers: List<Rover> = emptyList(),
    val stations: List<Station> = emptyList(),
    val routes: List<ExitRoute> = emptyList(),
    val now: Long = System.currentTimeMillis(),
    val canControl: Boolean = false,
)

class FleetViewModel(private val repo: EchoRepository = EchoRepository) : ViewModel() {
    val ui: StateFlow<FleetUiState> = combine(repo.rovers, repo.routes, repo.now, repo.session) { r, routes, now, user ->
        FleetUiState(r, repo.stations, routes, now, user?.role?.canControlRovers == true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FleetUiState(stations = repo.stations))

    fun simulateHazard(type: HazardType) = repo.simulateHazard(type)
}

data class RoverDetailUiState(
    val rover: Rover? = null,
    val station: Station? = null,
    val allRovers: List<Rover> = emptyList(),
    val stations: List<Station> = emptyList(),
    val routes: List<ExitRoute> = emptyList(),
    val now: Long = System.currentTimeMillis(),
    val canControl: Boolean = false,
) {
    val distanceToStation: Int get() = if (rover != null && station != null) rover.position.distanceTo(station.position).toInt() else 0
    val outOfRange: Boolean get() = station != null && distanceToStation > station.rangeM
}

class RoverDetailViewModel(savedState: SavedStateHandle) : ViewModel() {
    private val repo = EchoRepository
    val roverId: String = savedState.get<String>("id") ?: "R-01"

    val ui: StateFlow<RoverDetailUiState> = combine(repo.rovers, repo.routes, repo.now, repo.session) { list, routes, now, user ->
        val r = list.firstOrNull { it.id == roverId }
        RoverDetailUiState(
            rover = r,
            station = r?.let { repo.stationOf(it) },
            allRovers = list,
            stations = repo.stations,
            routes = routes,
            now = now,
            canControl = user?.role?.canControlRovers == true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoverDetailUiState())

    fun toggleDeploy(): String? = repo.toggleDeploy(roverId)
    fun setLight(on: Boolean) = repo.setLightRoute(roverId, on)
    fun setFacingBack(back: Boolean) = repo.setFacingBack(roverId, back)
    fun simulateBatteryDeath() = repo.simulateBatteryDeath(roverId)
    fun toggleLink() = repo.toggleLink(roverId)
    fun markRecovered() = repo.markRecovered(roverId)
    fun selectForCameras() = repo.selectRover(roverId)
}
