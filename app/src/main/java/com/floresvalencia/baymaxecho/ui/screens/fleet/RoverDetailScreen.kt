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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.WifiOff
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
import com.floresvalencia.baymaxecho.data.model.RoverMode
import com.floresvalencia.baymaxecho.data.model.SensorReadings
import com.floresvalencia.baymaxecho.data.model.formatAgo
import com.floresvalencia.baymaxecho.ui.components.BigButton
import com.floresvalencia.baymaxecho.ui.components.BuildingMap
import com.floresvalencia.baymaxecho.ui.components.MiniMetric
import com.floresvalencia.baymaxecho.ui.components.SectionTitle
import com.floresvalencia.baymaxecho.ui.components.StatusPill
import com.floresvalencia.baymaxecho.ui.components.ToggleRow
import com.floresvalencia.baymaxecho.ui.components.roverColor
import com.floresvalencia.baymaxecho.ui.theme.EchoColors
import com.floresvalencia.baymaxecho.ui.theme.Mono

@Composable
fun RoverDetailScreen(
    onBack: () -> Unit,
    onOpenCameras: () -> Unit,
    onMessage: (String) -> Unit,
    vm: RoverDetailViewModel = viewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val r = ui.rover ?: return
    val station = ui.station

    Column(
        Modifier
            .fillMaxSize()
            .background(EchoColors.BgCool)
            .verticalScroll(rememberScrollState())
    ) {
        // Cabecera clara con volver
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(onClick = onBack, shape = CircleShape, color = Color.Transparent, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", tint = EchoColors.Ink) }
            }
            Column(Modifier.weight(1f)) {
                Text(r.name, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = EchoColors.Ink)
                Text("Base: ${station?.name ?: "—"}", fontSize = 12.sp, color = EchoColors.Muted)
            }
            StatusPill(r.mode.label, if (r.online) roverColor(r.mode) else EchoColors.RedText, pulsing = !r.online || r.mode == RoverMode.LOW_BATTERY)
        }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {

            // ---- Sin señal: última ubicación registrada ----
            if (!r.online) {
                Surface(shape = RoundedCornerShape(22.dp), color = Color(0xFFFEF2F2), border = BorderStroke(2.dp, Color(0xFFFECACA))) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (r.battery == 0) Icons.Rounded.BatteryAlert else Icons.Rounded.LocationOff,
                                null, tint = EchoColors.RedText, modifier = Modifier.size(30.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(if (r.battery == 0) "Sin batería" else "Sin señal", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = EchoColors.RedText)
                                Text("Última ubicación registrada", fontSize = 12.sp, color = Color(0xFF9F1239))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(r.locationLabel, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = EchoColors.Ink)
                        Text(
                            "x ${r.position.x.toInt()} m · y ${r.position.y.toInt()} m · ${formatAgo(ui.now - r.lastSeenMillis)} · batería ${r.battery}%",
                            fontSize = 13.sp, fontFamily = Mono, color = EchoColors.Muted,
                        )
                        if (ui.canControl) {
                            Spacer(Modifier.height(12.dp))
                            BigButton("Marcar como recuperado", onClick = { vm.markRecovered(); onMessage("${r.id} en carga") }, icon = Icons.Rounded.Home, height = 56.dp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ---- Mapa con posición y rango ----
            Surface(shape = RoundedCornerShape(22.dp), color = Color.White, border = BorderStroke(1.5.dp, EchoColors.CardLine)) {
                Column(Modifier.padding(12.dp)) {
                    BuildingMap(routes = ui.routes, rovers = listOf(r), stations = ui.stations, selectedRoverId = r.id)
                    if (station != null) {
                        Spacer(Modifier.height(10.dp))
                        val ratio = ui.distanceToStation.toFloat() / station.rangeM
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("A ${ui.distanceToStation} m de ${station.id}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EchoColors.Ink, modifier = Modifier.weight(1f))
                            Text("Rango ${station.rangeM} m", fontSize = 13.sp, color = EchoColors.Muted)
                        }
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(EchoColors.CellOff)) {
                            Box(
                                Modifier
                                    .fillMaxWidth(ratio.coerceIn(0f, 1f))
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(if (ui.outOfRange) EchoColors.RedText else if (ratio > 0.8f) EchoColors.Amber else EchoColors.Blue)
                            )
                        }
                        if (ui.outOfRange) {
                            Text("Fuera del rango operativo: el enlace puede cortarse.", fontSize = 12.sp, color = EchoColors.RedText, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }

            // ---- Métricas ----
            Spacer(Modifier.height(12.dp))
            Text("Batería", fontSize = 12.sp, color = EchoColors.Muted)
            BatteryBar(r.battery, Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniMetric(if (r.online) "~${r.autonomyMin} min" else "—", "Autonomía", Modifier.weight(1f))
                MiniMetric(if (r.online) "${r.latencyMs} ms" else "Perdido", "Enlace", Modifier.weight(1f))
                MiniMetric(if (r.lightRoute) "Encendida" else "Apagada", "Ruta luz", Modifier.weight(1f))
            }

            // ---- Sensores ----
            SectionTitle("Sensores")
            SensorGrid(r.sensors, stale = !r.online)

            // ---- Control (solo brigadista) ----
            if (ui.canControl && r.online) {
                SectionTitle("Control")
                val deployed = r.mode == RoverMode.PATROLLING || r.mode == RoverMode.LOW_BATTERY
                BigButton(
                    if (deployed) "Recoger a la estación" else "Desplegar",
                    onClick = { vm.toggleDeploy()?.let(onMessage) },
                    icon = if (deployed) Icons.Rounded.Home else Icons.Rounded.PlayArrow,
                    outlined = deployed, height = 62.dp,
                )
                Spacer(Modifier.height(10.dp))
                ToggleRow("Ruta luminosa", if (r.lightRoute) "Proyectando en el suelo" else "Apagada", r.lightRoute, vm::setLight, Icons.Rounded.Lightbulb)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Segment("Cámara frontal", !r.facingBack, Modifier.weight(1f)) { vm.setFacingBack(false) }
                    Segment("Posterior", r.facingBack, Modifier.weight(1f)) { vm.setFacingBack(true) }
                }
                Spacer(Modifier.height(10.dp))
                BigButton("Ver cámaras", onClick = { vm.selectForCameras(); onOpenCameras() }, icon = Icons.Rounded.Videocam, outlined = true, container = EchoColors.Ink)
            } else if (!ui.canControl) {
                Text(
                    "Solo lectura: el control de los rovers lo tiene la brigada.",
                    fontSize = 13.sp, color = EchoColors.Muted, modifier = Modifier.padding(top = 14.dp),
                )
            }

            // ---- Demo ----
            if (ui.canControl) {
                SectionTitle("Simulación (demo)")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (r.online) {
                        BigButton("Agotar batería", onClick = vm::simulateBatteryDeath, icon = Icons.Rounded.BatteryAlert, outlined = true, container = EchoColors.RedText, modifier = Modifier.weight(1f), height = 54.dp, fontSize = 14f)
                    }
                    BigButton(
                        if (r.online) "Cortar señal" else "Restablecer",
                        onClick = vm::toggleLink, icon = if (r.online) Icons.Rounded.WifiOff else Icons.Rounded.Build,
                        outlined = true, container = EchoColors.Ink, modifier = Modifier.weight(1f), height = 54.dp, fontSize = 14f,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SensorGrid(s: SensorReadings, stale: Boolean) {
    val items = listOf(
        Triple("Humo", "${s.smokePpm} ppm", s.smokeAlarm),
        Triple("CO", "${s.coPpm} ppm", s.coAlarm),
        Triple("Temperatura", "${s.temperatureC} °C", s.heatAlarm),
        Triple("Sismo", "${"%.2f".format(s.seismicG)} g", s.seismicAlarm),
        Triple("Polvo", "${s.dustUg} µg/m³", s.dustAlarm),
        Triple("Llama", if (s.flame) "Detectada" else "No", s.flame),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, value, alarm) ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (alarm) Color(0xFFFEE2E2) else Color.White)
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Text(label, fontSize = 11.sp, color = if (alarm) EchoColors.RedText else EchoColors.Muted)
                        Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = if (alarm) EchoColors.RedText else if (stale) EchoColors.Soft else EchoColors.Ink, maxLines = 1)
                    }
                }
            }
        }
        if (stale) Text("Últimos valores recibidos antes de perder la señal.", fontSize = 12.sp, color = EchoColors.Muted)
    }
}

@Composable
private fun Segment(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) EchoColors.Navy else Color.White,
        border = BorderStroke(1.5.dp, if (selected) EchoColors.Navy else EchoColors.CardLine),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) {
            Text(text, color = if (selected) Color.White else EchoColors.Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}
