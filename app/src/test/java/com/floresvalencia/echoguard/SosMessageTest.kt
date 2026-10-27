package com.floresvalencia.echoguard

import com.floresvalencia.echoguard.data.sos.BatteryInfo
import com.floresvalencia.echoguard.data.sos.GeoFix
import com.floresvalencia.echoguard.data.sos.SosContext
import com.floresvalencia.echoguard.data.sos.SosKind
import com.floresvalencia.echoguard.data.sos.SosMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class SosMessageTest {
    private val now = 1_700_000_000_000L

    private fun ctx(location: GeoFix? = GeoFix(-12.046374, -77.042793, 15, now - 3 * 60_000), battery: BatteryInfo? = BatteryInfo(34, false, false)) =
        SosContext(
            kind = SosKind.HELP, name = "Luis Huamán", roleLabel = "Docente", classroomId = "3B",
            place = "Centro educativo · Bloque A", location = location, battery = battery,
            emergency = "Evacuación REAL en curso - Fuego", route = "Salida A - Pasillo este", nowMillis = now,
        )

    @Test fun `incluye todos los datos importantes`() {
        val m = SosMessage.build(ctx(), ZoneOffset.UTC)
        assertTrue(m.startsWith("SOS Echo Guard: NECESITO AYUDA"))
        assertTrue(m.contains("Luis Huaman (Docente, aula 3B)"))
        assertTrue(m.contains("https://maps.google.com/?q=-12.04637,-77.04279 (+-15 m, hace 3 min)"))
        assertTrue(m.contains("Bateria: 34%"))
        assertTrue(m.contains("Evacuacion REAL en curso - Fuego"))
        assertTrue(m.contains("Ruta: Salida A - Pasillo este"))
        assertTrue(m.contains("14/11 22:13"))
    }

    @Test fun `sin tildes ni simbolos que encarecen el SMS`() {
        val m = SosMessage.build(ctx(), ZoneOffset.UTC)
        assertFalse(m.any { it in "áéíóúÁÉÍÓÚ·°" })
    }

    @Test fun `sin GPS ni bateria lo dice claramente`() {
        val m = SosMessage.build(ctx(location = null, battery = null), ZoneOffset.UTC)
        assertTrue(m.contains("Ubicacion GPS: no disponible"))
        assertTrue(m.contains("Bateria: desconocida"))
    }

    @Test fun `bateria cargando o por agotarse`() {
        assertTrue(SosMessage.build(ctx(battery = BatteryInfo(80, true, false)), ZoneOffset.UTC).contains("80% (cargando)"))
        assertTrue(SosMessage.build(ctx(battery = BatteryInfo(9, false, false)), ZoneOffset.UTC).contains("9% (por agotarse)"))
    }

    @Test fun `validacion de telefonos`() {
        assertEquals("987654321", SosMessage.normalizePhone("987 654 321"))
        assertEquals("+51987654321", SosMessage.normalizePhone("+51 (987) 654-321"))
        assertNull(SosMessage.normalizePhone("12345"))
        assertNull(SosMessage.normalizePhone("98765abc4321"))
    }
}
