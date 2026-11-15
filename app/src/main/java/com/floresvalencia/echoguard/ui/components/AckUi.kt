package com.floresvalencia.echoguard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floresvalencia.echoguard.data.model.AckStatus
import com.floresvalencia.echoguard.data.model.Responder
import com.floresvalencia.echoguard.data.model.summary
import com.floresvalencia.echoguard.ui.theme.EchoColors

private fun ackColor(s: AckStatus?): Color = when (s) {
    AckStatus.CONFIRMED -> EchoColors.GreenDark
    AckStatus.RECEIVED -> EchoColors.Blue
    else -> EchoColors.DrillAmber
}

/**
 * Quién recibió la alerta y quién confirmó (recomendación del especialista de bomberos).
 * Los pendientes aparecen primero, para saber a quién llamar.
 */
@Composable
fun AckPanel(
    statuses: Map<String, AckStatus>,
    responders: List<Responder>,
    onResend: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val s = statuses.summary()
    var showAll by rememberSaveable { mutableStateOf(false) }
    val people = responders.filter { it.username in statuses }
        .sortedBy { (statuses[it.username]?.ordinal ?: 0) }      // pendientes primero
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(EchoColors.BgCool)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Confirmaron ${s.confirmed} de ${s.total}",
                fontWeight = FontWeight.Bold, fontSize = 15.sp, color = EchoColors.Ink, modifier = Modifier.weight(1f),
            )
            Text(
                if (s.pending == 0) "Todos" else "${s.pending} sin confirmar",
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = if (s.pending == 0) EchoColors.GreenDark else EchoColors.DrillAmber,
            )
        }
        LinearProgressIndicator(
            progress = { if (s.total == 0) 0f else s.confirmed / s.total.toFloat() },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = EchoColors.GreenDark, trackColor = EchoColors.CellOff,
        )
        Text("Recibida por ${s.received} · ${s.total - s.received} aún no la ven", fontSize = 12.sp, color = EchoColors.Muted)
        (if (showAll) people else people.take(3)).forEach { r ->
            val st = statuses[r.username]
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(ackColor(st)))
                Spacer(Modifier.width(8.dp))
                Text(
                    r.name + " · " + r.role.label + (r.classroomId?.let { " $it" } ?: ""),
                    fontSize = 13.sp, color = EchoColors.Ink, modifier = Modifier.weight(1f),
                )
                Text(st?.label ?: "—", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ackColor(st))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (people.size > 3) {
                TextButton(onClick = { showAll = !showAll }) {
                    Text(if (showAll) "Ver menos" else "Ver los ${people.size}", color = EchoColors.Blue)
                }
            }
            Spacer(Modifier.weight(1f))
            if (onResend != null && s.pending > 0) {
                TextButton(onClick = onResend) {
                    androidx.compose.material3.Icon(Icons.Rounded.NotificationsActive, null, tint = EchoColors.RedText, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reenviar a pendientes", color = EchoColors.RedText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
