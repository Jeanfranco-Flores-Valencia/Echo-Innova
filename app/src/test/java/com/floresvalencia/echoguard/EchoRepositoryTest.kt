package com.floresvalencia.echoguard

import com.floresvalencia.echoguard.data.model.AuditEntry
import com.floresvalencia.echoguard.data.model.EmergencyMode
import com.floresvalencia.echoguard.data.model.EntryResult
import com.floresvalencia.echoguard.data.model.Role
import com.floresvalencia.echoguard.data.model.RollStatus
import com.floresvalencia.echoguard.data.model.RoverMode
import com.floresvalencia.echoguard.data.model.RouteStatus
import com.floresvalencia.echoguard.data.model.StaffLogin
import com.floresvalencia.echoguard.data.repository.AuditLog
import com.floresvalencia.echoguard.data.repository.EchoRepository
import com.floresvalencia.echoguard.data.repository.InMemoryStaffDirectory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EchoRepositoryTest {
    private val repo = EchoRepository
    private val audit = mutableListOf<AuditEntry>()

    @Before fun setUp() {
        repo.staffDirectory = InMemoryStaffDirectory()
        repo.auditLog = AuditLog { audit.add(it) }
        repo.clearProfile()
        repo.resetDemo()
        audit.clear()
    }

    @Test fun `PIN correcto entra en modo personal`() = runTest {
        val r = repo.enterStaffMode("1111")
        assertTrue(r is StaffLogin.Success)
        assertEquals(Role.BRIGADISTA, repo.session.value?.role)
        assertTrue(audit.any { it.action == "Ingreso de personal" })
    }

    @Test fun `cinco PIN incorrectos bloquean el acceso`() = runTest {
        val t = 1_000_000L
        repeat(4) { assertTrue(repo.enterStaffMode("9999", t) is StaffLogin.WrongPin) }
        assertTrue(repo.enterStaffMode("9999", t) is StaffLogin.Locked)
        // Bloqueado incluso con el PIN correcto.
        assertTrue(repo.enterStaffMode("1111", t + 1_000) is StaffLogin.Locked)
        // Pasados 30 s vuelve a funcionar.
        assertTrue(repo.enterStaffMode("1111", t + 31_000) is StaffLogin.Success)
    }

    @Test fun `un alumno no puede controlar rovers ni reportar salidas`() {
        repo.register("Ana", student = true, classroomId = "3A")
        assertEquals(EchoRepository.NO_PERMISSION, repo.toggleDeploy("R-01"))
        val before = repo.routes.value.first { it.id == "A" }.status
        repo.cycleRoute("A")
        assertEquals(before, repo.routes.value.first { it.id == "A" }.status)
        assertFalse(repo.startEmergency(repo.session.value, EmergencyMode.REAL, null))
    }

    @Test fun `la brigada despliega rovers y reporta salidas`() = runTest {
        repo.enterStaffMode("1111")
        assertNull(repo.toggleDeploy("R-01"))
        assertEquals(RoverMode.PATROLLING, repo.rovers.value.first { it.id == "R-01" }.mode)
        repo.cycleRoute("A")
        assertEquals(RouteStatus.CONGESTED, repo.routes.value.first { it.id == "A" }.status)
    }

    @Test fun `direccion ve la flota pero no la controla`() = runTest {
        repo.enterStaffMode("2222")
        assertEquals(EchoRepository.NO_PERMISSION, repo.toggleDeploy("R-01"))
    }

    @Test fun `al finalizar la evacuacion los rovers vuelven a su estacion`() = runTest {
        repo.enterStaffMode("1111")
        assertTrue(repo.startEmergency(repo.session.value, EmergencyMode.DRILL, null))
        assertTrue(repo.rovers.value.any { it.mode == RoverMode.PATROLLING && it.lightRoute })
        assertTrue(repo.stopEmergency(repo.session.value)!!.durationMs >= 0)
        repo.rovers.value.filter { it.online }.forEach { r ->
            assertTrue("${r.id} sigue desplegado", r.mode == RoverMode.DOCKED || r.mode == RoverMode.CHARGING)
            assertFalse(r.lightRoute)
        }
    }

    @Test fun `estoy a salvo solo durante una evacuacion`() = runTest {
        repo.register("Ana", student = true, classroomId = "3A")
        assertFalse(repo.markSafe())
        repo.enterStaffMode("1111")
        repo.startEmergency(repo.session.value, EmergencyMode.DRILL, null)
        repo.exitStaffMode()
        assertTrue(repo.markSafe())
        assertTrue(repo.markSafe()) // no se duplica
        assertEquals(1, repo.safe.value.size)
        assertEquals("Ana", repo.safe.value.single().name)
    }

    @Test fun `el docente solo pasa lista de su aula`() = runTest {
        repo.enterStaffMode("3333") // aula 3B
        repo.setRoll("3A", RollStatus.COMPLETE, 0)
        assertEquals(RollStatus.PENDING, repo.classrooms.value.first { it.id == "3A" }.status)
        repo.setRoll("3B", RollStatus.MISSING, 2)
        val room = repo.classrooms.value.first { it.id == "3B" }
        assertEquals(RollStatus.MISSING, room.status)
        assertEquals(2, room.missing)
    }

    @Test fun `el modo personal se cierra por inactividad salvo en emergencia`() = runTest {
        repo.enterStaffMode("1111")
        val later = System.currentTimeMillis() + EchoRepository.STAFF_IDLE_MS + 1_000
        repo.startEmergency(repo.session.value, EmergencyMode.DRILL, null)
        assertFalse(repo.checkStaffIdle(later))
        repo.stopEmergency(repo.session.value)
        assertTrue(repo.checkStaffIdle(later))
        assertNull(repo.session.value)
    }

    @Test fun `un alumno no resuelve detecciones`() = runTest {
        repo.enterStaffMode("1111")
        repo.simulateHazard(com.floresvalencia.echoguard.data.model.HazardType.FIRE)
        val id = repo.alerts.value.last().id
        repo.exitStaffMode()
        repo.register("Ana", student = true, classroomId = "3A")
        repo.resolveAlert(id)
        assertTrue(repo.alerts.value.first { it.id == id }.active)
    }

    @Test fun `al iniciar la evacuacion todos los que estan dentro quedan pendientes`() = runTest {
        repo.enterStaffMode("1111")
        val inside = repo.occupancy.value.inside
        assertEquals(0, repo.occupancy.value.evacuated)
        repo.startEmergency(repo.session.value, EmergencyMode.DRILL, null)
        assertEquals(inside, repo.occupancy.value.atAlarm)
        repeat(3) { assertTrue(repo.registerExit()) }
        val o = repo.occupancy.value
        assertEquals(3, o.evacuated)
        assertEquals(inside - 3, o.pending)
        val summary = repo.stopEmergency(repo.session.value)!!
        assertEquals(inside, summary.atAlarm)
        assertEquals(3, summary.evacuated)
        assertEquals(inside - 3, summary.stillInside)
        assertEquals(0, repo.occupancy.value.atAlarm)
    }

    @Test fun `salidas sin evacuacion no cuentan como evacuadas`() = runTest {
        repo.enterStaffMode("1111")
        repo.registerExit()
        assertEquals(0, repo.occupancy.value.evacuated)
    }

    @Test fun `durante la evacuacion solo se acepta un reingreso de rescate`() = runTest {
        repo.enterStaffMode("1111")
        repo.startEmergency(repo.session.value, EmergencyMode.DRILL, null)
        val inside = repo.occupancy.value.inside
        assertEquals(EntryResult.DURING_EVACUATION, repo.registerEntry())
        assertEquals(inside, repo.occupancy.value.inside)
        assertEquals(EntryResult.OK, repo.registerEntry(rescue = true))
        assertEquals(inside + 1, repo.occupancy.value.inside)
        assertTrue(audit.any { it.action == "Reingreso durante la evacuación" })
    }

    @Test fun `ultimos 5 minutos descarta pasos antiguos`() = runTest {
        repo.enterStaffMode("1111")
        val t = 10_000_000L
        repo.registerExit(t)
        repo.registerExit(t + 60_000)
        repo.registerExit(t + 6 * 60_000)
        val o = repo.occupancy.value
        assertEquals(2, o.exitsLast5Min(t + 6 * 60_000))
        assertEquals(0, o.exitsLast5Min(t + 20 * 60_000))
    }

    @Test fun `un alumno no registra el aforo`() {
        repo.register("Ana", student = true, classroomId = "3A")
        assertEquals(EntryResult.NO_PERMISSION, repo.registerEntry())
        assertFalse(repo.registerExit())
    }
}
