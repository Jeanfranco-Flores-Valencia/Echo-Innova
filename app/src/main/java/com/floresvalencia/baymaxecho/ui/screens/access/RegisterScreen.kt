package com.floresvalencia.baymaxecho.ui.screens.access

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floresvalencia.baymaxecho.ui.components.BigButton
import com.floresvalencia.baymaxecho.ui.components.NavyHeader
import com.floresvalencia.baymaxecho.ui.components.ReportDialog
import com.floresvalencia.baymaxecho.ui.theme.EchoColors

/**
 * Se muestra UNA sola vez. Después de registrarse, la app abre directo en Inicio
 * aunque se cierre y se vuelva a abrir.
 */
@Composable
fun RegisterScreen(
    onStaffAccess: () -> Unit,
    onMessage: (String) -> Unit,
    vm: RegisterViewModel = viewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var showReport by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(EchoColors.Bg)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
    ) {
        NavyHeader {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Shield, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("BAYMAX", color = EchoColors.Sky, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 4.sp)
                    Text("ECHO", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Regístrate una sola vez. No necesitas contraseña.", color = EchoColors.SkyText, style = MaterialTheme.typography.bodyMedium)
        }

        Column(Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
            Text("¿Cómo te llamas?", style = MaterialTheme.typography.titleMedium, color = EchoColors.Ink)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = ui.name,
                onValueChange = vm::onName,
                placeholder = { Text("Tu nombre") },
                leadingIcon = { Icon(Icons.Rounded.Person, null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = EchoColors.Line,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(18.dp))
            Text("Soy…", style = MaterialTheme.typography.titleMedium, color = EchoColors.Ink)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileOption(Icons.Rounded.School, "Alumno", "Del colegio", ui.student == true, Modifier.weight(1f)) { vm.onStudent(true) }
                ProfileOption(Icons.Rounded.Person, "Visitante", "u otra persona", ui.student == false, Modifier.weight(1f)) { vm.onStudent(false) }
            }

            if (ui.student == true) {
                Spacer(Modifier.height(16.dp))
                Text("Tu aula", style = MaterialTheme.typography.titleMedium, color = EchoColors.Ink)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    vm.classrooms.forEach { id ->
                        val sel = ui.classroomId == id
                        Surface(
                            onClick = { vm.onClassroom(id) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (sel) EchoColors.Navy else Color.White,
                            border = BorderStroke(1.5.dp, if (sel) EchoColors.Navy else EchoColors.Line),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(id, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (sel) Color.White else EchoColors.Ink)
                            }
                        }
                    }
                }
            }

            Text(
                ui.error ?: "",
                color = EchoColors.RedText, fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            )
            BigButton(
                "Continuar", onClick = vm::register, height = 60.dp,
                container = if (ui.canContinue) EchoColors.Navy else EchoColors.Soft,
            )

            Spacer(Modifier.height(18.dp))
            BigButton(
                "Reportar emergencia sin registrarme", onClick = { showReport = true },
                icon = Icons.Rounded.Campaign, outlined = true, container = EchoColors.Red, height = 56.dp, fontSize = 15f,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onStaffAccess, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Icon(Icons.Rounded.Badge, null, tint = EchoColors.Blue)
                Spacer(Modifier.width(8.dp))
                Text("Soy docente o brigadista", color = EchoColors.Blue, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }

    if (showReport) {
        ReportDialog(
            title = "¿Qué está pasando?",
            onDismiss = { showReport = false },
            onReport = { type ->
                vm.reportAnonymous(type)
                showReport = false
                onMessage("Aviso enviado a la brigada")
            },
        )
    }
}

@Composable
private fun ProfileOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 108.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) EchoColors.BlueSoft else Color.White,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) EchoColors.Blue else EchoColors.Line),
    ) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = if (selected) EchoColors.Blue else EchoColors.Muted, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EchoColors.Ink)
            Text(subtitle, fontSize = 12.sp, color = EchoColors.Muted)
        }
    }
}
