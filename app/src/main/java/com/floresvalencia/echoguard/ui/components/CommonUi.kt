package com.floresvalencia.echoguard.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floresvalencia.echoguard.ui.theme.EchoColors
import kotlinx.coroutines.launch

/** Cabecera de las pantallas secundarias (con botón Volver de 48 dp). */
@Composable
fun SubScreenHeader(title: String, subtitle: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 6.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(onClick = onBack, shape = CircleShape, color = Color.Transparent, modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", tint = EchoColors.Ink) }
        }
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = EchoColors.Ink, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = EchoColors.Muted)
        }
    }
}

/**
 * Botón de "mantener presionado" (como el SOS de las apps de seguridad personal):
 * evita activaciones accidentales en el bolsillo. Con TalkBack basta un doble toque.
 */
@Composable
fun HoldToActivateButton(
    label: String,
    sublabel: String,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = EchoColors.Red,
    holdMillis: Int = 1_500,
    diameter: Dp = 132.dp,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val activate by rememberUpdatedState(onActivate)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(diameter + 20.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = "$label. $sublabel"
                    onClick(label) { activate(); true }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val job = scope.launch {
                            progress.animateTo(1f, tween(holdMillis, easing = LinearEasing))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            activate()
                            progress.snapTo(0f)
                        }
                        tryAwaitRelease()
                        if (job.isActive) {
                            job.cancel()
                            scope.launch { progress.animateTo(0f, tween(200)) }
                        }
                    })
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 7.dp.toPx()
                val inset = stroke / 2
                drawArc(
                    color = color.copy(alpha = 0.18f), startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = Offset(inset, inset), size = Size(size.width - stroke, size.height - stroke), style = Stroke(stroke),
                )
                drawArc(
                    color = color, startAngle = -90f, sweepAngle = 360f * progress.value, useCenter = false,
                    topLeft = Offset(inset, inset), size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            Box(
                Modifier
                    .size(diameter)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, letterSpacing = 2.sp)
            }
        }
        Text(sublabel, fontSize = 13.sp, color = EchoColors.Muted, textAlign = TextAlign.Center)
    }
}

/** Mosaico de acceso rápido (2 por fila en Inicio). */
@Composable
fun ToolTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 108.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, EchoColors.Line),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconChip(icon, accent, size = 38.dp)
                Spacer(Modifier.weight(1f))
                badge?.let { StatusPill(it, accent) }
            }
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = EchoColors.Ink, lineHeight = 18.sp)
            Text(subtitle, fontSize = 12.sp, color = EchoColors.Muted, lineHeight = 15.sp)
        }
    }
}

/** Tarjeta blanca de contenido. */
@Composable
fun EchoCard(modifier: Modifier = Modifier, border: Color = EchoColors.Line, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, border),
        content = content,
    )
}
