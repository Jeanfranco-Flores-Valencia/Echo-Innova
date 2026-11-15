package com.floresvalencia.echoguard

import com.floresvalencia.echoguard.data.hardware.Telemetry
import com.floresvalencia.echoguard.data.map.SchoolMap
import com.floresvalencia.echoguard.data.model.AckStatus
import com.floresvalencia.echoguard.data.model.AlertKind
import com.floresvalencia.echoguard.data.model.ArrowDir
import com.floresvalencia.echoguard.data.model.EmergencyMode
import com.floresvalencia.echoguard.data.model.EvacuationSummary
import com.floresvalencia.echoguard.data.model.ExitRoute
import com.floresvalencia.echoguard.data.model.HazardType
import com.floresvalencia.echoguard.data.model.MapPoint
import com.floresvalencia.echoguard.data.model.RouteStatus
import com.floresvalencia.echoguard.data.model.RoverMode
import com.floresvalencia.echoguard.data.model.summary
import com.floresvalencia.echoguard.data.repository.EchoRepository
import com.floresvalencia.echoguard.data.repository.EmergencyLog
import com.floresvalencia.echoguard.data.repository.InMemoryStaffDirectory
import com.floresvalencia.echoguard.data.sos.SosContext
import com.floresvalencia.echoguard.data.sos.SosKind
import com.floresvalencia.echoguard.data.sos.SosMessage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Mejoras de la v2.0 a partir de la entrevista con el especialista de bomberos. */
class V2FeaturesTest {
    private val repo = EchoRepository

    @Before fun setUp() {
        repo.staffDirectory = InMemoryStaffDirectory()
        repo.simulateResponders = false
        repo.emergencyLog = EmergencyLog { }
        repo.clearProfile()
        repo.resetDemo()
    }

    private fun exits(a: RouteStatus = RouteStatus.FREE, b: RouteStatus = RouteStatus.FREE, c: RouteStatus = RouteStatus.FREE) = listOf(
        ExitRoute("A", "Salida A", "Pasillo este", 31, a, MapPoint(60f, 17f), ArrowDir.RIGHT),
        ExitRoute("B", "Salida B", "Escalera norte", 24, b, MapPoint(30f, 0f), ArrowDir.UP),
        ExitRoute("C", "Salida C", "Patio central", 45, c, MapPoint(30f, 34f), ArrowDir.DOWN),
    )

    // ---------------- Mapa y rutas ----------------

    @Test fun `la ruta sigue los pasillos hasta la salida mas cercana`() {
        val p = SchoolMap.route("3C", exits())!!
        assertEquals("A", p.exit.id)                 // 3C → pasillo este → salida A (23 m)
        assertEquals(23, p.distanceM)
        assertEquals(MapPoint(40f, 14f), p.points.first())
        assertEquals(MapPoint(60f, 17f), p.points.last())
        assertFalse(p.isAlternative)
    }

    @Test fun `si la salida esta bloqueada da una ruta alternativa`() {
        val p = SchoolMap.route("3C", exits(a = RouteStatus.BLOCKED))!!
        assertTrue(p.exit.id != "A")
        assertTrue(p.isAlternative)
        assertEquals("A", p.avoided.single().id)
        assertEquals(30, p.distanceM)
    }

    @Test fun `una salida congestionada se evita si hay otra cercana libre`() {
        val free = SchoolMap.route("LAB", exits())!!
        assertEquals("A", free.exit.id)
        val congested = SchoolMap.route("LAB", exits(a = RouteStatus.CONGESTED))!!
        assertEquals("A", congested.exit.id)        // LAB está muy cerca de A: aun congestionada conviene
        val far = SchoolMap.route("3B", exits(b = RouteStatus.CONGESTED))!!
        assertTrue(far.exit.id != "B")
    }

    @Test fun `sin salidas libres no hay ruta`() {
        assertNull(SchoolMap.route("3A", exits(RouteStatus.BLOCKED, RouteStatus.BLOCKED, RouteStatus.BLOCKED)))
    }

    @Test fun `el humo detectado cierra la salida cercana y se reabre al resolver`() = runTest {
        repo.enterStaffMode("1111")
        repo.simulateHazard(HazardType.SMOKE)
        val blocked = repo.routes.value.single { it.status == RouteStatus.BLOCKED }
        assertEquals(HazardType.SMOKE, blocked.blockedBy)
        val alert = repo.alerts.value.last()
        assertTrue(alert.detail.contains("ruta alternativa"))
        repo.resolveAlert(alert.id)
        assertTrue(repo.routes.value.none { it.status == RouteStatus.BLOCKED })
    }

    // ---------------- Confirmaciones de alertas ----------------

    @Test fun `la evacuacion llega a todos los responsables y se ve quien confirma`() = runTest {
        repo.enterStaffMode("1111")
        repo.startEmergency(repo.session.value, EmergencyMode.DRILL, null)
        val alert = repo.alerts.value.last { it.kind == AlertKind.EVACUATION }
        val acks = repo.acks.value.getValue(alert.id)
        assertEquals(repo.responders.size, acks.size)
        assertEquals(AckStatus.RECEIVED, acks["gerardo"])          // quien la inicia ya la tiene
        repo.confirmAlert(alert.id)
        val s = repo.acks.value.getValue(alert.id).summary()
        assertEquals(1, s.confirmed)
        assertEquals(repo.responders.size - 1, s.pending)
        assertEquals(repo.responders.size - 1, repo.resendPending(alert.id).size)
    }

    @Test fun `las detecciones solo se envian a quien decide`() = runTest {
        repo.enterStaffMode("1111")
        repo.simulateHazard(HazardType.FIRE)
        val acks = repo.acks.value.getValue(repo.alerts.value.last().id)
        assertEquals(setOf("gerardo", "director"), acks.keys)
    }

    @Test fun `un alumno no puede reenviar alertas`() = runTest {
        repo.enterStaffMode("1111")
        repo.startEmergency(repo.session.value, EmergencyMode.DRILL, null)
        val id = repo.alerts.value.last().id
        repo.exitStaffMode()
        repo.register("Ana", student = true, classroomId = "3A")
        assertTrue(repo.resendPending(id).isEmpty())
    }

    // ---------------- Rovers ----------------

    @Test fun `con poca bateria el rover vuelve solo a cargar`() {
        repo.setBatteryForTest("R-02", EchoRepository.AUTO_RETURN_BATTERY + 1)
        val returned = repo.simulateStep(20)
        assertEquals(listOf("R-02"), returned)
        val r = repo.rovers.value.first { it.id == "R-02" }
        assertEquals(RoverMode.CHARGING, r.mode)
        assertEquals(repo.stationOf(r).position, r.position)
        assertTrue(repo.alerts.value.last().title.contains("volvió a cargar"))
    }

    @Test fun `solo la brigada activa la vision nocturna`() = runTest {
        repo.register("Ana", student = true, classroomId = "3A")
        repo.setNightVision("R-02", true)
        assertFalse(repo.rovers.value.first { it.id == "R-02" }.nightVision)
        repo.enterStaffMode("1111")
        repo.setNightVision("R-02", true)
        assertTrue(repo.rovers.value.first { it.id == "R-02" }.nightVision)
    }

    @Test fun `telemetria real del rover y de la estacion`() {
        val t = Telemetry.parse("""{"rover":"R-01","battery":76,"smoke":640,"flame":true,"x":20.5,"y":17}""") as Telemetry.Rover
        assertEquals(76, t.battery)
        assertEquals(640, t.smokePpm)
        assertEquals(true, t.flame)
        assertNull(t.coPpm)
        assertTrue(repo.applyTelemetry(t))
        val r = repo.rovers.value.first { it.id == "R-01" }
        assertTrue(r.sensors.smokeAlarm)
        assertEquals(MapPoint(20.5f, 17f), r.position)
        val st = Telemetry.parse("""{"station":"E1","seismic":0.31}""") as Telemetry.Station
        assertTrue(repo.applyTelemetry(st))
        assertTrue(repo.stationReadings.value.getValue("E1").seismicAlarm)
        assertNull(Telemetry.parse("basura"))
        assertFalse(repo.applyTelemetry(Telemetry.Rover("R-99", null, null, null, null, null, null, null)))
    }

    // ---------------- Seguridad ----------------

    @Test fun `la evacuacion real se confirma con el PIN de quien tiene la sesion`() = runTest {
        assertFalse(repo.verifyStaffPin("1111"))            // sin modo personal
        repo.enterStaffMode("1111")
        assertTrue(repo.verifyStaffPin("1111"))
        assertFalse(repo.verifyStaffPin("2222"))            // PIN de otra persona
    }

    @Test fun `los reportes anonimos se limitan para evitar alertas falsas`() {
        val t = 5_000_000L
        assertTrue(repo.reportEmergency(null, HazardType.FIRE, t))
        assertFalse(repo.reportEmergency(null, HazardType.FIRE, t + 10_000))
        assertTrue(repo.reportEmergency(null, HazardType.FIRE, t + EchoRepository.ANONYMOUS_REPORT_INTERVAL_MS))
    }

    @Test fun `cada emergencia queda en el historial`() = runTest {
        val log = mutableListOf<EvacuationSummary>()
        repo.emergencyLog = EmergencyLog { log += it }
        repo.enterStaffMode("1111")
        repo.startEmergency(repo.session.value, EmergencyMode.DRILL, HazardType.EARTHQUAKE)
        repo.confirmAlert(repo.alerts.value.last().id)
        repo.stopEmergency(repo.session.value)
        val rec = log.single()
        assertEquals("Gerardo", rec.startedBy)
        assertEquals("Gerardo", rec.endedBy)
        assertEquals(HazardType.EARTHQUAKE, rec.cause)
        assertEquals(repo.responders.size, rec.responders)
        assertEquals(1, rec.confirmed)
    }

    @Test fun `la ubicacion no queda guardada en el historial del SOS`() {
        val ctx = SosContext(
            kind = SosKind.HELP, name = "Ana", roleLabel = "Alumno", classroomId = "3A", place = "Bloque A",
            location = com.floresvalencia.echoguard.data.sos.GeoFix(-12.0, -77.0, 10, 0L), battery = null,
            emergency = null, route = null, nowMillis = 0L,
        )
        val msg = SosMessage.build(ctx)
        assertTrue(msg.contains("maps.google.com"))
        val saved = SosMessage.forHistory(msg)
        assertFalse(saved.contains("maps.google.com"))
        assertTrue(saved.contains("Ubicacion: enviada a los contactos"))
        val hidden = SosMessage.build(ctx.copy(location = null, locationShared = false))
        assertTrue(hidden.contains("Ubicacion: no compartida"))
        assertNotNull(hidden)
    }
}
