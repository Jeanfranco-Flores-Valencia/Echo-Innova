package com.floresvalencia.echoguard.ui.screens.sos

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floresvalencia.echoguard.data.sos.SosKind
import com.floresvalencia.echoguard.data.sos.SosStatus
import com.floresvalencia.echoguard.ui.components.BigButton
import com.floresvalencia.echoguard.ui.components.EchoCard
import com.floresvalencia.echoguard.ui.components.IconChip
import com.floresvalencia.echoguard.ui.components.SectionTitle
import com.floresvalencia.echoguard.ui.components.SubScreenHeader
import com.floresvalencia.echoguard.ui.theme.EchoColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val historyTime = DateTimeFormatter.ofPattern("dd/MM HH:mm")

/** Elegir un número de la agenda sin pedir permiso de contactos (acceso temporal al elegido). */
private class PickPhone : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit) =
        Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
    override fun parseResult(resultCode: Int, intent: Intent?): Uri? = if (resultCode == Activity.RESULT_OK) intent?.data else null
}

private fun readPicked(context: Context, uri: Uri): Pair<String, String>? = runCatching {
    context.contentResolver.query(
        uri, arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null,
    )?.use { c -> if (c.moveToFirst()) (c.getString(0) ?: "") to (c.getString(1) ?: "") else null }
}.getOrNull()

@Composable
fun ContactsScreen(onBack: () -> Unit, onMessage: (String) -> Unit, vm: SosViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var relation by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showSos by rememberSaveable { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(PickPhone()) { uri ->
        uri?.let { readPicked(context, it) }?.let { (n, p) ->
            name = n.take(40)
            phone = p
            error = null
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(EchoColors.Bg)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        SubScreenHeader("Contactos SOS", "A quién avisar en una emergencia", onBack)
        Column(Modifier.padding(horizontal = 16.dp)) {
            Surface(shape = RoundedCornerShape(18.dp), color = EchoColors.GreenSoft) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconChip(Icons.Rounded.Sms, EchoColors.GreenDark)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "El SOS se envía por SMS: funciona SIN internet, solo con señal del operador. Incluye tu ubicación GPS, batería, " +
                            "qué te pasa y si hay una evacuación en curso.",
                        fontSize = 13.sp, color = EchoColors.Ink,
                    )
                }
            }

            SectionTitle("Mis contactos (${ui.contacts.size}/${vm.maxContacts})")
            if (ui.contacts.isEmpty()) {
                Text("Todavía no agregaste a nadie.", color = EchoColors.Muted, fontSize = 14.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ui.contacts.forEach { c ->
                    EchoCard {
                        Row(Modifier.padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconChip(Icons.Rounded.Person, EchoColors.Blue, size = 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.name + (c.relation.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = EchoColors.Ink)
                                Text(c.phone, fontSize = 13.sp, color = EchoColors.Muted)
                            }
                            IconButton(onClick = { vm.deleteContact(c.id) }) { Icon(Icons.Rounded.Delete, "Eliminar ${c.name}", tint = EchoColors.Soft) }
                        }
                    }
                }
            }
            if (ui.contacts.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                BigButton("Probar SOS", onClick = { showSos = true }, outlined = true, container = EchoColors.Red, height = 52.dp, fontSize = 15f)
            }

            if (ui.contacts.size < vm.maxContacts) {
                SectionTitle("Agregar contacto")
                EchoCard {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        BigButton(
                            "Elegir de mi agenda", icon = Icons.Rounded.Contacts, outlined = true, container = EchoColors.Blue, height = 50.dp, fontSize = 14f,
                            onClick = { try { picker.launch(Unit) } catch (_: ActivityNotFoundException) { onMessage("No se encontró la agenda") } },
                        )
                        OutlinedTextField(name, { name = it.take(40); error = null }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(
                            phone, { phone = it.take(20); error = null }, label = { Text("Celular (ej. 987 654 321)") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Mamá", "Papá", "Tutor(a)", "Otro").forEach { r ->
                                FilterChip(selected = relation == r, onClick = { relation = if (relation == r) "" else r }, label = { Text(r, fontSize = 12.sp) })
                            }
                        }
                        error?.let { Text(it, color = EchoColors.RedText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                        BigButton("Guardar contacto", height = 52.dp, fontSize = 15f, onClick = {
                            vm.addContact(name, phone, relation) { err ->
                                if (err == null) {
                                    onMessage("Contacto guardado")
                                    name = ""; phone = ""; relation = ""
                                } else error = err
                            }
                        })
                    }
                }
            }

            if (ui.history.isNotEmpty()) {
                SectionTitle("Últimos SOS")
                EchoCard {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ui.history.forEach { h ->
                            val status = runCatching { SosStatus.valueOf(h.status) }.getOrNull()
                            Row {
                                Text(historyTime.format(Instant.ofEpochMilli(h.timeMillis).atZone(ZoneId.systemDefault())),
                                    fontSize = 12.sp, color = EchoColors.Soft, modifier = Modifier.width(78.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(runCatching { SosKind.valueOf(h.kind).label }.getOrDefault(h.kind), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = EchoColors.Ink)
                                    Text("${status?.label ?: h.status} · ${h.recipients}", fontSize = 12.sp,
                                        color = if (status == SosStatus.FAILED) EchoColors.RedText else EchoColors.Muted)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp).heightIn(min = 24.dp))
        }
    }

    if (showSos) SosDialog(onDismiss = { showSos = false }, onManageContacts = { showSos = false }, onMessage = onMessage, vm = vm)
}
