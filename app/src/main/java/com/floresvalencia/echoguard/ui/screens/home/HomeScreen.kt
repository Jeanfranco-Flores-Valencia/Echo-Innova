package com.floresvalencia.echoguard.ui.screens.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.ContactPhone
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalPolice
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floresvalencia.echoguard.data.model.Alert
import com.floresvalencia.echoguard.data.model.EmergencyMode
import com.floresvalencia.echoguard.data.model.HazardType
import com.floresvalencia.echoguard.data.model.Role
import com.floresvalencia.echoguard.data.model.RollStatus
import com.floresvalencia.echoguard.ui.components.BigButton
import com.floresvalencia.echoguard.ui.components.EvacuationSign
import com.floresvalencia.echoguard.ui.components.GradientBar
import com.floresvalencia.echoguard.ui.components.HoldToActivateButton
import com.floresvalencia.echoguard.ui.components.ToolTile
import com.floresvalencia.echoguard.ui.screens.sos.SosDialog
import com.floresvalencia.echoguard.ui.components.IconChip
import com.floresvalencia.echoguard.ui.components.NavyHeader
import com.floresvalencia.echoguard.ui.components.ReportDialog
import com.floresvalencia.echoguard.ui.components.SectionTitle
import com.floresvalencia.echoguard.ui.components.StatusPill
import com.floresvalencia.echoguard.ui.navigation.Routes
import com.floresvalencia.echoguard.ui.theme.EchoColors
import com.floresvalencia.echoguard.ui.theme.Mono

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    onStartEvacuation: (HazardType?) -> Unit,
    onStaffAccess: () -> Unit,
    onExitStaff: () -> Unit,
    onClearProfile: () -> Unit,
    onMessage: (String) -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val user = ui.user ?: return
    var showReport by rememberSaveable { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var showSos by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val dial: (String) -> Unit = { n -> context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$n"))) }

    Column(
        Modifier
            .fillMaxSize()
            .background(EchoColors.Bg)
            .verticalScroll(rememberScrollState())
    ) {
        NavyHeader(bottomSpace = 40.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ECHO GUARD", color = EchoColors.Sky, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
                    Text(
                        if (user.role.isStaff) "Modo personal · Bloque A" else "Centro educativo · Bloque A",
                        color = EchoColors.SkyMuted, fontSize = 12.sp,
                    )
                }
                // Perfil regular: botón de acceso de personal. Modo personal: salir y volver al perfil regular.
                Surface(
                    onClick = if (user.role.isStaff) onExitStaff else onStaffAccess,
                    shape = RoundedCornerShape(13.dp),
                    color = Color.White.copy(alpha = 0.10f),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(13.dp)),
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (user.role.isStaff) Icons.AutoMirrored.Rounded.Logout else Icons.Rounded.Badge,
                            null, tint = Color.White, modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (user.role.isStaff) "Salir" else "Personal",
                            color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("Hola,", color = EchoColors.SkyText, fontSize = 13.sp)
                    Text(user.name, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                }
                StatusPill(user.role.label + (user.classroomId?.let { " · $it" } ?: ""), EchoColors.Sky, onDark = true)
            }
        }

        Column(
            Modifier
                .offset(y = (-36).dp)
                .padding(horizontal = 16.dp)
        ) {
            when (user.role) {
                Role.BRIGADISTA, Role.DIRECTOR -> ManagerHome(ui, onNavigate, onStartEvacuation, vm::dismissAlert)
                Role.DOCENTE -> TeacherHome(ui, onNavigate) { showReport = true }
                Role.ALUMNO, Role.GENERAL -> StudentHome(
                    ui,
                    onReport = { showReport = true },
                    onHelp = { vm.report(HazardType.INJURED); onMessage("Pedido de ayuda enviado a la brigada") },
                )
            }

            // "Estoy a salvo": como el Safety Check de las apps de emergencia.
            if (ui.emergency.active) {
                Spacer(Modifier.height(12.dp))
                SafeCheckCard(ui.iAmSafe, ui.safeCount, showCount = user.role.canStartEvacuation) {
                    if (vm.markSafe()) onMessage("Te marcaste a salvo. La brigada lo verá.")
                }
            }

            SectionTitle("SOS a mis contactos")
            SosCard(ui.contacts, onActivate = { showSos = true }, onManage = { onNavigate(Routes.CONTACTS) })

            SectionTitle("Herramientas")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolTile(Icons.AutoMirrored.Rounded.MenuBook, "Guía de emergencias", "Qué hacer en sismo, incendio, gas", EchoColors.DrillAmber,
                    onClick = { onNavigate(Routes.GUIDE) }, modifier = Modifier.weight(1f))
                ToolTile(Icons.Rounded.ContactPhone, "Contactos SOS", "A quién avisar por SMS", EchoColors.Red,
                    onClick = { onNavigate(Routes.CONTACTS) }, modifier = Modifier.weight(1f), badge = if (ui.contacts == 0) "!" else "${ui.contacts}")
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolTile(Icons.Rounded.RateReview, "Probar y opinar", "Haz la prueba y cuéntanos", EchoColors.Blue,
                    onClick = { onNavigate(Routes.FEEDBACK) }, modifier = Modifier.weight(1f))
                ToolTile(Icons.Rounded.AdminPanelSettings, "Roles y permisos", "Qué puede hacer cada rol", EchoColors.Purple,
                    onClick = { onNavigate(Routes.PERMISSIONS) }, modifier = Modifier.weight(1f))
            }

            SectionTitle("Llamada rápida")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CallButton("Bomberos", "116", Icons.Rounded.LocalFireDepartment, EchoColors.Red, Modifier.weight(1f)) { dial("116") }
                CallButton("SAMU", "106", Icons.Rounded.MedicalServices, EchoColors.Cyan, Modifier.weight(1f)) { dial("106") }
                CallButton("Policía", "105", Icons.Rounded.LocalPolice, EchoColors.Blue, Modifier.weight(1f)) { dial("105") }
            }

            if (!user.role.isStaff) {
                TextButton(
                    onClick = { confirmClear = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .heightIn(min = 48.dp),
                ) { Text("Cambiar perfil en este teléfono", color = EchoColors.Muted, fontSize = 14.sp) }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = Color.White,
            title = { Text("¿Cambiar perfil?") },
            text = { Text("Se borrará tu registro de este teléfono y tendrás que registrarte de nuevo.") },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; onClearProfile() }) {
                    Text("Borrar perfil", color = EchoColors.RedText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar", color = EchoColors.Ink) } },
        )
    }

    if (showSos) {
        SosDialog(
            onDismiss = { showSos = false },
            onManageContacts = { showSos = false; onNavigate(Routes.CONTACTS) },
            onMessage = onMessage,
        )
    }

    if (showReport) {
        ReportDialog(
            title = if (user.role == Role.DOCENTE) "Solicitar evacuación" else "Reportar emergencia",
            onDismiss = { showReport = false },
            onReport = { type ->
                vm.report(type)
                showReport = false
                onMessage("Aviso enviado. La brigada decidirá si evacuar.")
            },
        )
    }
}

// ---------------------------------------------------------------------
// Brigadista / Director
// ---------------------------------------------------------------------
@Composable
private fun ManagerHome(
    ui: HomeUiState,
    onNavigate: (String) -> Unit,
    onStartEvacuation: (HazardType?) -> Unit,
    onDismiss: (Int) -> Unit,
) {
    // 1) Lo que exige una decisión, arriba de todo.
    ui.pendingDecisions.firstOrNull()?.let { a ->
        if (!ui.emergency.active) {
            DecisionCard(a, extra = ui.pendingDecisions.size - 1, onStart = { onStartEvacuation(a.hazard) }, onDismiss = { onDismiss(a.id) })
            Spacer(Modifier.height(12.dp))
        }
    }

    // 2) Evacuación
    Surface(shape = RoundedCornerShape(22.dp), color = Color.White, shadowElevation = 8.dp) {
        Column(Modifier.padding(14.dp)) {
            if (!ui.emergency.active) {
                BigButton(
                    "INICIAR EVACUACIÓN", onClick = { onStartEvacuation(null) }, icon = Icons.Rounded.Warning,
                    container = EchoColors.Red, height = 76.dp, fontSize = 19f,
                )
            } else {
                val real = ui.emergency.mode == EmergencyMode.REAL
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (real) "Evacuación en curso" else "Simulacro en curso", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            color = if (real) EchoColors.Red else EchoColors.DrillAmber,
                        )
                        Text("Por ${ui.emergency.startedBy}" + (ui.emergency.cause?.let { " · ${it.label}" } ?: ""), fontSize = 12.sp, color = EchoColors.Muted)
                    }
                    Text(ui.elapsed, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = EchoColors.Ink)
                }
                Text(
                    "${ui.safeCount} persona(s) se marcaron a salvo en la app",
                    fontSize = 13.sp, color = EchoColors.GreenDark, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 4.dp),
                )
                Spacer(Modifier.height(10.dp))
                BigButton("Ir a evacuación", onClick = { onNavigate(Routes.EVACUATION) }, icon = Icons.AutoMirrored.Rounded.DirectionsRun)
            }
        }
    }
    Spacer(Modifier.height(12.dp))

    // 3) Aforo
    Surface(onClick = { onNavigate(Routes.EVACUATION) }, shape = RoundedCornerShape(22.dp), shadowElevation = 6.dp, color = EchoColors.NavyCard) {
        Column(
            Modifier
                .background(Brush.linearGradient(listOf(EchoColors.NavyCard, EchoColors.NavyCard2)))
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("Personas dentro", color = EchoColors.SkyMuted, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${ui.occupancy.inside}", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                        Text(" / ${ui.occupancy.capacity}", color = EchoColors.SkyMuted, fontSize = 15.sp, modifier = Modifier.padding(bottom = 7.dp))
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${ui.occupancy.percent}%", color = EchoColors.Sky, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text("ocupación", color = EchoColors.SkyMuted, fontSize = 11.sp)
                }
            }
            GradientBar(ui.occupancy.ratio, Modifier.padding(vertical = 14.dp))
            Row {
                Text("${ui.occupancy.evacuated} evacuadas", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("${ui.occupancy.pending} pendientes", color = EchoColors.SkyText, fontSize = 13.sp)
            }
        }
    }

    // 4) Flota
    SectionTitle("Flota de rovers")
    LinkCard(
        icon = Icons.Rounded.SmartToy, accent = EchoColors.Blue,
        title = "${ui.fleet.available} de ${ui.fleet.total} disponibles",
        subtitle = listOfNotNull(
            "${ui.fleet.deployed} desplegados",
            if (ui.fleet.charging > 0) "${ui.fleet.charging} cargando" else null,
            if (ui.fleet.offline > 0) "${ui.fleet.offline} sin señal" else null,
        ).joinToString(" · "),
        warning = ui.fleet.offline > 0,
        onClick = { onNavigate(Routes.FLEET) },
    )

    // 5) Aulas
    Spacer(Modifier.height(10.dp))
    LinkCard(
        icon = Icons.Rounded.Groups, accent = EchoColors.Purple,
        title = "Aulas reportadas ${ui.classroomsReported}/${ui.classroomsTotal}",
        subtitle = "Faltan ${ui.missingTotal} · ${ui.assistPending} requieren asistencia",
        warning = ui.missingTotal > 0 || (ui.emergency.active && ui.assistPending > 0),
        onClick = { onNavigate(Routes.EVACUATION) },
    )
}

@Composable
private fun DecisionCard(a: Alert, extra: Int, onStart: () -> Unit, onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp), color = Color.White, shadowElevation = 8.dp,
        border = BorderStroke(2.dp, EchoColors.Red),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconChip(Icons.Rounded.Warning, EchoColors.Red, size = 46.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = EchoColors.Ink)
                    Text("${a.source} · ${a.time}", fontSize = 12.sp, color = EchoColors.Muted)
                }
                if (extra > 0) StatusPill("+$extra", EchoColors.RedText)
            }
            Spacer(Modifier.height(10.dp))
            Text(a.detail, fontSize = 15.sp, color = Color(0xFF475569))
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigButton("Descartar", onClick = onDismiss, outlined = true, container = EchoColors.Ink, modifier = Modifier.weight(1f), height = 56.dp, fontSize = 15f)
                BigButton("Evacuar", onClick = onStart, container = EchoColors.Red, modifier = Modifier.weight(1.4f), height = 56.dp, icon = Icons.Rounded.Warning)
            }
        }
    }
}

// ---------------------------------------------------------------------
// Docente
// ---------------------------------------------------------------------
@Composable
private fun TeacherHome(ui: HomeUiState, onNavigate: (String) -> Unit, onRequest: () -> Unit) {
    StatusHero(ui)
    Spacer(Modifier.height(12.dp))
    val room = ui.myClassroom
    if (room != null) {
        LinkCard(
            icon = Icons.Rounded.Groups,
            accent = when (room.status) {
                RollStatus.COMPLETE -> EchoColors.Green
                RollStatus.MISSING -> EchoColors.RedText
                RollStatus.PENDING -> EchoColors.Amber
            },
            title = "Mi aula ${room.id} · ${room.students} alumnos",
            subtitle = when (room.status) {
                RollStatus.COMPLETE -> "Lista reportada: completa"
                RollStatus.MISSING -> "Reportado: faltan ${room.missing}"
                RollStatus.PENDING -> if (ui.emergency.active) "Pasa lista ahora" else "Sin reportar"
            },
            warning = ui.emergency.active && room.status != RollStatus.COMPLETE,
            onClick = { onNavigate(Routes.CLASSROOM) },
        )
        Spacer(Modifier.height(12.dp))
    }
    if (!ui.emergency.active) {
        BigButton("Solicitar evacuación a la brigada", onClick = onRequest, icon = Icons.Rounded.Campaign, container = EchoColors.Red, outlined = true, height = 60.dp, fontSize = 15.5f)
        if (ui.myActiveReports > 0) PendingNote()
    }
}

// ---------------------------------------------------------------------
// Alumno
// ---------------------------------------------------------------------
@Composable
private fun StudentHome(ui: HomeUiState, onReport: () -> Unit, onHelp: () -> Unit) {
    StatusHero(ui)
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BigButton("Reportar", onClick = onReport, icon = Icons.Rounded.Campaign, container = EchoColors.Red, modifier = Modifier.weight(1f), height = 72.dp)
        BigButton("Necesito ayuda", onClick = onHelp, icon = Icons.Rounded.PanTool, container = EchoColors.Blue, modifier = Modifier.weight(1f), height = 72.dp, fontSize = 15f)
    }
    if (ui.myActiveReports > 0) PendingNote()
    Spacer(Modifier.height(12.dp))
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White, border = BorderStroke(1.dp, EchoColors.Line)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Si hay evacuación", fontWeight = FontWeight.Bold, color = EchoColors.Ink)
            val guide = if (ui.user?.role == Role.GENERAL) "Sigue la señalética y al personal" else "Sigue a tu docente"
            listOf("Camina, no corras", "No regreses por tus cosas", guide).forEach {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = EchoColors.SafeGreen, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(it, fontSize = 16.sp, color = EchoColors.Ink)
                }
            }
        }
    }
}

@Composable
private fun PendingNote() {
    Text(
        "Aviso enviado · esperando a la brigada",
        fontSize = 13.sp, color = EchoColors.DrillAmber, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
    )
}

/** Estado grande para alumno y docente: todo en orden, o la señal de evacuación con la ruta. */
@Composable
private fun StatusHero(ui: HomeUiState) {
    val e = ui.emergency
    if (!e.active) {
        Surface(shape = RoundedCornerShape(22.dp), color = Color.White, shadowElevation = 8.dp) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                IconChip(Icons.Rounded.CheckCircle, EchoColors.Green, size = 52.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Todo en orden", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = EchoColors.Ink)
                    Text("Sin emergencias activas", fontSize = 13.sp, color = EchoColors.Muted)
                }
            }
        }
    } else {
        val real = e.mode == EmergencyMode.REAL
        Surface(shape = RoundedCornerShape(22.dp), color = if (real) EchoColors.Red else EchoColors.DrillAmber, shadowElevation = 8.dp) {
            Column(Modifier.padding(18.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (real) "EVACUAR AHORA" else "SIMULACRO", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                e.cause?.let { Text(it.label, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp) }
                Spacer(Modifier.height(14.dp))
                EvacuationSign(ui.route?.arrow, size = 76.dp)
                Spacer(Modifier.height(10.dp))
                Text(
                    ui.route?.let { "${it.name} · ${it.path}" } ?: "Quédate en zona segura",
                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                )
                e.cause?.let {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.18f))
                            .padding(12.dp)
                    ) { Text(it.instruction, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
@Composable
private fun LinkCard(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    warning: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(if (warning) 2.dp else 1.dp, if (warning) EchoColors.RedText.copy(alpha = 0.5f) else EchoColors.Line),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconChip(icon, accent)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EchoColors.Ink)
                Text(subtitle, fontSize = 13.sp, color = if (warning) EchoColors.RedText else EchoColors.Muted)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = EchoColors.Soft)
        }
    }
}

@Composable
private fun CallButton(label: String, number: String, icon: ImageVector, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 88.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, EchoColors.Line),
    ) {
        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(6.dp))
            Text(number, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = EchoColors.Ink)
            Text(label, fontSize = 12.sp, color = EchoColors.Muted)
        }
    }
}

@Composable
private fun SafeCheckCard(marked: Boolean, count: Int, showCount: Boolean, onMark: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (marked) EchoColors.GreenSoft else Color.White,
        border = BorderStroke(2.dp, EchoColors.SafeGreen.copy(alpha = if (marked) 0.3f else 0.8f)),
        shadowElevation = if (marked) 0.dp else 6.dp,
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            if (marked) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = EchoColors.SafeGreen, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Estás marcado(a) a salvo", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = EchoColors.GreenDark)
                        Text("Quédate en el punto de reunión hasta que te indiquen.", fontSize = 13.sp, color = EchoColors.Muted)
                    }
                }
            } else {
                Text("¿Ya estás en un lugar seguro?", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = EchoColors.Ink)
                Text("Avisa a la brigada con un toque. Ayuda a saber quién falta.", fontSize = 13.sp, color = EchoColors.Muted)
                Spacer(Modifier.height(10.dp))
                BigButton("Estoy a salvo", onClick = onMark, icon = Icons.Rounded.CheckCircle, container = EchoColors.SafeGreen, height = 60.dp)
            }
            if (showCount) {
                Spacer(Modifier.height(8.dp))
                Text("$count persona(s) a salvo hasta ahora", fontSize = 13.sp, color = EchoColors.GreenDark, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** SOS de mantener presionado (evita envíos accidentales). */
@Composable
private fun SosCard(contacts: Int, onActivate: () -> Unit, onManage: () -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = Color.White, border = BorderStroke(1.dp, EchoColors.Line)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            HoldToActivateButton("SOS", "Mantén presionado", onActivate = onActivate, diameter = 96.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (contacts == 0) "Configura tus contactos" else "Avisa a $contacts contacto(s)",
                    fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = EchoColors.Ink,
                )
                Text("Envía tu ubicación, batería y lo que pasa por SMS. Funciona sin internet.", fontSize = 13.sp, color = EchoColors.Muted)
                TextButton(onClick = onManage, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (contacts == 0) "Agregar contactos" else "Editar contactos", color = EchoColors.Blue, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
