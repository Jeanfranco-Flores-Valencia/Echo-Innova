package com.floresvalencia.echoguard.ui.screens.evacuation

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Accessible
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floresvalencia.echoguard.data.model.Classroom
import com.floresvalencia.echoguard.data.model.ExitRoute
import com.floresvalencia.echoguard.data.model.RollStatus
import com.floresvalencia.echoguard.data.model.OccupancyLevel
import com.floresvalencia.echoguard.data.model.RouteStatus
import com.floresvalencia.echoguard.data.model.SafeMark
import com.floresvalencia.echoguard.ui.components.CapacityCells
import com.floresvalencia.echoguard.ui.components.EvacuationSign
import com.floresvalencia.echoguard.ui.components.IconChip
import com.floresvalencia.echoguard.ui.components.ScreenTitle
import com.floresvalencia.echoguard.ui.components.SectionTitle
import com.floresvalencia.echoguard.ui.components.StatusPill
import com.floresvalencia.echoguard.ui.theme.EchoColors
import com.floresvalencia.echoguard.ui.theme.Mono
import kotlin.math.roundToInt

@Composable
fun EvacuationScreen(
    onMessage: (String) -> Unit,
    vm: EvacuationViewModel = viewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val o = ui.occupancy
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(0) }

    val levelColor = when (o.level) {
        OccupancyLevel.FULL -> Color(0xFFEF4444)
        OccupancyLevel.HIGH -> EchoColors.Amber
        OccupancyLevel.NORMAL -> Color(0xFF2F7BFF)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6FA))
            .verticalScroll(rememberScrollState())
    ) {
        ScreenTitle("Evacuación", "Conteo en tiempo real")

        if (ui.canSeeAllClassrooms) {
            val reported = ui.classrooms.count { it.status != RollStatus.PENDING }
            Row(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE8EDF5))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TabButton("Conteo y rutas", tab == 0, Modifier.weight(1f)) { tab = 0 }
                TabButton("Aulas $reported/${ui.classrooms.size}", tab == 1, Modifier.weight(1f)) { tab = 1 }
            }
        }

        if (tab == 1 && ui.canSeeAllClassrooms) {
            SafeList(ui.safe)
            ClassroomBoard(ui.classrooms, onToggleAssist = vm::toggleAssist)
        } else Column(Modifier.padding(horizontal = 16.dp)) {
            // 1) Ruta recomendada primero: es lo que hay que comunicar a la gente.
            RecommendedRoute(ui.recommended)

            // 2) Contador gigante
            Spacer(Modifier.height(16.dp))
            Text("Personas dentro", fontSize = 13.sp, color = EchoColors.Muted, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${o.inside}", fontFamily = Mono, fontSize = 64.sp, fontWeight = FontWeight.Bold,
                    color = EchoColors.Ink, modifier = Modifier.weight(1f),
                )
                when (o.level) {
                    OccupancyLevel.FULL -> StatusPill("Aforo lleno", Color(0xFF991B1B))
                    OccupancyLevel.HIGH -> StatusPill("Aforo alto", Color(0xFF92400E))
                    OccupancyLevel.NORMAL -> StatusPill("Flujo normal", Color(0xFF166534))
                }
            }
            CapacityCells(filled = (o.ratio * 25).roundToInt(), color = levelColor)
            Row(Modifier.padding(top = 6.dp)) {
                Text("0", fontSize = 11.5.sp, color = EchoColors.Muted, modifier = Modifier.weight(1f))
                Text("Máximo ${o.capacity}", fontSize = 11.5.sp, color = EchoColors.Muted)
            }

            // 3) Evacuadas / pendientes
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LedgerItem("Evacuadas", o.evacuated, EchoColors.Green, Modifier.weight(1f))
                LedgerItem("Pendientes", o.pending, EchoColors.Amber, Modifier.weight(1f))
            }

            // 4) Botones enormes de conteo
            SectionTitle("Registrar paso")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CounterButton(
                    label = "Entrada", count = o.entries5m, icon = Icons.Rounded.Add,
                    container = Color.White, accent = EchoColors.Ink, modifier = Modifier.weight(1f),
                ) { if (!vm.registerEntry()) onMessage("Aforo máximo alcanzado") }
                CounterButton(
                    label = "Salida", count = o.exits5m, icon = Icons.Rounded.Remove,
                    container = EchoColors.GreenSoft, accent = EchoColors.GreenDark, modifier = Modifier.weight(1f),
                ) { if (!vm.registerExit()) onMessage("No hay personas dentro") }
            }
            Text(
                "Últimos 5 min",
                fontSize = 11.5.sp, color = EchoColors.Muted,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp),
            )

            // 5) Estado de cada salida (lo reporta la brigada)
            SectionTitle(if (ui.canReportRoutes) "Salidas · toca para reportar" else "Salidas")
            ui.routes.forEach { r ->
                RouteRow(r, recommended = r.id == ui.recommended?.id, enabled = ui.canReportRoutes) { vm.cycleRoute(r.id) }
                Spacer(Modifier.height(8.dp))
            }

            TextButton(
                onClick = { confirmReset = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
            ) { Text("Reiniciar contador", color = EchoColors.Muted, fontSize = 15.sp) }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = Color.White,
            title = { Text("¿Reiniciar contador?") },
            text = { Text("Se perderá el conteo actual.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.reset(); confirmReset = false; onMessage("Contador reiniciado")
                }) { Text("Reiniciar", color = EchoColors.RedText, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancelar", color = EchoColors.Ink) } },
        )
    }
}

@Composable
private fun RecommendedRoute(route: ExitRoute?) {
    val ok = route != null && route.status == RouteStatus.FREE
    val bg = when {
        route == null -> EchoColors.RedSoft
        ok -> EchoColors.SafeGreen
        else -> Color(0xFFFEF3C7)
    }
    val fg = when {
        route == null -> EchoColors.RedText
        ok -> Color.White
        else -> Color(0xFF92400E)
    }
    Surface(shape = RoundedCornerShape(22.dp), color = bg, shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            if (route == null) Icon(Icons.Rounded.Block, null, tint = fg, modifier = Modifier.size(40.dp))
            else EvacuationSign(route.arrow, size = 52.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (route == null) "SIN SALIDA LIBRE" else "SALIR POR",
                    color = fg.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp,
                )
                Text(
                    route?.let { "${it.name} · ${it.path}" } ?: "Esperar indicaciones",
                    color = fg, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold,
                )
                if (route != null) {
                    Text(
                        "${route.distanceM} m · ${route.status.label}",
                        color = fg.copy(alpha = 0.85f), fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun LedgerItem(label: String, value: Int, accent: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconChip(Icons.Rounded.Groups, accent, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 12.sp, color = EchoColors.Muted)
            Text("$value", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = EchoColors.Ink)
        }
    }
}

/** Botón de 120 dp: se acierta aunque tiemble la mano. */
@Composable
private fun CounterButton(
    label: String,
    count: Int,
    icon: ImageVector,
    container: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(22.dp),
        color = container,
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.25f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = accent, modifier = Modifier.weight(1f))
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(26.dp)) }
            }
            Spacer(Modifier.weight(1f))
            Text("$count", fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = accent)
        }
    }
}

@Composable
private fun RouteRow(route: ExitRoute, recommended: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val (color, icon) = when (route.status) {
        RouteStatus.FREE -> EchoColors.SafeGreen to Icons.AutoMirrored.Rounded.DirectionsRun
        RouteStatus.CONGESTED -> EchoColors.Amber to Icons.Rounded.Warning
        RouteStatus.BLOCKED -> Color(0xFFEF4444) to Icons.Rounded.Block
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(if (recommended) 2.dp else 1.5.dp, if (recommended) EchoColors.SafeGreen else Color(0xFFDDE4EF)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconChip(icon, color, size = 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${route.name} · ${route.path}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = EchoColors.Ink)
                Text("${route.distanceM} m", fontSize = 12.sp, color = EchoColors.Muted)
            }
            StatusPill(route.status.label, color)
        }
    }
}


@Composable
private fun TabButton(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color.White else Color.Transparent,
        shadowElevation = if (selected) 2.dp else 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = if (selected) EchoColors.Ink else EchoColors.Muted)
        }
    }
}

/** Tablero de aulas para brigada y dirección: primero quién necesita ayuda, luego qué aulas faltan. */
@Composable
private fun ClassroomBoard(rooms: List<Classroom>, onToggleAssist: (String, Int) -> Unit) {
    val pendingAssist = rooms.flatMap { c -> c.assist.map { c to it } }.sortedBy { it.second.located }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LedgerItem("Faltan", rooms.sumOf { it.missing }, EchoColors.RedText, Modifier.weight(1f))
            LedgerItem("Sin reportar", rooms.count { it.status == RollStatus.PENDING }, EchoColors.Amber, Modifier.weight(1f))
        }
        if (pendingAssist.isNotEmpty()) {
            SectionTitle("Requieren asistencia")
            pendingAssist.forEach { (c, p) ->
                Surface(
                    onClick = { onToggleAssist(c.id, p.id) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (p.located) EchoColors.GreenSoft else Color.White,
                    border = BorderStroke(1.5.dp, if (p.located) Color(0xFFB7ECD3) else Color(0xFFFECACA)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconChip(Icons.Rounded.Accessible, if (p.located) EchoColors.GreenDark else EchoColors.RedText)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name + " · " + c.id, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EchoColors.Ink)
                            Text(p.need + " · piso " + c.floor, fontSize = 13.sp, color = EchoColors.Muted)
                        }
                        Text(if (p.located) "A salvo" else "Ubicar", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (p.located) EchoColors.GreenDark else EchoColors.RedText)
                    }
                }
            }
        }
        SectionTitle("Aulas")
        rooms.sortedBy { it.status != RollStatus.MISSING }.forEach { c ->
            val (color, label) = when (c.status) {
                RollStatus.COMPLETE -> EchoColors.GreenDark to "Completa"
                RollStatus.MISSING -> EchoColors.RedText to "Faltan ${c.missing}"
                RollStatus.PENDING -> EchoColors.DrillAmber to "Sin reportar"
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(c.id, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = EchoColors.Ink, modifier = Modifier.width(56.dp))
                Column(Modifier.weight(1f)) {
                    Text("Prof. ${c.teacher}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = EchoColors.Ink)
                    Text("Piso ${c.floor} · ${c.students} alumnos", fontSize = 12.sp, color = EchoColors.Muted)
                }
                StatusPill(label, color, pulsing = c.status == RollStatus.MISSING)
            }
        }
    }
}

/** Personas que se marcaron "Estoy a salvo" desde su teléfono. */
@Composable
private fun SafeList(safe: List<SafeMark>) {
    Surface(
        shape = RoundedCornerShape(18.dp), color = EchoColors.GreenSoft,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("${safe.size} a salvo según la app", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EchoColors.GreenDark)
            if (safe.isEmpty()) {
                Text("Nadie se ha marcado todavía. Recuérdalo por megáfono: \"Marca Estoy a salvo en Echo Guard\".", fontSize = 13.sp, color = EchoColors.Muted)
            }
            safe.take(12).forEach { m ->
                Text("• ${m.name} · ${m.role.label}" + (m.classroomId?.let { " ($it)" } ?: ""), fontSize = 13.sp, color = EchoColors.Ink)
            }
            if (safe.size > 12) Text("y ${safe.size - 12} más", fontSize = 12.sp, color = EchoColors.Muted)
        }
    }
}
