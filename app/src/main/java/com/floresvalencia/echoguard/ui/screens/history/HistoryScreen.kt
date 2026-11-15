package com.floresvalencia.echoguard.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floresvalencia.echoguard.AppGraph
import com.floresvalencia.echoguard.data.db.EmergencyLogEntity
import com.floresvalencia.echoguard.data.model.EmergencyMode
import com.floresvalencia.echoguard.data.model.HazardType
import com.floresvalencia.echoguard.data.model.Permission
import com.floresvalencia.echoguard.data.model.formatElapsed
import com.floresvalencia.echoguard.data.repository.EchoRepository
import com.floresvalencia.echoguard.ui.components.EchoCard
import com.floresvalencia.echoguard.ui.components.StatusPill
import com.floresvalencia.echoguard.ui.components.SubScreenHeader
import com.floresvalencia.echoguard.ui.theme.EchoColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HistoryViewModel : ViewModel() {
    /** Solo brigada y dirección ven el historial. */
    val items: StateFlow<List<EmergencyLogEntity>?> = combine(EchoRepository.session, AppGraph.history.emergencies) { u, list ->
        if (u?.role?.has(Permission.VIEW_EMERGENCY_LOG) == true) list else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

@Composable
fun HistoryScreen(onBack: () -> Unit, vm: HistoryViewModel = viewModel()) {
    val items by vm.items.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .background(EchoColors.Bg)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        SubScreenHeader("Historial de emergencias", "Cada evacuación y simulacro queda registrado", onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                items == null -> Text(EchoRepository.NO_PERMISSION, color = EchoColors.Muted)
                items!!.isEmpty() -> Text("Aún no hay emergencias registradas. Haz un simulacro para ver el primero.", color = EchoColors.Muted, fontSize = 14.sp)
                else -> items!!.forEach { HistoryCard(it) }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HistoryCard(e: EmergencyLogEntity) {
    val real = e.mode == EmergencyMode.REAL.name
    val cause = e.cause?.let { c -> runCatching { HazardType.valueOf(c).label }.getOrNull() }
    EchoCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text((if (real) "Evacuación real" else "Simulacro") + (cause?.let { " · $it" } ?: ""), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EchoColors.Ink)
                    Text(dateFormat.format(Instant.ofEpochMilli(e.startedAt).atZone(ZoneId.systemDefault())), fontSize = 12.sp, color = EchoColors.Muted)
                }
                StatusPill(formatElapsed(e.endedAt - e.startedAt), if (real) EchoColors.RedText else EchoColors.DrillAmber)
            }
            Text("Salieron ${e.evacuated} de ${e.atAlarm}" + if (e.stillInside > 0) " · ${e.stillInside} sin contar" else " · todos fuera", fontSize = 14.sp, color = EchoColors.Ink)
            Text("${e.markedSafe} se marcaron a salvo · ${e.confirmed} de ${e.responders} responsables confirmaron", fontSize = 13.sp, color = EchoColors.Muted)
            Text("Inició: ${e.startedBy} · Cerró: ${e.endedBy}", fontSize = 12.sp, color = EchoColors.Soft)
        }
    }
}
