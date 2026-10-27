package com.floresvalencia.echoguard.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.floresvalencia.echoguard.data.model.ArrowDir
import com.floresvalencia.echoguard.data.model.ExitRoute
import com.floresvalencia.echoguard.data.model.MapPoint
import com.floresvalencia.echoguard.data.model.RouteStatus
import com.floresvalencia.echoguard.data.model.Rover
import com.floresvalencia.echoguard.data.model.RoverMode
import com.floresvalencia.echoguard.data.model.Station
import com.floresvalencia.echoguard.data.model.distanceTo
import com.floresvalencia.echoguard.data.repository.EchoRepository
import com.floresvalencia.echoguard.ui.theme.EchoColors

// =====================================================================
// Señal de evacuación (estilo ISO 7010: verde de seguridad, figura corriendo y flecha)
// =====================================================================

@Composable
fun EvacuationSign(arrow: ArrowDir?, modifier: Modifier = Modifier, size: Dp = 64.dp) {
    Box(
        modifier = modifier
            .size(width = size * 1.6f, height = size)
            .clip(RoundedCornerShape(size * 0.16f))
            .background(EchoColors.SafeGreen)
            .padding(size * 0.06f)
            .border(size * 0.03f, Color.White, RoundedCornerShape(size * 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Rounded.DirectionsRun, null, tint = Color.White, modifier = Modifier.size(size * 0.62f))
            if (arrow != null) {
                Icon(
                    when (arrow) {
                        ArrowDir.RIGHT -> Icons.AutoMirrored.Rounded.ArrowForward
                        ArrowDir.LEFT -> Icons.AutoMirrored.Rounded.ArrowBack
                        ArrowDir.UP -> Icons.Rounded.ArrowUpward
                        ArrowDir.DOWN -> Icons.Rounded.ArrowDownward
                    },
                    null, tint = Color.White, modifier = Modifier.size(size * 0.52f),
                )
            }
        }
    }
}

fun routeColor(status: RouteStatus): Color = when (status) {
    RouteStatus.FREE -> EchoColors.SafeGreen
    RouteStatus.CONGESTED -> EchoColors.Amber
    RouteStatus.BLOCKED -> Color(0xFFEF4444)
}

fun roverColor(mode: RoverMode): Color = when (mode) {
    RoverMode.PATROLLING -> EchoColors.Green
    RoverMode.DOCKED -> EchoColors.Blue
    RoverMode.CHARGING -> EchoColors.Amber
    RoverMode.LOW_BATTERY -> Color(0xFFEA580C)
    RoverMode.OFFLINE -> EchoColors.Soft
}

// =====================================================================
// Plano del edificio: estaciones con rango operativo, rovers (o su última ubicación) y salidas
// =====================================================================

@Composable
fun BuildingMap(
    routes: List<ExitRoute>,
    modifier: Modifier = Modifier,
    rovers: List<Rover> = emptyList(),
    stations: List<Station> = emptyList(),
    selectedRoverId: String? = null,
    highlightRoute: ExitRoute? = null,
    onRoverClick: ((String) -> Unit)? = null,
) {
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = Color(0xFF7A8BA6), fontWeight = FontWeight.SemiBold)
    val tagStyle = TextStyle(fontSize = 10.sp, color = EchoColors.Ink, fontWeight = FontWeight.Bold)
    val pulse = rememberInfiniteTransition(label = "map")
    val ring by pulse.animateFloat(0.6f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "ring")

    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(EchoRepository.FLOOR_W / EchoRepository.FLOOR_H)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF7F9FC))
            .pointerInput(rovers, onRoverClick) {
                if (onRoverClick == null) return@pointerInput
                detectTapGestures { tap ->
                    val k = size.width / EchoRepository.FLOOR_W
                    val hit = rovers.minByOrNull { (Offset(it.position.x * k, it.position.y * k) - tap).getDistance() }
                    if (hit != null && (Offset(hit.position.x * k, hit.position.y * k) - tap).getDistance() < 32.dp.toPx()) {
                        onRoverClick(hit.id)
                    }
                }
            }
    ) {
        val k = size.width / EchoRepository.FLOOR_W
        fun p(m: MapPoint) = Offset(m.x * k, m.y * k)
        fun pt(x: Float, y: Float) = Offset(x * k, y * k)

        // Pasillos
        val corridor = Color(0xFFE3EBF6)
        drawRect(corridor, topLeft = pt(0f, 14f), size = Size(size.width, 6f * k))
        drawRect(corridor, topLeft = pt(27f, 0f), size = Size(6f * k, size.height))
        // Divisiones de aulas
        val wall = Color(0xFFC7D3E3)
        drawLine(wall, pt(14f, 0f), pt(14f, 14f), strokeWidth = 2f)
        drawLine(wall, pt(46f, 0f), pt(46f, 14f), strokeWidth = 2f)
        drawLine(wall, pt(14f, 20f), pt(14f, 34f), strokeWidth = 2f)
        drawRoundRect(Color(0xFFB8C6DA), size = size, cornerRadius = CornerRadius(16.dp.toPx()), style = Stroke(2.dp.toPx()))
        listOf("3A" to pt(5f, 5f), "3B" to pt(19f, 5f), "3C" to pt(36f, 5f), "LAB" to pt(51f, 5f),
            "4A" to pt(5f, 27f), "DIR" to pt(19f, 30f), "PATIO" to pt(42f, 26f)).forEach { (t, o) ->
            drawText(measurer, t, topLeft = o, style = labelStyle)
        }

        // Ruta resaltada (desde el cruce central hasta la salida)
        if (highlightRoute != null) {
            drawLine(
                EchoColors.SafeGreen.copy(alpha = 0.85f), pt(30f, 17f), p(highlightRoute.position),
                strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f)),
            )
        }

        // Estaciones + rango operativo
        stations.forEach { s ->
            val c = p(s.position)
            drawCircle(EchoColors.Blue.copy(alpha = 0.06f), radius = s.rangeM * k, center = c)
            drawCircle(
                EchoColors.Blue.copy(alpha = 0.35f), radius = s.rangeM * k, center = c,
                style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
            )
            val half = 7.dp.toPx()
            drawRoundRect(EchoColors.Navy, topLeft = c - Offset(half, half), size = Size(half * 2, half * 2), cornerRadius = CornerRadius(3.dp.toPx()))
            drawText(measurer, s.id, topLeft = c + Offset(-half, half + 2.dp.toPx()), style = tagStyle)
        }

        // Salidas
        routes.forEach { r ->
            val c = p(r.position)
            drawCircle(Color.White, radius = 12.dp.toPx(), center = c)
            drawCircle(routeColor(r.status), radius = 10.dp.toPx(), center = c)
            drawCenteredText(measurer, r.id, c, TextStyle(fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.ExtraBold))
        }

        // Rovers
        rovers.forEach { r ->
            val c = p(r.position)
            val station = stations.firstOrNull { it.id == r.homeStationId }
            val outOfRange = station != null && r.position.distanceTo(station.position) > station.rangeM
            if (r.id == selectedRoverId) {
                drawCircle(EchoColors.Blue.copy(alpha = 0.25f), radius = 18.dp.toPx() * ring, center = c)
            }
            if (r.online) {
                drawCircle(Color.White, radius = 10.dp.toPx(), center = c)
                drawCircle(roverColor(r.mode), radius = 8.dp.toPx(), center = c)
                if (outOfRange) drawCircle(Color(0xFFEF4444), radius = 12.dp.toPx(), center = c, style = Stroke(2.dp.toPx()))
            } else {
                // Última ubicación conocida: marcador "fantasma" punteado
                drawCircle(Color.White.copy(alpha = 0.9f), radius = 10.dp.toPx(), center = c)
                drawCircle(
                    Color(0xFF64748B), radius = 9.dp.toPx(), center = c,
                    style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))),
                )
                drawCenteredText(measurer, "?", c, TextStyle(fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.ExtraBold))
            }
            drawText(measurer, r.id, topLeft = c + Offset(-12.dp.toPx(), -24.dp.toPx()), style = tagStyle)
        }
    }
}

private fun DrawScope.drawCenteredText(
    measurer: androidx.compose.ui.text.TextMeasurer,
    text: String,
    center: Offset,
    style: TextStyle,
) {
    val layout = measurer.measure(text, style)
    drawText(layout, topLeft = center - Offset(layout.size.width / 2f, layout.size.height / 2f))
}

/** Leyenda compacta del mapa. */
@Composable
fun MapLegend(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LegendDot(EchoColors.Green, "Desplegado")
        LegendDot(EchoColors.Blue, "Estación")
        LegendDot(EchoColors.Amber, "Cargando")
        LegendDot(EchoColors.Soft, "Última ubicación")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(RoundedCornerShape(5.dp)).background(color))
        Spacer(Modifier.size(4.dp))
        Text(label, fontSize = 10.5.sp, color = EchoColors.Muted)
    }
}

// =====================================================================
// Alarma a pantalla completa
// =====================================================================

/**
 * Se muestra a TODOS los roles cuando llega algo crítico. El contenido y los botones
 * cambian según el rol: solo quien tiene permiso ve "Iniciar evacuación".
 */
@Composable
fun AlarmOverlay(
    title: String,
    location: String,
    instruction: String,
    primaryText: String?,
    onPrimary: () -> Unit,
    dismissText: String,
    onDismiss: () -> Unit,
    sign: ArrowDir? = null,
    signText: String? = null,
) {
    val t = rememberInfiniteTransition(label = "alarm")
    val pulse by t.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(550), RepeatMode.Reverse), label = "pulse")

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(EchoColors.Red)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.Warning, null, tint = Color.White, modifier = Modifier.size(96.dp).scale(pulse))
            Spacer(Modifier.height(18.dp))
            Text(title.uppercase(), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 36.sp)
            Spacer(Modifier.height(8.dp))
            Text(location, color = Color.White.copy(alpha = 0.9f), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            if (sign != null || signText != null) {
                Spacer(Modifier.height(20.dp))
                EvacuationSign(sign, size = 72.dp)
                if (signText != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(signText, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.22f))
                    .padding(16.dp),
            ) {
                Text(instruction, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.weight(1f))
            if (primaryText != null) {
                BigButton(primaryText, onClick = onPrimary, container = Color.White, contentColor = EchoColors.Red, height = 72.dp, fontSize = 19f)
                Spacer(Modifier.height(12.dp))
            }
            BigButton(dismissText, onClick = onDismiss, container = Color.Black.copy(alpha = 0.25f), height = 60.dp)
        }
    }
}
