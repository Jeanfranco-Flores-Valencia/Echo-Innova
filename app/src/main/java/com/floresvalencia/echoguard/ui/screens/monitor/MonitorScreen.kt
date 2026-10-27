package com.floresvalencia.echoguard.ui.screens.monitor

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floresvalencia.echoguard.data.model.CameraDirection
import com.floresvalencia.echoguard.ui.components.BigButton
import com.floresvalencia.echoguard.ui.components.MiniMetric
import com.floresvalencia.echoguard.ui.components.NavyHeader
import com.floresvalencia.echoguard.ui.components.StatusPill
import com.floresvalencia.echoguard.ui.components.ToggleRow
import com.floresvalencia.echoguard.ui.theme.EchoColors
import com.floresvalencia.echoguard.ui.theme.Mono

/**
 * Las 3 cámaras se ven a la vez al entrar: frontal grande arriba, izquierda y derecha abajo
 * (misma disposición que el rover en el espacio). Tocar una la abre a pantalla completa.
 */
@Composable
fun MonitorScreen(vm: MonitorViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var expanded by rememberSaveable { mutableStateOf<CameraDirection?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(EchoColors.Bg)
            .verticalScroll(rememberScrollState())
    ) {
        NavyHeader(bottomSpace = 24.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Cámaras", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${ui.selected?.id ?: "—"} · ${ui.selected?.locationLabel ?: ""} · ${ui.clock}", color = EchoColors.SkyMuted, fontSize = 12.sp, fontFamily = Mono, maxLines = 1)
                }
                if (ui.connected) StatusPill("EN VIVO", EchoColors.Green, onDark = true, pulsing = true)
                else StatusPill("SIN SEÑAL", EchoColors.RedText, onDark = true, pulsing = true)
            }
        }

        Column(
            Modifier
                .offset(y = (-20).dp)
                .padding(horizontal = 16.dp)
        ) {
            // Selector de rover: solo los que tienen señal se pueden ver en vivo
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                ui.rovers.forEach { r ->
                    val sel = r.id == ui.selected?.id
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (sel) EchoColors.Navy else Color.White)
                            .clickable { vm.select(r.id) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            r.id + if (!r.online) " ✕" else "",
                            fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            color = when {
                                sel -> Color.White
                                !r.online -> EchoColors.Soft
                                else -> EchoColors.Ink
                            },
                        )
                    }
                }
            }
            CameraFeed(
                direction = CameraDirection.FRONT,
                connected = ui.connected,
                big = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f),
                onClick = { expanded = CameraDirection.FRONT },
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CameraFeed(
                    direction = CameraDirection.LEFT,
                    connected = ui.connected,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(4f / 3.4f),
                    onClick = { expanded = CameraDirection.LEFT },
                )
                CameraFeed(
                    direction = CameraDirection.RIGHT,
                    connected = ui.connected,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(4f / 3.4f),
                    onClick = { expanded = CameraDirection.RIGHT },
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniMetric(if (ui.connected) "${ui.selected?.fps ?: 0} FPS" else "0 FPS", "Flujo", Modifier.weight(1f))
                MiniMetric(if (ui.connected) "${ui.selected?.latencyMs ?: 0} ms" else "—", "Latencia", Modifier.weight(1f))
                MiniMetric("${ui.selected?.battery ?: 0}%", "Batería", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            if (ui.canControl && ui.connected) {
                val lit = ui.selected?.lightRoute == true
                ToggleRow(
                    title = "Ruta luminosa",
                    subtitle = if (lit) "Proyectando en el suelo" else "Apagada",
                    checked = lit,
                    onCheckedChange = vm::setLightRoute,
                    icon = Icons.Rounded.Lightbulb,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    expanded?.let { dir ->
        Dialog(
            onDismissRequest = { expanded = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                CameraFeed(
                    direction = dir,
                    connected = ui.connected,
                    big = true,
                    clock = ui.clock,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f),
                    onClick = {},
                )
                Row(
                    Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CameraDirection.entries.forEach { d ->
                        val selected = d == dir
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) Color.White else Color.White.copy(alpha = 0.15f))
                                .clickable { expanded = d }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(d.label, color = if (selected) EchoColors.Ink else Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                BigButton(
                    "Cerrar", onClick = { expanded = null }, icon = Icons.Rounded.Close,
                    container = Color.White, contentColor = EchoColors.Ink,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(16.dp),
                )
            }
        }
    }
}

/**
 * Vista simulada de cámara. Para conectar el stream real del ESP32-CAM (MJPEG),
 * reemplazar el Canvas por un visor de video (p. ej. AndroidView + WebView/ExoPlayer).
 */
@Composable
fun CameraFeed(
    direction: CameraDirection,
    connected: Boolean,
    modifier: Modifier = Modifier,
    big: Boolean = false,
    clock: String? = null,
    onClick: () -> Unit,
) {
    val t = rememberInfiniteTransition(label = "feed")
    val scan by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "scan")
    val blink by t.animateFloat(1f, 0.25f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "blink")

    Box(
        modifier
            .clip(RoundedCornerShape(if (big) 22.dp else 18.dp))
            .background(Color(0xFF050A16))
            .clickable(onClick = onClick)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * direction.focusX
            drawRect(Brush.verticalGradient(listOf(Color(0xFF08112A), Color(0xFF03060F))))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x4D7896D2), Color(0x147896D2), Color.Transparent),
                    center = Offset(cx, h / 2),
                    radius = h * 0.8f,
                ),
                radius = h * 0.8f,
                center = Offset(cx, h / 2),
            )
            // Perspectiva del pasillo
            val line = Color(0x4796AFE1)
            drawLine(line, Offset(0f, h), Offset(cx, h * 0.52f), strokeWidth = 1.5f)
            drawLine(line, Offset(w, h), Offset(cx, h * 0.52f), strokeWidth = 1.5f)
            // Mira
            drawLine(line, Offset(0f, h / 2), Offset(w, h / 2), strokeWidth = 1f)
            drawLine(line, Offset(cx, 0f), Offset(cx, h), strokeWidth = 1f)
            // Línea de escaneo
            drawRect(Color.White.copy(alpha = 0.05f), topLeft = Offset(0f, h * scan), size = Size(w, 5.dp.toPx()))
        }

        // Etiqueta de cámara (grande y legible)
        Row(
            Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(CircleShape)
                .background(Color.White)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(EchoColors.RedText.copy(alpha = if (connected) blink else 0.3f))
            )
            Spacer(Modifier.width(6.dp))
            Text(direction.label.uppercase(), color = EchoColors.Ink, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
        Text(
            listOfNotNull(clock, "${direction.degrees}°").joinToString("  "),
            color = Color(0xFFCFE0FF), fontFamily = Mono, fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
        )

        if (!connected) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.VideocamOff, null, tint = Color.White, modifier = Modifier.size(if (big) 36.dp else 26.dp))
                Text("SIN SEÑAL", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = if (big) 16.sp else 12.sp)
            }
        }
    }
}
