package com.floresvalencia.baymaxecho.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floresvalencia.baymaxecho.data.model.EmergencyMode
import com.floresvalencia.baymaxecho.ui.theme.EchoColors
import com.floresvalencia.baymaxecho.ui.theme.Mono

/**
 * Banner fijo visible en TODAS las pantallas durante una evacuación.
 * Muestra una sola instrucción clara: buscar más información retrasa la salida.
 */
@Composable
fun EmergencyBanner(
    mode: EmergencyMode,
    elapsed: String,
    instruction: String,
    canFinish: Boolean,
    onFinish: () -> Unit,
) {
    val real = mode == EmergencyMode.REAL
    val t = rememberInfiniteTransition(label = "banner")
    val dot by t.animateFloat(1f, 0.2f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "dot")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (real) EchoColors.Red else EchoColors.DrillAmber)
            .statusBarsPadding()
            .padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = dot))
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (real) "EVACUAR AHORA" else "SIMULACRO",
                color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp,
            )
            Text(instruction, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(elapsed, color = Color.White, fontFamily = Mono, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (canFinish) Surface(
                onClick = onFinish,
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.22f),
                modifier = Modifier.heightIn(min = 40.dp).padding(top = 4.dp),
            ) {
                Box(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), contentAlignment = Alignment.Center) {
                    Text("Finalizar", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Elegir tipo de evacuación con dos botones enormes. Sin formularios ni texto largo. */
@Composable
fun StartEmergencyDialog(
    cause: com.floresvalencia.baymaxecho.data.model.HazardType?,
    onDismiss: () -> Unit,
    onStart: (EmergencyMode) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Iniciar evacuación" + (cause?.let { " · ${it.label}" } ?: ""), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Avisa a todos, despliega los rovers disponibles con ruta luminosa y reinicia el pase de lista.",
                    style = MaterialTheme.typography.bodyMedium, color = EchoColors.Muted,
                )
                BigButton(
                    "EMERGENCIA REAL", onClick = { onStart(EmergencyMode.REAL) },
                    icon = Icons.Rounded.Warning, container = EchoColors.Red, height = 72.dp, fontSize = 18f,
                )
                BigButton(
                    "SIMULACRO", onClick = { onStart(EmergencyMode.DRILL) },
                    icon = Icons.Rounded.Timer, container = EchoColors.DrillAmber, height = 64.dp,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", fontSize = 16.sp, color = EchoColors.Ink) }
        },
    )
}

@Composable
fun StopEmergencyDialog(
    real: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        icon = { androidx.compose.material3.Icon(Icons.AutoMirrored.Rounded.DirectionsRun, null, tint = EchoColors.Ink) },
        title = { Text(if (real) "¿Todos evacuados?" else "¿Terminar simulacro?") },
        text = { Text("Se cerrará el modo emergencia y se guardará el tiempo total.") },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = onConfirm,
                modifier = Modifier.heightIn(min = 52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = EchoColors.Navy),
            ) { Text("Finalizar", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Seguir", fontSize = 16.sp, color = EchoColors.Ink) }
        },
    )
}

/**
 * Reporte rápido de un peligro (alumnos, docentes o sin sesión).
 * No inicia la evacuación: crea una alerta crítica para la brigada.
 */
@Composable
fun ReportDialog(
    title: String,
    onDismiss: () -> Unit,
    onReport: (com.floresvalencia.baymaxecho.data.model.HazardType) -> Unit,
) {
    val options = listOf(
        com.floresvalencia.baymaxecho.data.model.HazardType.FIRE,
        com.floresvalencia.baymaxecho.data.model.HazardType.SMOKE,
        com.floresvalencia.baymaxecho.data.model.HazardType.EARTHQUAKE,
        com.floresvalencia.baymaxecho.data.model.HazardType.GAS,
        com.floresvalencia.baymaxecho.data.model.HazardType.INJURED,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("La brigada recibirá el aviso y decidirá si evacuar.", color = EchoColors.Muted, fontSize = 14.sp)
                options.forEach { h ->
                    BigButton(
                        h.label, onClick = { onReport(h) }, height = 54.dp, outlined = true,
                        container = if (h == com.floresvalencia.baymaxecho.data.model.HazardType.INJURED) EchoColors.Blue else EchoColors.Red,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", fontSize = 16.sp, color = EchoColors.Ink) } },
    )
}
