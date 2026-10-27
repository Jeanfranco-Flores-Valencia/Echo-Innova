package com.floresvalencia.echoguard.ui.screens.feedback

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floresvalencia.echoguard.data.feedback.FeedbackSummary
import com.floresvalencia.echoguard.data.feedback.RatingQuestion
import com.floresvalencia.echoguard.data.feedback.TaskOutcome
import com.floresvalencia.echoguard.data.feedback.UsabilityTask
import com.floresvalencia.echoguard.ui.components.BigButton
import com.floresvalencia.echoguard.ui.components.EchoCard
import com.floresvalencia.echoguard.ui.components.SectionTitle
import com.floresvalencia.echoguard.ui.components.StatusPill
import com.floresvalencia.echoguard.ui.components.SubScreenHeader
import com.floresvalencia.echoguard.ui.theme.EchoColors

/** Prueba guiada: tareas por rol + preguntas cortas + comentarios. Se guarda en la base de datos del teléfono. */
@Composable
fun FeedbackScreen(onBack: () -> Unit, onMessage: (String) -> Unit, vm: FeedbackViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmNext by rememberSaveable { mutableStateOf(false) }
    val f = ui.form

    Column(
        Modifier
            .fillMaxSize()
            .background(EchoColors.Bg)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        SubScreenHeader("Probar y opinar", "Tu opinión hace que Echo Guard sea más segura", onBack)
        Column(Modifier.padding(horizontal = 16.dp)) {
            if (f.sent) {
                ThanksCard(onAnother = vm::newResponse, onNext = { confirmNext = true })
            } else {
                IntroCard(ui.tasks.size)

                SectionTitle("1 · Prueba estas tareas")
                Text("Hazlas en la app y luego marca cómo te fue.", fontSize = 13.sp, color = EchoColors.Muted)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ui.tasks.forEachIndexed { i, t -> TaskCard(i + 1, t, f.outcomes[t.id]) { vm.setOutcome(t.id, it) } }
                }

                SectionTitle("2 · Tu experiencia")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RatingQuestion.entries.forEach { q -> RatingCard(q, f.ratings[q]) { vm.setRating(q, it) } }
                    RecommendCard(f.recommend, vm::setRecommend)
                }

                SectionTitle("3 · Cuéntanos (opcional)")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(f.liked, vm::setLiked, label = { Text("¿Qué fue lo que más te gustó?") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    OutlinedTextField(f.improvements, vm::setImprovements, label = { Text("¿Qué mejorarías?") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    OutlinedTextField(f.bugs, vm::setBugs, label = { Text("¿Algo falló o te confundió?") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                }
                f.error?.let {
                    Text(it, color = EchoColors.RedText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
                }
                Spacer(Modifier.height(14.dp))
                BigButton(if (f.sending) "Guardando..." else "Enviar opinión", onClick = vm::submit, icon = Icons.AutoMirrored.Rounded.Send, container = EchoColors.Blue)
            }

            // ---- Estado de las respuestas en este teléfono
            SectionTitle("Tus respuestas")
            EchoCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (ui.syncEnabled && ui.pendingSync == 0) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff, null,
                            tint = if (ui.syncEnabled && ui.pendingSync == 0) EchoColors.GreenDark else EchoColors.Muted,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            when {
                                !ui.syncEnabled -> "Se guardan en este teléfono (funciona sin internet). Compártelas con el equipo:"
                                ui.pendingSync > 0 -> "${ui.pendingSync} por enviar: se enviarán solas cuando haya internet."
                                else -> "Todas las respuestas llegaron al equipo."
                            },
                            fontSize = 13.sp, color = EchoColors.Ink, modifier = Modifier.weight(1f),
                        )
                    }
                    Text("Enviaste ${ui.myResponses} respuesta(s) desde este teléfono.", fontSize = 13.sp, color = EchoColors.Muted)
                    BigButton(
                        if (ui.canSeeResults) "Exportar todas (CSV)" else "Compartir mis respuestas",
                        onClick = { shareCsv(context, vm.csv()) }, icon = Icons.Rounded.IosShare,
                        outlined = true, container = EchoColors.Blue, height = 50.dp, fontSize = 14f,
                    )
                }
            }

            // ---- Resultados (brigada / dirección)
            ui.summary?.let { s ->
                SectionTitle("Resultados de la prueba")
                ResultsCard(s)
                if (ui.recentComments.isNotEmpty()) {
                    SectionTitle("Comentarios recientes")
                    EchoCard {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ui.recentComments.forEach { c ->
                                Column {
                                    Text("${c.userName} · ${runCatching { com.floresvalencia.echoguard.data.model.Role.valueOf(c.role).label }.getOrDefault(c.role)}",
                                        fontSize = 12.sp, color = EchoColors.Muted, fontWeight = FontWeight.SemiBold)
                                    if (c.liked.isNotBlank()) Text("👍 ${c.liked}", fontSize = 14.sp, color = EchoColors.Ink)
                                    if (c.improvements.isNotBlank()) Text("💡 ${c.improvements}", fontSize = 14.sp, color = EchoColors.Ink)
                                    if (c.bugs.isNotBlank()) Text("⚠️ ${c.bugs}", fontSize = 14.sp, color = EchoColors.RedText)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigButton("Reiniciar demo", onClick = { vm.resetDemo(); onMessage("Demo reiniciada") }, icon = Icons.Rounded.RestartAlt,
                        outlined = true, container = EchoColors.Ink, modifier = Modifier.weight(1f), height = 50.dp, fontSize = 13f)
                    BigButton("Borrar respuestas", onClick = { confirmDelete = true }, outlined = true, container = EchoColors.RedText,
                        modifier = Modifier.weight(1f), height = 50.dp, fontSize = 13f)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Color.White,
            title = { Text("¿Borrar todas las respuestas?") },
            text = { Text("Se eliminarán de este teléfono. Exporta el CSV antes si las necesitas.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.deleteAll(); onMessage("Respuestas borradas") }) {
                    Text("Borrar", color = EchoColors.RedText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar", color = EchoColors.Ink) } },
        )
    }
    if (confirmNext) {
        AlertDialog(
            onDismissRequest = { confirmNext = false },
            containerColor = Color.White,
            title = { Text("¿Pasar al siguiente participante?") },
            text = { Text("Se reinicia la demo y se borra tu perfil de este teléfono para que la siguiente persona se registre. Tus respuestas quedan guardadas.") },
            confirmButton = {
                TextButton(onClick = { confirmNext = false; vm.nextParticipant() }) { Text("Continuar", color = EchoColors.Blue, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmNext = false }) { Text("Cancelar", color = EchoColors.Ink) } },
        )
    }
}

@Composable
private fun IntroCard(tasks: Int) {
    Surface(shape = RoundedCornerShape(18.dp), color = EchoColors.BlueSoft) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Text("Prueba de la app · 5 minutos", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = EchoColors.Ink)
            Spacer(Modifier.height(4.dp))
            Text(
                "Intenta $tasks tareas como si fuera una emergencia, responde 5 preguntas y cuéntanos qué mejorarías. " +
                    "No hay respuestas correctas: evaluamos la app, no a ti.",
                fontSize = 14.sp, color = EchoColors.Ink,
            )
        }
    }
}

@Composable
private fun TaskCard(n: Int, task: UsabilityTask, selected: TaskOutcome?, onSelect: (TaskOutcome) -> Unit) {
    EchoCard(border = if (selected != null) EchoColors.Blue.copy(alpha = 0.4f) else EchoColors.Line) {
        Column(Modifier.padding(14.dp)) {
            Text("$n. ${task.title}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = EchoColors.Ink)
            Text(task.hint, fontSize = 12.sp, color = EchoColors.Muted)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TaskOutcome.entries.forEach { o ->
                    val on = selected == o
                    val color = when (o) {
                        TaskOutcome.EASY -> EchoColors.GreenDark
                        TaskOutcome.HARD -> EchoColors.DrillAmber
                        TaskOutcome.FAILED -> EchoColors.RedText
                        TaskOutcome.SKIPPED -> EchoColors.Muted
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (on) color else EchoColors.BgCool)
                            .selectable(selected = on, role = Role.RadioButton) { onSelect(o) }
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(o.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                            color = if (on) Color.White else EchoColors.Ink, lineHeight = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingCard(q: RatingQuestion, value: Int?, onSelect: (Int) -> Unit) {
    EchoCard {
        Column(Modifier.padding(14.dp)) {
            Text(q.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = EchoColors.Ink)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                (1..5).forEach { v ->
                    val on = value != null && v <= value
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .selectable(selected = value == v, role = Role.RadioButton) { onSelect(v) }
                            .semantics { contentDescription = "$v de 5" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(if (on) Icons.Rounded.Star else Icons.Rounded.StarBorder, null,
                            tint = if (on) EchoColors.Amber else EchoColors.Soft, modifier = Modifier.size(36.dp))
                    }
                }
            }
            Row {
                Text(q.low, fontSize = 11.sp, color = EchoColors.Soft, modifier = Modifier.weight(1f))
                Text(q.high, fontSize = 11.sp, color = EchoColors.Soft)
            }
        }
    }
}

@Composable
private fun RecommendCard(value: Int?, onSelect: (Int) -> Unit) {
    EchoCard {
        Column(Modifier.padding(14.dp)) {
            Text("Del 0 al 10, ¿qué tanto recomendarías Echo Guard a tu colegio?", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = EchoColors.Ink)
            Spacer(Modifier.height(8.dp))
            listOf(0..5, 6..10).forEach { range ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    range.forEach { v ->
                        val on = value == v
                        val color = when {
                            v <= 6 -> EchoColors.RedText
                            v <= 8 -> EchoColors.DrillAmber
                            else -> EchoColors.GreenDark
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (on) color else EchoColors.BgCool)
                                .selectable(selected = on, role = Role.RadioButton) { onSelect(v) },
                            contentAlignment = Alignment.Center,
                        ) { Text("$v", fontWeight = FontWeight.Bold, color = if (on) Color.White else EchoColors.Ink) }
                    }
                    if (range.first == 6) Spacer(Modifier.weight(1f))
                }
            }
            Row {
                Text("Nada probable", fontSize = 11.sp, color = EchoColors.Soft, modifier = Modifier.weight(1f))
                Text("Muy probable", fontSize = 11.sp, color = EchoColors.Soft)
            }
        }
    }
}

@Composable
private fun ThanksCard(onAnother: () -> Unit, onNext: () -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = EchoColors.GreenSoft, border = BorderStroke(1.dp, EchoColors.Green.copy(alpha = 0.4f))) {
        Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.CheckCircle, null, tint = EchoColors.SafeGreen, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(8.dp))
            Text("¡Gracias por tu opinión!", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = EchoColors.Ink)
            Text("Quedó guardada. Con ella mejoramos la app antes de usarla en el colegio.", fontSize = 14.sp,
                color = EchoColors.Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            BigButton("Siguiente participante", onClick = onNext, icon = Icons.Rounded.PersonAdd, container = EchoColors.SafeGreen, height = 54.dp, fontSize = 15f)
            TextButton(onClick = onAnother, modifier = Modifier.heightIn(min = 48.dp)) { Text("Enviar otra respuesta", color = EchoColors.GreenDark) }
        }
    }
}

@Composable
private fun ResultsCard(s: FeedbackSummary) {
    EchoCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${s.responses}", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = EchoColors.Ink)
                    Text("respuestas", fontSize = 12.sp, color = EchoColors.Muted)
                }
                s.nps?.let { nps ->
                    Column(horizontalAlignment = Alignment.End) {
                        StatusPill("NPS ${if (nps > 0) "+" else ""}$nps", when {
                            nps >= 30 -> EchoColors.GreenDark
                            nps >= 0 -> EchoColors.DrillAmber
                            else -> EchoColors.RedText
                        })
                        Text("recomendación", fontSize = 11.sp, color = EchoColors.Muted)
                    }
                }
            }
            if (s.byRole.isNotEmpty()) {
                Text(s.byRole.entries.joinToString(" · ") { "${it.key}: ${it.value}" }, fontSize = 12.sp, color = EchoColors.Muted)
            }
            if (s.responses > 0) {
                RatingQuestion.entries.forEach { q ->
                    val avg = s.averages[q] ?: 0f
                    Column {
                        Row {
                            Text(q.text, fontSize = 13.sp, color = EchoColors.Ink, modifier = Modifier.weight(1f))
                            Text("%.1f / 5".format(avg), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EchoColors.Ink)
                        }
                        LinearProgressIndicator(
                            progress = { avg / 5f }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = if (avg >= 4f) EchoColors.Green else if (avg >= 3f) EchoColors.Amber else EchoColors.RedText,
                            trackColor = EchoColors.CellOff,
                        )
                    }
                }
            }
            if (s.tasks.isNotEmpty()) {
                Text("Tareas (las más difíciles primero)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EchoColors.Ink, modifier = Modifier.padding(top = 6.dp))
                s.tasks.forEach { t ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(t.title, fontSize = 13.sp, color = EchoColors.Ink)
                            Text("${t.easy} fácil · ${t.hard} le costó · ${t.failed} no pudo", fontSize = 11.sp, color = EchoColors.Muted)
                        }
                        Text("${t.successPercent}%", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = when {
                            t.successPercent >= 80 -> EchoColors.GreenDark
                            t.successPercent >= 50 -> EchoColors.DrillAmber
                            else -> EchoColors.RedText
                        })
                    }
                }
            }
        }
    }
}
