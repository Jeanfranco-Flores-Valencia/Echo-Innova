package com.floresvalencia.echoguard

import com.floresvalencia.echoguard.data.db.FeedbackEntity
import com.floresvalencia.echoguard.data.db.FeedbackWithTasks
import com.floresvalencia.echoguard.data.db.TaskResultEntity
import com.floresvalencia.echoguard.data.feedback.FeedbackStats
import com.floresvalencia.echoguard.data.feedback.RatingQuestion
import com.floresvalencia.echoguard.data.feedback.TaskOutcome
import com.floresvalencia.echoguard.data.feedback.UsabilityTasks
import com.floresvalencia.echoguard.data.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class FeedbackStatsTest {

    private fun fb(id: Long, recommend: Int, ease: Int = 4, liked: String = "", role: Role = Role.ALUMNO, vararg tasks: Pair<String, TaskOutcome>) =
        FeedbackWithTasks(
            FeedbackEntity(
                id = id, createdAt = 0L, userId = "u$id", userName = "Persona $id", role = role.name, classroomId = null,
                ease = ease, clarity = 5, confidence = 3, design = 4, recommend = recommend,
                liked = liked, improvements = "", bugs = "", appVersion = "1.1", device = "test",
            ),
            tasks.map { TaskResultEntity(feedbackId = id, taskId = it.first, outcome = it.second.name) },
        )

    @Test fun `NPS y promedios`() {
        val s = FeedbackStats.summarize(listOf(fb(1, 10, ease = 5), fb(2, 9, ease = 3), fb(3, 5, ease = 4), fb(4, 7, ease = 4)))
        // 2 promotores, 1 detractor, 4 respuestas → (2-1)/4 = 25
        assertEquals(25, s.nps)
        assertEquals(4f, s.averages.getValue(RatingQuestion.EASE), 0.001f)
        assertEquals(4, s.responses)
    }

    @Test fun `sin respuestas no hay NPS`() {
        assertNull(FeedbackStats.summarize(emptyList()).nps)
    }

    @Test fun `tareas - porcentaje de exito y las mas dificiles primero`() {
        val s = FeedbackStats.summarize(
            listOf(
                fb(1, 8, tasks = arrayOf("route" to TaskOutcome.EASY, "sos" to TaskOutcome.FAILED)),
                fb(2, 8, tasks = arrayOf("route" to TaskOutcome.HARD, "sos" to TaskOutcome.FAILED)),
                fb(3, 8, tasks = arrayOf("route" to TaskOutcome.SKIPPED, "sos" to TaskOutcome.EASY)),
            )
        )
        assertEquals("sos", s.tasks.first().taskId)
        assertEquals(33, s.tasks.first().successPercent)
        val route = s.tasks.first { it.taskId == "route" }
        assertEquals(2, route.attempts) // "no la probé" no cuenta
        assertEquals(100, route.successPercent)
    }

    @Test fun `CSV escapa comas, comillas y saltos de linea`() {
        val csv = FeedbackStats.toCsv(listOf(fb(1, 9, liked = "Rápida, \"clara\"\ny simple")), ZoneOffset.UTC)
        assertTrue(csv.startsWith("﻿id,fecha,"))
        assertTrue(csv.contains("\"Rápida, \"\"clara\"\"\ny simple\""))
        assertTrue(csv.contains("1970-01-01 00:00"))
    }

    @Test fun `JSON valido con caracteres especiales`() {
        val json = FeedbackStats.toJson(fb(7, 9, liked = "dijo \"hola\"\n\\fin", tasks = arrayOf("route" to TaskOutcome.EASY)))
        assertTrue(json.contains("\"liked\":\"dijo \\\"hola\\\"\\n\\\\fin\""))
        assertTrue(json.contains("\"tasks\":[{\"task\":\"route\",\"outcome\":\"EASY\"}]"))
        assertTrue(json.contains("\"classroomId\":null"))
    }

    @Test fun `cada rol tiene tareas y el docente pasa lista`() {
        Role.entries.forEach { assertTrue(UsabilityTasks.forRole(it).isNotEmpty()) }
        assertTrue(UsabilityTasks.forRole(Role.DOCENTE).any { it.id == "roll" })
        assertTrue(UsabilityTasks.forRole(Role.ALUMNO).none { it.id == "start" })
    }
}
