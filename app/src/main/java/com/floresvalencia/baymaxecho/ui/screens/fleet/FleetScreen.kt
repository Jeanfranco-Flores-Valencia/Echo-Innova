package com.floresvalencia.baymaxecho.ui.screens.fleet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floresvalencia.baymaxecho.data.model.HazardType
import com.floresvalencia.baymaxecho.data.model.Rover
import com.floresvalencia.baymaxecho.data.model.RoverMode
import com.floresvalencia.baymaxecho.data.model.formatAgo
import com.floresvalencia.baymaxecho.ui.components.BuildingMap
import com.floresvalencia.baymaxecho.ui.components.MapLegend
import com.floresvalencia.baymaxecho.ui.components.NavyHeader
import com.floresvalencia.baymaxecho.ui.components.SectionTitle
import com.floresvalencia.baymaxecho.ui.components.roverColor
import com.floresvalencia.baymaxecho.ui.theme.EchoColors

@Composable
fun FleetScreen(
    onOpenRover: (String) -> Unit,
    onMessage: (String) -> Unit,
    vm: FleetViewModel = viewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val available = ui.rovers.count { it.available }

    Column(
        Modifier
            .fillMaxSize()
            .background(EchoColors.Bg)
            .verticalScroll(rememberScrollState())
    ) {
        NavyHeader(bottomSpace = 28.dp) {
            Text("Flota de rovers", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text("Bloque A · ${ui.stations.size} estaciones", color = EchoColors.SkyMuted, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$available", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold)
                Text(" de ${ui.rovers.size} disponibles", color = EchoColors.SkyText, fontSize = 16.sp, modifier = Modifier.padding(bottom = 9.dp))
            }
        }

        Column(
            Modifier
                .offset(y = (-22).dp)
                .padding(horizontal = 16.dp)
        ) {
            // Resumen por estado
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CountChip("Desplegados", ui.rovers.count { it.mode == RoverMode.PATROLLING || it.mode == RoverMode.LOW_BATTERY }, EchoColors.Green, Modifier.weight(1f))
                CountChip("En estación", ui.rovers.count { it.mode == RoverMode.DOCKED }, EchoColors.Blue, Modifier.weight(1f))
                CountChip("Cargando", ui.rovers.count { it.mode == RoverMode.CHARGING }, EchoColors.Amber, Modifier.weight(1f))
                CountChip("Sin señal", ui.rovers.count { !it.online }, EchoColors.RedText, Modifier.weight(1f))
            }

            // Mapa con rango operativo
            Spacer(Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(22.dp), color = Color.White, shadowElevation = 3.dp) {
                Column(Modifier.padding(12.dp)) {
                    BuildingMap(
                        routes = ui.routes, rovers = ui.rovers, stations = ui.stations,
                        onRoverClick = onOpenRover,
                    )
                    Spacer(Modifier.height(10.dp))
                    MapLegend()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Rango operativo: " + ui.stations.joinToString(" · ") { "${it.id} ${it.rangeM} m" } +
                            " (círculo punteado). Toca un rover para ver su detalle.",
                        fontSize = 12.sp, color = EchoColors.Muted,
                    )
                }
            }

            SectionTitle("Unidades")
            ui.rovers.forEach { r ->
                RoverCard(r, ui.now) { onOpenRover(r.id) }
                Spacer(Modifier.height(8.dp))
            }

            if (ui.canControl) {
                SectionTitle("Simular detección (demo)")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(HazardType.EARTHQUAKE, HazardType.SMOKE, HazardType.GAS, HazardType.FIRE).forEach { h ->
                        Surface(
                            onClick = { vm.simulateHazard(h); onMessage("Simulando: ${h.label}") },
                            shape = RoundedCornerShape(14.dp), color = Color.White,
                            border = BorderStroke(1.5.dp, EchoColors.RedText.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(h.label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EchoColors.RedText, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountChip(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 3.dp) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Text("$count", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = if (count > 0) color else EchoColors.Soft)
            Text(label, fontSize = 10.5.sp, color = EchoColors.Muted, maxLines = 1)
        }
    }
}

@Composable
fun RoverCard(r: Rover, now: Long, onClick: () -> Unit) {
    val color = roverColor(r.mode)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (r.online) Color.White else Color(0xFFFFFAFA),
        border = BorderStroke(1.5.dp, if (r.online) EchoColors.Line else Color(0xFFFECACA)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(r.id, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = EchoColors.Ink)
                    Spacer(Modifier.width(8.dp))
                    Text(r.mode.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (r.online) color else EchoColors.RedText)
                }
                Spacer(Modifier.height(6.dp))
                BatteryBar(r.battery)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocationOn, null, tint = EchoColors.Muted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (r.online) "${r.locationLabel} · ~${r.autonomyMin} min de autonomía"
                        else "Última ubicación: ${r.locationLabel} · ${formatAgo(now - r.lastSeenMillis)}",
                        fontSize = 12.sp, color = if (r.online) EchoColors.Muted else EchoColors.RedText, maxLines = 2,
                    )
                }
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = EchoColors.Soft)
        }
    }
}

@Composable
fun BatteryBar(battery: Int, modifier: Modifier = Modifier) {
    val c = when {
        battery <= 15 -> EchoColors.RedText
        battery <= 35 -> EchoColors.Amber
        else -> EchoColors.Green
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .weight(1f)
                .height(8.dp)
                .clip(CircleShape)
                .background(EchoColors.CellOff)
        ) {
            Box(
                Modifier
                    .fillMaxWidth((battery / 100f).coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(c)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("$battery%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c)
    }
}
