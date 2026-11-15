package com.floresvalencia.echoguard.ui.screens.route

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floresvalencia.echoguard.data.map.EvacPath
import com.floresvalencia.echoguard.data.map.MapFeature
import com.floresvalencia.echoguard.data.map.MapLayer
import com.floresvalencia.echoguard.data.map.SchoolMap
import com.floresvalencia.echoguard.data.model.ExitRoute
import com.floresvalencia.echoguard.data.model.MapPoint
import com.floresvalencia.echoguard.data.model.Rover
import com.floresvalencia.echoguard.data.model.RouteStatus
import com.floresvalencia.echoguard.data.repository.EchoRepository
import com.floresvalencia.echoguard.ui.components.BuildingMap
import com.floresvalencia.echoguard.ui.components.EvacuationSign
import com.floresvalencia.echoguard.ui.components.ScreenTitle
import com.floresvalencia.echoguard.ui.components.SectionTitle
import com.floresvalencia.echoguard.ui.components.StatusPill
import com.floresvalencia.echoguard.ui.components.routeColor
import com.floresvalencia.echoguard.ui.theme.EchoColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class MapUiState(
    val routes: List<ExitRoute> = emptyList(),
    val rovers: List<Rover> = emptyList(),
    /** Aula desde donde se calcula la ruta (null = cruce central). */
    val room: String? = null,
    val path: EvacPath? = null,
    val layers: Set<MapLayer> = MapLayer.entries.toSet() - MapLayer.ROVERS,
    val selected: MapFeature? = null,
    val canSeeRovers: Boolean = false,
) {
    val here: MapPoint get() = SchoolMap.pointOf(room)
}

class RouteViewModel(private val repo: EchoRepository = EchoRepository) : ViewModel() {
    private val room = MutableStateFlow(repo.session.value?.classroomId)
    private val layers = MutableStateFlow(MapLayer.entries.toSet() - MapLayer.ROVERS)
    private val selected = MutableStateFlow<MapFeature?>(null)

    val ui: StateFlow<MapUiState> = combine(repo.routes, repo.rovers, repo.session, room, combine(layers, selected) { l, s -> l to s }) { routes, rovers, user, r, (l, sel) ->
        val staff = user?.role?.canSeeFleet == true
        MapUiState(
            routes = routes,
            rovers = if (staff) rovers else emptyList(),
            room = r,
            path = SchoolMap.route(r, routes),
            layers = if (staff) l else l - MapLayer.ROVERS,
            selected = sel,
            canSeeRovers = staff,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState(room = room.value))

    fun setRoom(r: String?) { room.value = r }
    fun toggle(l: MapLayer) = layers.update { if (l in it) it - l else it + l }
    fun select(f: MapFeature?) { selected.value = f }
}

/** Mapa interactivo: salidas, zonas seguras, escaleras y la ruta desde tu aula (evita salidas bloqueadas). */
@Composable
fun RouteScreen(vm: RouteViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val best = ui.path?.exit

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6FA))
            .verticalScroll(rememberScrollState())
    ) {
        ScreenTitle("Mapa de evacuación", "Toca un ícono para saber qué es")
        Column(Modifier.padding(horizontal = 16.dp)) {
            // 1) A dónde ir
            Surface(
                shape = RoundedCornerShape(22.dp), shadowElevation = 6.dp, modifier = Modifier.fillMaxWidth(),
                color = if (best == null) EchoColors.RedText else EchoColors.SafeGreen,
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    EvacuationSign(best?.arrow, size = 64.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("SALIR POR", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                        Text(best?.name ?: "Zona segura", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            best?.let { "${it.path} · ${ui.path?.distanceM} m" } ?: "Todas las salidas bloqueadas",
                            color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp,
                        )
                    }
                }
            }

            // 2) Ruta alternativa: se avisa por qué
            ui.path?.takeIf { it.isAlternative }?.let { p ->
                Spacer(Modifier.height(10.dp))
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFFEF3C7), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Warning, null, tint = EchoColors.DrillAmber)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            p.avoided.joinToString(" · ") { "${it.name} bloqueada" + (it.blockedBy?.let { h -> " por ${h.label.lowercase()}" } ?: "") } +
                                ". Ruta alternativa.",
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF92400E),
                        )
                    }
                }
            }

            // 3) El mapa
            Spacer(Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(22.dp), color = Color.White, border = BorderStroke(1.dp, EchoColors.Line)) {
                Column(Modifier.padding(12.dp)) {
                    BuildingMap(
                        routes = ui.routes, rovers = ui.rovers,
                        path = ui.path?.points, youAreHere = ui.here,
                        features = SchoolMap.features, layers = ui.layers,
                        onFeatureClick = vm::select,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        ui.selected?.let { "${it.kind.label}: ${it.name}" } ?: "Azul = estás aquí · verde = tu ruta · R = punto de reunión",
                        fontSize = 13.sp, color = EchoColors.Muted,
                    )
                }
            }

            // 4) Capas
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MapLayer.entries.filter { it != MapLayer.ROVERS || ui.canSeeRovers }.forEach { l ->
                    FilterChip(selected = l in ui.layers, onClick = { vm.toggle(l) }, label = { Text(l.label) })
                }
            }

            // 5) Desde dónde
            SectionTitle("Estoy en")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = ui.room == null, onClick = { vm.setRoom(null) }, label = { Text("Pasillo") })
                SchoolMap.rooms.keys.forEach { r ->
                    FilterChip(selected = ui.room == r, onClick = { vm.setRoom(r) }, label = { Text(r) })
                }
            }

            // 6) Salidas
            SectionTitle("Salidas")
            ui.routes.forEach { r ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${r.name} · ${r.path}", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = EchoColors.Ink)
                        if (r.status == RouteStatus.BLOCKED && r.blockedBy != null) {
                            Text("Detectado: ${r.blockedBy.label.lowercase()}", fontSize = 12.sp, color = EchoColors.RedText)
                        }
                    }
                    StatusPill(r.status.label, routeColor(r.status))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
