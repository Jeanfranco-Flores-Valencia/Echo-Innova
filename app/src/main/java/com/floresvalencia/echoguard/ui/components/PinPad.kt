package com.floresvalencia.echoguard.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floresvalencia.echoguard.ui.theme.EchoColors

/** Teclado numérico de teclas grandes (84 x 64 dp). */
@Composable
fun PinPad(pin: String, onDigit: (Char) -> Unit, onDelete: () -> Unit, error: Boolean = false) {
    val haptic = LocalHapticFeedback.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { i ->
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                error -> EchoColors.RedText
                                i < pin.length -> EchoColors.Navy
                                else -> EchoColors.CellOff
                            }
                        )
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        listOf("123", "456", "789", " 0<").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                row.forEach { k ->
                    when (k) {
                        ' ' -> Spacer(Modifier.size(width = 84.dp, height = 64.dp))
                        '<' -> Surface(
                            onClick = onDelete, shape = RoundedCornerShape(18.dp), color = Color.Transparent,
                            modifier = Modifier.size(width = 84.dp, height = 64.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Rounded.Backspace, "Borrar", tint = EchoColors.Ink)
                            }
                        }
                        else -> Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDigit(k)
                            },
                            shape = RoundedCornerShape(18.dp), color = Color.White,
                            border = BorderStroke(1.dp, EchoColors.Line),
                            modifier = Modifier.size(width = 84.dp, height = 64.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(k.toString(), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = EchoColors.Ink)
                            }
                        }
                    }
                }
            }
        }
    }
}
