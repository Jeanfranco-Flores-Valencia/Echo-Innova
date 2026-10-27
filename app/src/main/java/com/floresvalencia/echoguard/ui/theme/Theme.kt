package com.floresvalencia.echoguard.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.floresvalencia.echoguard.data.model.EmergencyMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Paleta tomada del prototipo HTML. */
object EchoColors {
    val Navy = Color(0xFF0B1424)
    val Navy2 = Color(0xFF111D33)
    val NavyCard = Color(0xFF0F1C36)
    val NavyCard2 = Color(0xFF16284A)
    val Ink = Color(0xFF0F172A)
    val Muted = Color(0xFF64748B)
    val Soft = Color(0xFF94A3B8)
    val Line = Color(0xFFE2E8F0)
    val CardLine = Color(0xFFD6E0EE)
    val Bg = Color(0xFFF8FAFC)
    val BgCool = Color(0xFFEEF3FA)
    val Sky = Color(0xFF7AB8FF)
    val SkyText = Color(0xFFBCD3F5)
    val SkyMuted = Color(0xFF9FB4D6)
    val Blue = Color(0xFF2563EB)
    val BlueSoft = Color(0xFFE0ECFF)
    val Green = Color(0xFF10B981)
    val GreenDark = Color(0xFF047857)
    val GreenSoft = Color(0xFFECFDF5)
    val Amber = Color(0xFFF59E0B)
    val AmberRoute = Color(0xFFF5A524)
    val DrillAmber = Color(0xFFB45309)
    val Red = Color(0xFFE60012)
    val RedText = Color(0xFFDC2626)
    val RedSoft = Color(0xFFFEF2F2)
    val Purple = Color(0xFF8B5CF6)
    val Cyan = Color(0xFF0EA5E9)
    val CellOff = Color(0xFFDBE3EF)
    /** Verde de seguridad para señalética de evacuación (estilo ISO 7010). */
    val SafeGreen = Color(0xFF00874A)
    /** Tema de emergencia real: cabeceras rojo oscuro / negro. */
    val AlarmDark = Color(0xFF1A0505)
    val AlarmDark2 = Color(0xFF4A0B0B)
}

/** Modo de emergencia actual, disponible para cualquier componente (cambia el aspecto global). */
val LocalEmergencyMode = staticCompositionLocalOf { EmergencyMode.NONE }

val Mono = FontFamily.Monospace

private val EchoScheme = lightColorScheme(
    primary = EchoColors.Navy,
    onPrimary = Color.White,
    secondary = EchoColors.Blue,
    onSecondary = Color.White,
    background = EchoColors.Bg,
    onBackground = EchoColors.Ink,
    surface = Color.White,
    onSurface = EchoColors.Ink,
    surfaceVariant = Color(0xFFE8EDF5),
    onSurfaceVariant = EchoColors.Muted,
    outline = EchoColors.CardLine,
    error = EchoColors.Red,
    onError = Color.White,
)

// Tamaños algo mayores que el estándar: en una emergencia se lee peor (visión de túnel).
private val EchoTypography = Typography(
    displayMedium = TextStyle(fontSize = 64.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp),
    headlineLarge = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 17.sp),
    bodyMedium = TextStyle(fontSize = 15.sp),
    bodySmall = TextStyle(fontSize = 13.sp),
    labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
)

private val EchoShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun EchoGuardTheme(content: @Composable () -> Unit) {
    // Siempre claro: contraste constante, independiente del modo del teléfono.
    MaterialTheme(colorScheme = EchoScheme, typography = EchoTypography, shapes = EchoShapes, content = content)
}
