package com.floresvalencia.echoguard.ui.screens.guide

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Backpack
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Propane
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floresvalencia.echoguard.ui.components.EchoCard
import com.floresvalencia.echoguard.ui.components.IconChip
import com.floresvalencia.echoguard.ui.components.SectionTitle
import com.floresvalencia.echoguard.ui.components.SubScreenHeader
import com.floresvalencia.echoguard.ui.theme.EchoColors

private data class HazardGuide(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val before: List<String>,
    val during: List<String>,
    val after: List<String>,
)

/** Recomendaciones basadas en las guías de INDECI y del Cuerpo General de Bomberos del Perú. */
private val guides = listOf(
    HazardGuide(
        "Sismo", Icons.Rounded.Vibration, EchoColors.DrillAmber,
        before = listOf("Ubica las zonas seguras y las rutas de salida de tu aula.", "Ten lista tu mochila de emergencia.", "Participa en los simulacros."),
        during = listOf("Mantén la calma. Aléjate de ventanas y objetos que puedan caer.", "Si no puedes salir, ubícate en la zona segura del aula.", "No uses ascensores ni escaleras mientras tiembla."),
        after = listOf("Evacúa por la ruta indicada, camina sin correr.", "Ve al punto de reunión y repórtate con tu docente.", "Prepárate para réplicas. Usa mensajes de texto: deja libres las líneas."),
    ),
    HazardGuide(
        "Incendio", Icons.Rounded.LocalFireDepartment, EchoColors.Red,
        before = listOf("Conoce dónde están los extintores y las salidas.", "No sobrecargues enchufes ni dejes equipos encendidos."),
        during = listOf("Avisa de inmediato y llama al 116.", "Evacúa sin detenerte por tus cosas. No uses ascensores.", "Antes de abrir una puerta, toca la manija: si está caliente, busca otra salida.", "Si se prende tu ropa: detente, tírate al suelo y rueda."),
        after = listOf("No regreses al edificio hasta que lo autoricen.", "Repórtate en el punto de reunión."),
    ),
    HazardGuide(
        "Humo", Icons.Rounded.Air, EchoColors.Muted,
        before = listOf("Identifica salidas alternativas por si una está bloqueada."),
        during = listOf("Agáchate: el aire limpio está cerca del suelo.", "Cúbrete nariz y boca con un paño, de ser posible húmedo.", "Avanza pegado a la pared hacia la salida."),
        after = listOf("Si tienes dificultad para respirar, pide ayuda a la brigada (SAMU 106)."),
    ),
    HazardGuide(
        "Fuga de gas / CO", Icons.Rounded.Propane, EchoColors.Cyan,
        before = listOf("Reconoce el olor del gas y dónde están las llaves de paso."),
        during = listOf("No enciendas ni apagues interruptores, ni uses fuego.", "Abre puertas y ventanas si es seguro.", "Evacúa y avisa a la brigada."),
        after = listOf("Espera a que los bomberos o el personal autorizado revisen la zona."),
    ),
)

/** Mochila para emergencias recomendada por INDECI. */
private val backpack = listOf(
    "Agua embotellada", "Alimentos no perecibles", "Linterna y pilas", "Radio a pilas", "Botiquín de primeros auxilios",
    "Copia de documentos (DNI)", "Silbato", "Abrigo y manta", "Mascarillas", "Cargador o batería externa", "Lista de contactos en papel",
)

/** Guía disponible siempre, sin internet: qué hacer antes, durante y después. */
@Composable
fun GuideScreen(onBack: () -> Unit) {
    var open by rememberSaveable { mutableStateOf(0) }
    var packed by rememberSaveable { mutableStateOf(setOf<Int>()) }

    Column(
        Modifier
            .fillMaxSize()
            .background(EchoColors.Bg)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        SubScreenHeader("Guía de emergencias", "Disponible sin internet", onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            guides.forEachIndexed { i, g ->
                val expanded = open == i
                EchoCard(border = if (expanded) g.color.copy(alpha = 0.5f) else EchoColors.Line) {
                    Column {
                        Surface(onClick = { open = if (expanded) -1 else i }, color = Color.Transparent) {
                            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconChip(g.icon, g.color)
                                Spacer(Modifier.width(12.dp))
                                Text(g.title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = EchoColors.Ink, modifier = Modifier.weight(1f))
                                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, if (expanded) "Cerrar" else "Abrir", tint = EchoColors.Soft)
                            }
                        }
                        AnimatedVisibility(expanded) {
                            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Phase("Antes", g.before, EchoColors.Blue)
                                Phase("Durante", g.during, g.color)
                                Phase("Después", g.after, EchoColors.SafeGreen)
                            }
                        }
                    }
                }
            }

            SectionTitle("Mochila de emergencia (${packed.size}/${backpack.size})")
            EchoCard {
                Column(Modifier.padding(vertical = 6.dp)) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconChip(Icons.Rounded.Backpack, EchoColors.DrillAmber, size = 36.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Prepárala en casa y tenla a la mano.", fontSize = 13.sp, color = EchoColors.Muted)
                    }
                    backpack.forEachIndexed { i, item ->
                        val on = i in packed
                        Surface(onClick = { packed = if (on) packed - i else packed + i }, color = Color.Transparent) {
                            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (on) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank, null,
                                    tint = if (on) EchoColors.SafeGreen else EchoColors.Soft)
                                Spacer(Modifier.width(12.dp))
                                Text(item, fontSize = 15.sp, color = EchoColors.Ink)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Phase(title: String, steps: List<String>, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = color, letterSpacing = 1.5.sp)
        steps.forEach { s ->
            Row {
                Box(Modifier.padding(top = 7.dp).size(7.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(10.dp))
                Text(s, fontSize = 15.sp, color = EchoColors.Ink, lineHeight = 20.sp)
            }
        }
    }
}
