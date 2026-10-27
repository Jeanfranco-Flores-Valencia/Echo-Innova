package com.floresvalencia.echoguard.ui.screens.sos

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floresvalencia.echoguard.data.sos.DeviceStatus
import com.floresvalencia.echoguard.data.sos.SosKind
import com.floresvalencia.echoguard.data.sos.SosStatus
import com.floresvalencia.echoguard.ui.components.BigButton
import com.floresvalencia.echoguard.ui.theme.EchoColors

/**
 * SOS a contactos de emergencia, en pocos toques:
 * 1) elegir qué pasa · 2) revisar · 3) enviar. Usa SMS: no necesita internet.
 */
@Composable
fun SosDialog(
    onDismiss: () -> Unit,
    onManageContacts: () -> Unit,
    onMessage: (String) -> Unit,
    vm: SosViewModel = viewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val d = ui.draft
    var askedSms by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { vm.prepare() }
    val smsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.sendDirect { onMessage(it.label) } else vm.sendWithSmsApp(onMessage)
    }

    LaunchedEffect(Unit) {
        vm.reset()
        vm.prepare()
        if (!DeviceStatus.hasLocationPermission(context)) {
            locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    val send: () -> Unit = {
        when {
            vm.canSendDirectly() -> vm.sendDirect { onMessage(it.label) }
            !askedSms -> { askedSms = true; smsPermission.launch(Manifest.permission.SEND_SMS) }
            else -> vm.sendWithSmsApp(onMessage)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF1A0505))
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("SOS", color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold, letterSpacing = 3.sp, fontSize = 13.sp)
                    Text("Avisar a mis contactos", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                }
                Surface(onClick = onDismiss, shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.1f), modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, "Cerrar", tint = Color.White) }
                }
            }
            Spacer(Modifier.height(12.dp))

            if (ui.contacts.isEmpty()) {
                Text("Aún no tienes contactos de emergencia.", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Agrega a tu mamá, papá o tutor para avisarles en segundos, incluso sin internet.", color = Color(0xFFFECACA), fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                BigButton("Agregar contacto", onClick = onManageContacts, container = Color.White, contentColor = EchoColors.Red)
                return@Column
            }

            StatusStrip(d.locating, d.location != null, d.locationPermission, d.battery?.percent)
            Spacer(Modifier.height(16.dp))

            if (d.result != null && d.result != SosStatus.FAILED) {
                Surface(shape = RoundedCornerShape(20.dp), color = EchoColors.SafeGreen) {
                    Column(Modifier.padding(18.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = Color.White, modifier = Modifier.size(48.dp))
                        Text(d.result.label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text(ui.contacts.joinToString(", ") { it.name }, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                BigButton("Listo", onClick = onDismiss, container = Color.White, contentColor = EchoColors.Ink)
                return@Column
            }

            Text("¿Qué está pasando?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            val kinds = listOf(
                Triple(SosKind.SAFE, Icons.Rounded.CheckCircle, EchoColors.SafeGreen),
                Triple(SosKind.HELP, Icons.Rounded.PanTool, EchoColors.Blue),
                Triple(SosKind.INJURED, Icons.Rounded.Healing, EchoColors.Red),
                Triple(SosKind.TRAPPED, Icons.Rounded.Warning, EchoColors.DrillAmber),
            )
            kinds.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 5.dp)) {
                    row.forEach { (k, icon, color) ->
                        KindButton(k.label, icon, color, selected = d.kind == k, modifier = Modifier.weight(1f)) { vm.select(k) }
                    }
                }
            }

            if (d.kind != null) {
                Spacer(Modifier.height(14.dp))
                Text("Mensaje para ${ui.contacts.size} contacto(s)", color = Color(0xFFFECACA), fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.08f)) {
                    Text(d.preview, color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(14.dp).fillMaxWidth())
                }
                if (d.result == SosStatus.FAILED) {
                    Text(
                        "No se pudo enviar (¿sin señal?). Reintenta o usa la app de mensajes.",
                        color = Color(0xFFFCA5A5), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                BigButton(
                    if (d.sending) "Enviando..." else "Enviar SOS", onClick = send, icon = Icons.AutoMirrored.Rounded.Send,
                    container = EchoColors.Red, height = 68.dp, fontSize = 19f,
                )
                TextButton(onClick = { vm.sendWithSmsApp(onMessage) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Abrir en mi app de mensajes", color = Color(0xFFFECACA))
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onManageContacts, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Editar contactos", color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun StatusStrip(locating: Boolean, hasLocation: Boolean, permission: Boolean, battery: Int?) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip(
            icon = if (hasLocation) Icons.Rounded.LocationOn else Icons.Rounded.LocationOff,
            text = when {
                hasLocation -> "Ubicación lista"
                locating -> "Buscando GPS..."
                !permission -> "Sin permiso GPS"
                else -> "Sin ubicación"
            },
            ok = hasLocation, loading = locating && !hasLocation, modifier = Modifier.weight(1.3f),
        )
        Chip(Icons.Rounded.BatteryStd, battery?.let { "$it%" } ?: "—", ok = (battery ?: 0) > 15, modifier = Modifier.weight(0.8f))
        Chip(Icons.Rounded.SignalCellularAlt, "SMS", ok = true, modifier = Modifier.weight(0.8f))
    }
}

@Composable
private fun Chip(icon: ImageVector, text: String, ok: Boolean, modifier: Modifier = Modifier, loading: Boolean = false) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.08f)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (loading) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
            else Icon(icon, null, tint = if (ok) Color(0xFF86EFAC) else Color(0xFFFCA5A5), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun KindButton(label: String, icon: ImageVector, color: Color, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick, modifier = modifier.heightIn(min = 84.dp), shape = RoundedCornerShape(18.dp),
        color = if (selected) color else Color.White.copy(alpha = 0.08f),
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}
