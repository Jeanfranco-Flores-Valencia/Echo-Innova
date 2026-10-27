package com.floresvalencia.baymaxecho.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floresvalencia.baymaxecho.data.model.EmergencyMode
import com.floresvalencia.baymaxecho.ui.theme.EchoColors
import com.floresvalencia.baymaxecho.ui.theme.LocalEmergencyMode

/** Cabecera navy con el círculo decorativo del prototipo. Incluye el padding de la barra de estado. */
@Composable
fun NavyHeader(
    modifier: Modifier = Modifier,
    bottomSpace: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    // En emergencia REAL todas las cabeceras pasan a rojo oscuro: imposible confundirla con un simulacro.
    val alarm = LocalEmergencyMode.current == EmergencyMode.REAL
    val colors = if (alarm) listOf(EchoColors.AlarmDark, EchoColors.AlarmDark2) else listOf(EchoColors.Navy, EchoColors.Navy2)
    val circle = if (alarm) Color(0x40E60012) else Color(0x383B64AA)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .background(Brush.verticalGradient(colors))
            .drawBehind {
                drawCircle(
                    color = circle,
                    radius = 110.dp.toPx(),
                    center = Offset(size.width - 16.dp.toPx(), 12.dp.toPx()),
                )
            }
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 20.dp + bottomSpace),
        content = content,
    )
}

/** Título para las pantallas de fondo claro. */
@Composable
fun ScreenTitle(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = EchoColors.Ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = EchoColors.Muted)
        }
        trailing?.invoke()
    }
}

@Composable
fun IconChip(icon: ImageVector, accent: Color, modifier: Modifier = Modifier, size: Dp = 42.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(size * 0.5f))
    }
}

/** Píldora de estado con punto (opcionalmente parpadeante). */
@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
    pulsing: Boolean = false,
) {
    val dotAlpha = if (pulsing) {
        val t = rememberInfiniteTransition(label = "pill")
        val a by t.animateFloat(
            initialValue = 1f, targetValue = 0.25f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "dot",
        )
        a
    } else 1f
    val textColor = if (onDark) lerp(color, Color.White, 0.45f) else color
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = if (onDark) 0.18f else 0.12f))
            .border(1.dp, color.copy(alpha = 0.45f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = dotAlpha))
        )
        Spacer(Modifier.width(7.dp))
        Text(text, color = textColor, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
    }
}

/** Tarjeta de estado: barra lateral de color + ícono + dato. Tocable si recibe onClick. */
@Composable
fun StatTile(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .drawBehind {
                    drawRoundRect(
                        color = accent,
                        topLeft = Offset(0f, 14.dp.toPx()),
                        size = Size(4.dp.toPx(), size.height - 28.dp.toPx()),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                    )
                }
                .heightIn(min = 72.dp)
                .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconChip(icon, accent, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = EchoColors.Muted)
                Text(
                    value, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = if (accent == EchoColors.RedText) EchoColors.RedText else EchoColors.Ink,
                    maxLines = 1,
                )
            }
        }
    }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 3.dp) { content() }
    } else {
        Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 3.dp) { content() }
    }
}

/** Botón grande (mín. 60 dp) con vibración: bajo estrés se pierde la motricidad fina. */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    container: Color = EchoColors.Navy,
    contentColor: Color = Color.White,
    height: Dp = 60.dp,
    outlined: Boolean = false,
    fontSize: Float = 17f,
) {
    val haptic = LocalHapticFeedback.current
    val click: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onClick()
    }
    val inner: @Composable () -> Unit = {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, fontSize = fontSize.sp, fontWeight = FontWeight.Bold)
    }
    if (outlined) {
        OutlinedButton(
            onClick = click,
            modifier = modifier.fillMaxWidth().height(height),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, container),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = container),
        ) { inner() }
    } else {
        Button(
            onClick = click,
            modifier = modifier.fillMaxWidth().height(height),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = contentColor),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
        ) { inner() }
    }
}

/** Fila grande con interruptor: toda la fila es tocable. */
@Composable
fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Color = EchoColors.AmberRoute,
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.5.dp, if (checked) accent else EchoColors.CardLine),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconChip(icon, if (checked) accent else EchoColors.Soft)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = EchoColors.Ink)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = EchoColors.Muted)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = accent,
                    checkedThumbColor = Color.White,
                    checkedBorderColor = accent,
                ),
            )
        }
    }
}

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = EchoColors.Ink, modifier = Modifier.weight(1f))
        if (action != null) {
            TextButton(onClick = onAction) { Text(action, color = EchoColors.Blue, fontWeight = FontWeight.SemiBold) }
        }
    }
}

/** Barra de aforo por celdas (25 celdas = capacidad total). */
@Composable
fun CapacityCells(filled: Int, color: Color, modifier: Modifier = Modifier, total: Int = 25) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(total) { i ->
            val c by animateColorAsState(if (i < filled) color else EchoColors.CellOff, label = "cell")
            Box(
                Modifier
                    .weight(1f)
                    .height(24.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(c)
            )
        }
    }
}

/** Barra de progreso con degradado azul. */
@Composable
fun GradientBar(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(10.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(Color(0xFF3B82F6), EchoColors.Sky)))
        )
    }
}

/** Indicador pequeño "dato + etiqueta" en una línea. */
@Composable
fun MiniMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF1F5FB))
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Text(label, fontSize = 11.sp, color = EchoColors.Muted)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EchoColors.Ink, maxLines = 1)
    }
}
