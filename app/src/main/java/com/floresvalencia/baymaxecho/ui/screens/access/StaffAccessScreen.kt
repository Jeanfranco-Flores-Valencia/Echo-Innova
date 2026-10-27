package com.floresvalencia.baymaxecho.ui.screens.access

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floresvalencia.baymaxecho.ui.components.PinPad
import com.floresvalencia.baymaxecho.ui.theme.EchoColors

/**
 * Acceso de personal con PIN. El modo personal no se guarda: al cerrar la app
 * se vuelve al perfil regular (un teléfono perdido no conserva permisos de brigada).
 */
@Composable
fun StaffAccessScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    vm: StaffAccessViewModel = viewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .background(EchoColors.Bg)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(onClick = onBack, shape = CircleShape, color = Color.Transparent, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", tint = EchoColors.Ink) }
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Acceso de personal", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = EchoColors.Ink)
            Spacer(Modifier.height(6.dp))
            Text(
                "Docentes, brigadistas y dirección.\nIngresa tu PIN de 4 dígitos.",
                fontSize = 15.sp, color = EchoColors.Muted, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            PinPad(
                pin = ui.pin,
                error = ui.error,
                onDigit = { d -> if (vm.onDigit(d)) onSuccess() },
                onDelete = vm::onDelete,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (ui.error) "PIN incorrecto" else " ",
                color = EchoColors.RedText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Demo: 1111 brigadista · 2222 dirección · 3333 docente",
                fontSize = 12.sp, color = EchoColors.Soft, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Por seguridad, el modo personal se cierra al salir de la app.",
                fontSize = 12.sp, color = EchoColors.Soft, textAlign = TextAlign.Center,
            )
        }
    }
}
