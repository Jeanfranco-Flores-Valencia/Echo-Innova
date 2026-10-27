package com.floresvalencia.baymaxecho.ui.screens.route

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.floresvalencia.baymaxecho.data.model.ExitRoute
import com.floresvalencia.baymaxecho.data.model.recommended
import com.floresvalencia.baymaxecho.data.repository.EchoRepository
import com.floresvalencia.baymaxecho.ui.components.BuildingMap
import com.floresvalencia.baymaxecho.ui.components.EvacuationSign
import com.floresvalencia.baymaxecho.ui.components.ScreenTitle
import com.floresvalencia.baymaxecho.ui.components.SectionTitle
import com.floresvalencia.baymaxecho.ui.components.StatusPill
import com.floresvalencia.baymaxecho.ui.components.routeColor
import com.floresvalencia.baymaxecho.ui.theme.EchoColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RouteViewModel(repo: EchoRepository = EchoRepository) : ViewModel() {
    val routes: StateFlow<List<ExitRoute>> = repo.routes
        .map { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.routes.value)
}

/** Vista de solo lectura para alumnos y docentes: hacia dónde salir. */
@Composable
fun RouteScreen(vm: RouteViewModel = viewModel()) {
    val routes by vm.routes.collectAsStateWithLifecycle()
    val best = routes.recommended()

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6FA))
            .verticalScroll(rememberScrollState())
    ) {
        ScreenTitle("Ruta de evacuación", "Se actualiza si una salida se bloquea")
        Column(Modifier.padding(horizontal = 16.dp)) {
            Surface(shape = RoundedCornerShape(22.dp), color = EchoColors.SafeGreen, shadowElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    EvacuationSign(best?.arrow, size = 84.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("SALIR POR", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                    Text(best?.let { "${it.name} · ${it.path}" } ?: "Quédate en zona segura", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    best?.let { Text("${it.distanceM} m", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(22.dp), color = Color.White, border = BorderStroke(1.dp, EchoColors.Line)) {
                Column(Modifier.padding(12.dp)) {
                    BuildingMap(routes = routes, highlightRoute = best)
                }
            }
            SectionTitle("Estado de las salidas")
            routes.forEach { r ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${r.name} · ${r.path}", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = EchoColors.Ink, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    StatusPill(r.status.label, routeColor(r.status))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
