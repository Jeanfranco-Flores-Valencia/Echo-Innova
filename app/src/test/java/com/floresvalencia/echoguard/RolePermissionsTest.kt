package com.floresvalencia.echoguard

import com.floresvalencia.echoguard.data.model.Alert
import com.floresvalencia.echoguard.data.model.AlertKind
import com.floresvalencia.echoguard.data.model.Permission
import com.floresvalencia.echoguard.data.model.Role
import com.floresvalencia.echoguard.data.model.User
import com.floresvalencia.echoguard.data.model.canBeResolvedBy
import com.floresvalencia.echoguard.data.model.isVisibleTo
import com.floresvalencia.echoguard.data.model.validatePin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RolePermissionsTest {

    @Test fun `solo brigada y direccion inician la evacuacion`() {
        assertEquals(setOf(Role.BRIGADISTA, Role.DIRECTOR), Role.entries.filter { it.canStartEvacuation }.toSet())
    }

    @Test fun `solo la brigada controla los rovers`() {
        assertEquals(listOf(Role.BRIGADISTA), Role.entries.filter { it.has(Permission.CONTROL_ROVERS) })
    }

    @Test fun `solo direccion administra al personal y ve la auditoria`() {
        assertEquals(listOf(Role.DIRECTOR), Role.entries.filter { it.has(Permission.MANAGE_STAFF) })
        assertEquals(listOf(Role.DIRECTOR), Role.entries.filter { it.has(Permission.VIEW_AUDIT_LOG) })
    }

    @Test fun `todos pueden reportar, ver la ruta y enviar SOS`() {
        Role.entries.forEach { r ->
            assertTrue(r.has(Permission.REPORT_EMERGENCY))
            assertTrue(r.has(Permission.VIEW_ROUTE))
            assertTrue(r.has(Permission.SEND_SOS))
        }
    }

    @Test fun `el docente solicita pero no inicia la evacuacion`() {
        assertTrue(Role.DOCENTE.has(Permission.REQUEST_EVACUATION))
        assertTrue(Role.DOCENTE.has(Permission.TAKE_ROLL))
        assertFalse(Role.DOCENTE.canStartEvacuation)
    }

    @Test fun `alumnos y visitantes no tienen permisos de personal`() {
        listOf(Role.ALUMNO, Role.GENERAL).forEach { r ->
            assertFalse(r.isStaff)
            assertTrue(r.permissions.all { it.group.name == "EMERGENCY" })
        }
    }

    @Test fun `las detecciones solo las ve quien decide o quien reporto`() {
        val alumno = User("u1", "Ana", Role.ALUMNO)
        val brigada = User("b", "Gerardo", Role.BRIGADISTA)
        val hazard = alert(AlertKind.HAZARD)
        assertFalse(hazard.isVisibleTo(alumno))
        assertTrue(hazard.isVisibleTo(brigada))
        assertTrue(alert(AlertKind.REQUEST, createdBy = "u1").isVisibleTo(alumno))
        assertTrue(alert(AlertKind.EVACUATION).isVisibleTo(alumno))
    }

    @Test fun `la evacuacion no se resuelve como alerta`() {
        assertFalse(alert(AlertKind.EVACUATION).canBeResolvedBy(Role.DIRECTOR))
        assertTrue(alert(AlertKind.HAZARD).canBeResolvedBy(Role.BRIGADISTA))
        assertFalse(alert(AlertKind.HAZARD).canBeResolvedBy(Role.DOCENTE))
        assertTrue(alert(AlertKind.SYSTEM).canBeResolvedBy(Role.DIRECTOR))
    }

    @Test fun `reglas del PIN`() {
        assertNull(validatePin("4826"))
        assertNotNull(validatePin("123"))
        assertNotNull(validatePin("12a4"))
        assertNotNull(validatePin("7777"))
        assertNotNull(validatePin("1234"))
    }

    private fun alert(kind: AlertKind, createdBy: String? = null) =
        Alert(1, kind, "t", "s", "08:00", critical = true, active = true, detail = "", createdBy = createdBy)
}
