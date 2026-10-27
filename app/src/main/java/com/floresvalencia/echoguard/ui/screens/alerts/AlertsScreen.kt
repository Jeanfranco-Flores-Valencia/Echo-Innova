package com.floresvalencia.echoguard.ui.screens.alerts

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floresvalencia.echoguard.data.model.Alert
import com.floresvalencia.echoguard.data.model.AlertKind
import com.floresvalencia.echoguard.data.model.canBeResolvedBy
import com.floresvalencia.echoguard.data.model.HazardType
import com.floresvalencia.echoguard.ui.components.BigButton
import com.floresvalencia.echoguard.ui.components.IconChip
import com.floresvalencia.echoguard.ui.components.ScreenTitle
import com.floresvalencia.echoguard.ui.components.StatusPill
import com.floresvalencia.echoguard.ui.theme.EchoColors

@Composable
fun AlertsScreen(
    onStartEvacuation: (HazardType?) -> Unit,
    onOpenRover: (String) -> Unit,
    vm: AlertsViewModel = viewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FB))
    ) {
        ScreenTitle(
            "Alertas",
            if (ui.active == 1) "1 alerta activa" else "${ui.active} alertas activas",
        )
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (ui.critical > 0) StatusPill(if (ui.critical == 1) "1 crítica" else "${ui.critical} críticas", EchoColors.RedText, pulsing = true)
            else StatusPill("Sin alertas críticas", EchoColors.GreenDark)
            StatusPill("${ui.alerts.size} registros", EchoColors.Muted)
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(ui.alerts, key = { it.id }) { alert ->
                val decision = alert.active && (alert.kind == AlertKind.HAZARD || alert.kind == AlertKind.REQUEST)
                AlertCard(
                    alert,
                    canResolve = alert.canBeResolvedBy(ui.role),
                    canEvacuate = decision && ui.role.canStartEvacuation && !ui.emergencyActive,
                    onResolve = { vm.resolve(alert.id) },
                    onEvacuate = { onStartEvacuation(alert.hazard) },
                    onOpenRover = if (ui.role.canSeeFleet) alert.roverId?.let { id -> { onOpenRover(id) } } else null,
                )
            }
        }
    }
}

@Composable
private fun AlertCard(
    alert: Alert,
    canResolve: Boolean,
    canEvacuate: Boolean,
    onResolve: () -> Unit,
    onEvacuate: () -> Unit,
    onOpenRover: (() -> Unit)?,
) {
    // Las alertas críticas activas se muestran abiertas: la acción debe estar a un toque.
    var open by rememberSaveable(alert.id, alert.active) { mutableStateOf(alert.active && alert.critical) }
    val critical = alert.active && alert.critical
    val accent = when {
        critical -> EchoColors.RedText
        alert.active -> EchoColors.Blue
        else -> EchoColors.Soft
    }
    val chevron by animateFloatAsState(if (open) 180f else 0f, label = "chev")

    Surface(
        onClick = { open = !open },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        color = if (critical) Color(0xFFFFFAFA) else Color.White,
        border = BorderStroke(if (critical) 2.dp else 1.5.dp, if (critical) Color(0xFFFECACA) else Color(0xFFE1E8F2)),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconChip(
                    when {
                        critical -> Icons.Rounded.Warning
                        alert.active -> Icons.Rounded.Info
                        else -> Icons.Rounded.CheckCircle
                    },
                    accent,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(alert.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EchoColors.Ink)
                    Text("${alert.source} · ${alert.time}", fontSize = 12.sp, color = EchoColors.Muted)
                }
                Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = EchoColors.Ink, modifier = Modifier.rotate(chevron))
            }
            if (open) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFDBE3EF))
                Spacer(Modifier.height(12.dp))
                Text(alert.detail, fontSize = 15.sp, color = Color(0xFF475569))
                if (alert.active) {
                    Spacer(Modifier.height(12.dp))
                    if (canEvacuate) {
                        BigButton("Iniciar evacuación", onClick = onEvacuate, height = 56.dp, icon = Icons.Rounded.Warning, container = EchoColors.Red)
                        Spacer(Modifier.height(8.dp))
                    }
                    if (onOpenRover != null) {
                        BigButton("Ver rover", onClick = onOpenRover, height = 52.dp, outlined = true, container = EchoColors.Ink)
                        Spacer(Modifier.height(8.dp))
                    }
                    if (canResolve) {
                        BigButton(
                            if (canEvacuate) "Descartar (falsa alarma)" else "Marcar como resuelta",
                            onClick = onResolve, height = 52.dp, icon = Icons.Rounded.CheckCircle, outlined = canEvacuate,
                        )
                    } else if (alert.kind != AlertKind.EVACUATION) {
                        Text("La brigada está evaluando tu aviso.", fontSize = 13.sp, color = EchoColors.DrillAmber, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Text("Resuelta", fontSize = 13.sp, color = EchoColors.GreenDark, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}
