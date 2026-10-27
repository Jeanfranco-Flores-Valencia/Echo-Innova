package com.floresvalencia.baymaxecho.ui.screens.classroom

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessible
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floresvalencia.baymaxecho.data.model.RollStatus
import com.floresvalencia.baymaxecho.ui.components.BigButton
import com.floresvalencia.baymaxecho.ui.components.IconChip
import com.floresvalencia.baymaxecho.ui.components.ScreenTitle
import com.floresvalencia.baymaxecho.ui.components.SectionTitle
import com.floresvalencia.baymaxecho.ui.components.StatusPill
import com.floresvalencia.baymaxecho.ui.theme.EchoColors

/** Pase de lista del docente en el punto de reunión. Dos toques: "Completa" o "Faltan N". */
@Composable
fun ClassroomScreen(
    onMessage: (String) -> Unit,
    vm: ClassroomViewModel = viewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val room = ui.classroom ?: return
    var missing by rememberSaveable { mutableIntStateOf(if (room.missing > 0) room.missing else 1) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6FA))
            .verticalScroll(rememberScrollState())
    ) {
        ScreenTitle("Mi aula ${room.id}", "Prof. ${room.teacher} · ${room.students} alumnos") {
            when (room.status) {
                RollStatus.COMPLETE -> StatusPill("Completa", EchoColors.GreenDark)
                RollStatus.MISSING -> StatusPill("Faltan ${room.missing}", EchoColors.RedText, pulsing = true)
                RollStatus.PENDING -> StatusPill("Sin reportar", EchoColors.DrillAmber, pulsing = ui.emergencyActive)
            }
        }

        Column(Modifier.padding(horizontal = 16.dp)) {
            if (!ui.emergencyActive) {
                Text(
                    "Durante una evacuación o simulacro, reporta aquí tu lista en el punto de reunión.",
                    fontSize = 14.sp, color = EchoColors.Muted, modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            BigButton(
                "TODOS PRESENTES", onClick = { vm.reportComplete(); onMessage("Aula ${room.id}: completa") },
                icon = Icons.Rounded.CheckCircle, container = EchoColors.SafeGreen, height = 80.dp, fontSize = 19f,
            )

            Spacer(Modifier.height(14.dp))
            Surface(shape = RoundedCornerShape(22.dp), color = Color.White, border = BorderStroke(1.5.dp, EchoColors.CardLine)) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Faltan alumnos", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EchoColors.Ink)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StepButton(Icons.Rounded.Remove) { missing = (missing - 1).coerceAtLeast(1) }
                        Text(
                            "$missing", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = EchoColors.RedText,
                            modifier = Modifier.width(110.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        StepButton(Icons.Rounded.Add) { missing = (missing + 1).coerceAtMost(room.students) }
                    }
                    Spacer(Modifier.height(12.dp))
                    BigButton(
                        "Reportar faltan $missing", onClick = { vm.reportMissing(missing); onMessage("Aula ${room.id}: faltan $missing") },
                        container = EchoColors.RedText, height = 60.dp,
                    )
                }
            }

            if (room.assist.isNotEmpty()) {
                SectionTitle("Requieren asistencia")
                room.assist.forEach { p ->
                    Surface(
                        onClick = { vm.toggleAssist(p.id) },
                        shape = RoundedCornerShape(18.dp),
                        color = if (p.located) EchoColors.GreenSoft else Color.White,
                        border = BorderStroke(1.5.dp, if (p.located) Color(0xFFB7ECD3) else Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconChip(Icons.Rounded.Accessible, if (p.located) EchoColors.GreenDark else EchoColors.RedText)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EchoColors.Ink)
                                Text(p.need, fontSize = 13.sp, color = EchoColors.Muted)
                            }
                            Text(
                                if (p.located) "A salvo" else "Toca al ubicar",
                                fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                color = if (p.located) EchoColors.GreenDark else EchoColors.RedText,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(18.dp), color = Color(0xFFF1F5FB), modifier = Modifier.size(64.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = EchoColors.Ink, modifier = Modifier.size(30.dp)) }
    }
}

