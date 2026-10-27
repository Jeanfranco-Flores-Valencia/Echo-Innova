package com.floresvalencia.echoguard.ui.screens.permissions

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floresvalencia.echoguard.data.model.Permission
import com.floresvalencia.echoguard.data.model.PermissionGroup
import com.floresvalencia.echoguard.data.model.Role
import com.floresvalencia.echoguard.data.model.StaffAccount
import com.floresvalencia.echoguard.ui.components.BigButton
import com.floresvalencia.echoguard.ui.components.EchoCard
import com.floresvalencia.echoguard.ui.components.SectionTitle
import com.floresvalencia.echoguard.ui.components.StatusPill
import com.floresvalencia.echoguard.ui.components.SubScreenHeader
import com.floresvalencia.echoguard.ui.theme.EchoColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val auditTime = DateTimeFormatter.ofPattern("dd/MM HH:mm")

/** Qué puede hacer cada rol, administración del personal y registro de acciones. */
@Composable
fun PermissionsScreen(onBack: () -> Unit, onMessage: (String) -> Unit, vm: PermissionsViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var creating by rememberSaveable { mutableStateOf(false) }
    var pinFor by remember { mutableStateOf<StaffAccount?>(null) }
    var showMatrix by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(EchoColors.Bg)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        SubScreenHeader("Roles y permisos", "Qué puede hacer cada persona en Echo Guard", onBack)
        Column(Modifier.padding(horizontal = 16.dp)) {
            // ---- Tu rol
            EchoCard {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Tu rol", fontSize = 12.sp, color = EchoColors.Muted)
                            Text(ui.role.label, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = EchoColors.Ink)
                        }
                        StatusPill(if (ui.role.isStaff) "Modo personal" else "Perfil guardado", if (ui.role.isStaff) EchoColors.Purple else EchoColors.Blue)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(ui.role.description, fontSize = 14.sp, color = EchoColors.Muted)
                    if (ui.role.isStaff) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Por seguridad, el modo personal se cierra al salir de la app o tras 10 min sin uso (nunca durante una evacuación).",
                            fontSize = 12.sp, color = EchoColors.Soft,
                        )
                    }
                }
            }

            PermissionGroup.entries.forEach { group ->
                val perms = Permission.entries.filter { it.group == group }
                SectionTitle(group.label)
                EchoCard {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        perms.forEach { p -> PermissionRow(p, allowed = ui.role.has(p)) }
                    }
                }
            }

            SectionTitle("Todos los roles", action = if (showMatrix) "Ocultar" else "Ver", onAction = { showMatrix = !showMatrix })
            if (showMatrix) RoleMatrix()

            // ---- Personal (dirección)
            if (ui.canManage) {
                SectionTitle("Personal con PIN")
                EchoCard {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        ui.accounts.forEach { acc ->
                            AccountRow(
                                acc,
                                isMe = acc.username == ui.user?.username,
                                onToggle = { on -> vm.setActive(acc.username, on) { it?.let(onMessage) } },
                                onChangePin = { pinFor = acc },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                BigButton("Agregar persona", onClick = { creating = true }, icon = Icons.Rounded.Add, height = 54.dp, fontSize = 15f)
            }

            // ---- Auditoría
            if (ui.canAudit) {
                SectionTitle("Registro de acciones")
                EchoCard {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (ui.audit.isEmpty()) Text("Aún no hay acciones registradas.", color = EchoColors.Muted, fontSize = 14.sp)
                        ui.audit.forEach { a ->
                            Row {
                                Text(
                                    auditTime.format(Instant.ofEpochMilli(a.timeMillis).atZone(ZoneId.systemDefault())),
                                    fontSize = 12.sp, color = EchoColors.Soft, modifier = Modifier.width(78.dp),
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(a.action, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = EchoColors.Ink)
                                    Text(
                                        listOf("${a.actor} (${a.role})", a.detail).filter { it.isNotBlank() }.joinToString(" · "),
                                        fontSize = 12.sp, color = EchoColors.Muted,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (creating) {
        CreateAccountDialog(
            classrooms = vm.classrooms,
            onDismiss = { creating = false },
            onCreate = { name, role, room, pin, setError ->
                vm.create(name, role, room, pin) { err ->
                    if (err == null) {
                        creating = false
                        onMessage("Cuenta creada · $name")
                    } else setError(err)
                }
            },
        )
    }
    pinFor?.let { acc ->
        PinDialog(
            title = "Nuevo PIN · ${acc.name}",
            onDismiss = { pinFor = null },
            onSave = { pin, setError ->
                vm.changePin(acc.username, pin) { err ->
                    if (err == null) {
                        pinFor = null
                        onMessage("PIN actualizado")
                    } else setError(err)
                }
            },
        )
    }
}

@Composable
private fun PermissionRow(p: Permission, allowed: Boolean) {
    val who = Role.entries.filter { it.has(p) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .semantics { contentDescription = "${p.label}: ${if (allowed) "permitido" else "no permitido"}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (allowed) EchoColors.GreenSoft else EchoColors.BgCool),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (allowed) Icons.Rounded.Check else Icons.Rounded.Close, null,
                tint = if (allowed) EchoColors.GreenDark else EchoColors.Soft, modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(p.label, fontSize = 14.sp, color = if (allowed) EchoColors.Ink else EchoColors.Muted)
            if (!allowed) {
                Text(
                    if (who.size == Role.entries.size) "Todos" else "Lo hace: " + who.joinToString(", ") { it.label },
                    fontSize = 11.sp, color = EchoColors.Soft,
                )
            }
        }
    }
}

/** Tabla compacta permisos × roles. */
@Composable
private fun RoleMatrix() {
    val roles = Role.entries
    val short = mapOf(Role.BRIGADISTA to "Brig", Role.DIRECTOR to "Dir", Role.DOCENTE to "Doc", Role.ALUMNO to "Alum", Role.GENERAL to "Visit")
    EchoCard {
        Column(Modifier.padding(12.dp)) {
            Row {
                Spacer(Modifier.weight(1f))
                roles.forEach {
                    Text(short.getValue(it), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EchoColors.Muted,
                        textAlign = TextAlign.Center, modifier = Modifier.width(36.dp))
                }
            }
            Permission.entries.forEach { p ->
                Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(p.label, fontSize = 12.sp, color = EchoColors.Ink, modifier = Modifier.weight(1f), lineHeight = 14.sp)
                    roles.forEach { r ->
                        Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                            if (r.has(p)) Icon(Icons.Rounded.Check, "${r.label} sí", tint = EchoColors.GreenDark, modifier = Modifier.size(16.dp))
                            else Text("·", color = EchoColors.Soft)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountRow(acc: StaffAccount, isMe: Boolean, onToggle: (Boolean) -> Unit, onChangePin: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(acc.name + if (isMe) " (tú)" else "", fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                color = if (acc.active) EchoColors.Ink else EchoColors.Soft)
            Text(acc.role.label + (acc.classroomId?.let { " · Aula $it" } ?: "") + if (acc.active) "" else " · Desactivada",
                fontSize = 12.sp, color = EchoColors.Muted)
        }
        TextButton(onClick = onChangePin) { Text("PIN", color = EchoColors.Blue, fontWeight = FontWeight.SemiBold) }
        Switch(checked = acc.active, onCheckedChange = onToggle, enabled = !isMe)
    }
}

@Composable
private fun CreateAccountDialog(
    classrooms: List<String>,
    onDismiss: () -> Unit,
    onCreate: (String, Role, String?, String, (String) -> Unit) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf(Role.BRIGADISTA) }
    var room by rememberSaveable { mutableStateOf<String?>(null) }
    var pin by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Agregar persona") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it.take(40); error = null }, label = { Text("Nombre") }, singleLine = true)
                Text("Rol", fontSize = 13.sp, color = EchoColors.Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Role.staffRoles.forEach { r ->
                        FilterChip(selected = role == r, onClick = { role = r; error = null }, label = { Text(r.label, fontSize = 12.sp) })
                    }
                }
                if (role == Role.DOCENTE) {
                    Text("Aula", fontSize = 13.sp, color = EchoColors.Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        classrooms.forEach { c -> FilterChip(selected = room == c, onClick = { room = c }, label = { Text(c, fontSize = 12.sp) }) }
                    }
                }
                OutlinedTextField(
                    pin, { v -> pin = v.filter { it.isDigit() }.take(4); error = null },
                    label = { Text("PIN de 4 dígitos") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
                error?.let { Text(it, color = EchoColors.RedText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name, role, if (role == Role.DOCENTE) room else null, pin) { error = it } }) {
                Text("Crear", fontWeight = FontWeight.Bold, color = EchoColors.Blue)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = EchoColors.Ink) } },
    )
}

@Composable
private fun PinDialog(title: String, onDismiss: () -> Unit, onSave: (String, (String) -> Unit) -> Unit) {
    var pin by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    pin, { v -> pin = v.filter { it.isDigit() }.take(4); error = null },
                    label = { Text("PIN de 4 dígitos") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
                error?.let { Text(it, color = EchoColors.RedText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(pin) { error = it } }) { Text("Guardar", fontWeight = FontWeight.Bold, color = EchoColors.Blue) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = EchoColors.Ink) } },
    )
}
