package com.floresvalencia.echoguard.data.feedback

import com.floresvalencia.echoguard.data.db.FeedbackWithTasks
import com.floresvalencia.echoguard.data.model.Role
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** Cómo le fue a la persona con cada tarea de la prueba. */
enum class TaskOutcome(val label: String) {
    EASY("Fácil"),
    HARD("Me costó"),
    FAILED("No pude"),
    SKIPPED("No la probé"),
}

/** Tarea que se le pide a quien prueba la app (según su rol). */
data class UsabilityTask(val id: String, val title: String, val hint: String, val roles: Set<Role>)

object UsabilityTasks {
    private val everyone = Role.entries.toSet()
    private val managers = setOf(Role.BRIGADISTA, Role.DIRECTOR)
    private val regular = setOf(Role.ALUMNO, Role.GENERAL)

    val all = listOf(
        UsabilityTask("route", "Encontrar tu ruta de evacuación", "Inicio o pestaña Ruta", everyone),
        UsabilityTask("guide", "Leer qué hacer en caso de sismo", "Inicio → Guía de emergencias", everyone),
        UsabilityTask("contact", "Agregar un contacto de emergencia", "Inicio → Contactos SOS", everyone),
        UsabilityTask("sos", "Enviar un SOS de prueba a tu contacto", "Inicio → mantener presionado SOS", everyone),
        UsabilityTask("report", "Reportar una emergencia", "Inicio → Reportar", regular + Role.DOCENTE),
        UsabilityTask("help", "Pedir ayuda a la brigada", "Inicio → Necesito ayuda", regular),
        UsabilityTask("safe", "Marcarte \"Estoy a salvo\" en un simulacro", "Aparece en Inicio durante la evacuación", everyone),
        UsabilityTask("roll", "Pasar lista de tu aula", "Pestaña Mi aula", setOf(Role.DOCENTE)),
        UsabilityTask("start", "Iniciar un simulacro de evacuación", "Inicio → Iniciar evacuación → Simulacro", managers),
        UsabilityTask("alert", "Revisar y resolver una alerta", "Pestaña Alertas", managers),
        UsabilityTask("fleet", "Revisar el estado de los rovers", "Pestaña Flota", managers),
        UsabilityTask("deploy", "Desplegar un rover y encender la ruta luminosa", "Flota → elegir rover", setOf(Role.BRIGADISTA)),
        UsabilityTask("staff", "Crear una cuenta de personal", "Inicio → Roles y permisos", setOf(Role.DIRECTOR)),
        UsabilityTask("stop", "Finalizar la evacuación", "Banner rojo → Finalizar", managers),
    )

    fun forRole(role: Role): List<UsabilityTask> = all.filter { role in it.roles }

    fun title(id: String): String = all.firstOrNull { it.id == id }?.title ?: id
}

/** Preguntas de 1 a 5 (inspiradas en SUS, en versión corta). */
enum class RatingQuestion(val text: String, val low: String, val high: String) {
    EASE("¿Qué tan fácil fue usar la app?", "Muy difícil", "Muy fácil"),
    CLARITY("¿Las instrucciones de emergencia fueron claras?", "Confusas", "Muy claras"),
    CONFIDENCE("¿Te sentirías seguro(a) usándola en una emergencia real?", "Nada", "Totalmente"),
    DESIGN("¿Te gustó el diseño (colores, botones, textos)?", "No me gustó", "Me encantó"),
}

data class TaskStat(val taskId: String, val title: String, val easy: Int, val hard: Int, val failed: Int) {
    val attempts: Int get() = easy + hard + failed
    /** Porcentaje de personas que lograron la tarea (fácil o con dificultad). */
    val successPercent: Int get() = if (attempts == 0) 0 else ((easy + hard) * 100f / attempts).roundToInt()
}

data class FeedbackSummary(
    val responses: Int,
    val averages: Map<RatingQuestion, Float>,
    /** Net Promoter Score: % promotores (9-10) − % detractores (0-6). Entre -100 y 100. */
    val nps: Int?,
    val tasks: List<TaskStat>,
    val byRole: Map<String, Int>,
)

object FeedbackStats {

    fun summarize(list: List<FeedbackWithTasks>): FeedbackSummary {
        val f = list.map { it.feedback }
        fun avg(sel: (com.floresvalencia.echoguard.data.db.FeedbackEntity) -> Int) =
            if (f.isEmpty()) 0f else f.map(sel).average().toFloat()
        val averages = mapOf(
            RatingQuestion.EASE to avg { it.ease },
            RatingQuestion.CLARITY to avg { it.clarity },
            RatingQuestion.CONFIDENCE to avg { it.confidence },
            RatingQuestion.DESIGN to avg { it.design },
        )
        val nps = if (f.isEmpty()) null else {
            val promoters = f.count { it.recommend >= 9 }
            val detractors = f.count { it.recommend <= 6 }
            ((promoters - detractors) * 100f / f.size).roundToInt()
        }
        val results = list.flatMap { it.tasks }
        val tasks = results.groupBy { it.taskId }.map { (id, r) ->
            TaskStat(
                id, UsabilityTasks.title(id),
                easy = r.count { it.outcome == TaskOutcome.EASY.name },
                hard = r.count { it.outcome == TaskOutcome.HARD.name },
                failed = r.count { it.outcome == TaskOutcome.FAILED.name },
            )
        }.filter { it.attempts > 0 }.sortedBy { it.successPercent }
        val byRole = f.groupingBy { runCatching { Role.valueOf(it.role).label }.getOrDefault(it.role) }.eachCount()
        return FeedbackSummary(list.size, averages, nps, tasks, byRole)
    }

    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    private fun date(ms: Long, zone: ZoneId) = dateFormat.format(Instant.ofEpochMilli(ms).atZone(zone))

    /** Una fila por respuesta y una columna por tarea. Se abre directo en Excel o Google Sheets. */
    fun toCsv(list: List<FeedbackWithTasks>, zone: ZoneId = ZoneId.systemDefault()): String {
        val taskIds = UsabilityTasks.all.map { it.id }
        val header = listOf(
            "id", "fecha", "nombre", "rol", "aula", "facilidad", "claridad", "confianza", "diseño", "recomendaria_0_10",
        ) + taskIds.map { "tarea_$it" } + listOf("le_gusto", "mejoras", "errores", "version", "dispositivo")
        val rows = list.map { fw ->
            val f = fw.feedback
            val outcomes = fw.tasks.associate { it.taskId to it.outcome }
            listOf(
                f.id.toString(), date(f.createdAt, zone), f.userName, f.role, f.classroomId.orEmpty(),
                f.ease.toString(), f.clarity.toString(), f.confidence.toString(), f.design.toString(), f.recommend.toString(),
            ) + taskIds.map { id -> outcomes[id]?.let { o -> runCatching { TaskOutcome.valueOf(o).label }.getOrDefault(o) }.orEmpty() } +
                listOf(f.liked, f.improvements, f.bugs, f.appVersion, f.device)
        }
        // BOM para que Excel reconozca las tildes.
        return Char(0xFEFF) + (listOf(header) + rows).joinToString("\r\n") { r -> r.joinToString(",") { csvCell(it) } }
    }

    fun csvCell(v: String): String =
        if (v.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + v.replace("\"", "\"\"") + "\"" else v

    /** JSON de una respuesta para enviarla al servidor de opiniones. */
    fun toJson(fw: FeedbackWithTasks): String {
        val f = fw.feedback
        val tasks = fw.tasks.joinToString(",") { "{\"task\":${q(it.taskId)},\"outcome\":${q(it.outcome)}}" }
        return "{" + listOf(
            "\"id\":${f.id}", "\"createdAt\":${f.createdAt}", "\"userId\":${q(f.userId)}", "\"userName\":${q(f.userName)}",
            "\"role\":${q(f.role)}", "\"classroomId\":${f.classroomId?.let { q(it) } ?: "null"}",
            "\"ease\":${f.ease}", "\"clarity\":${f.clarity}", "\"confidence\":${f.confidence}", "\"design\":${f.design}",
            "\"recommend\":${f.recommend}", "\"liked\":${q(f.liked)}", "\"improvements\":${q(f.improvements)}",
            "\"bugs\":${q(f.bugs)}", "\"appVersion\":${q(f.appVersion)}", "\"device\":${q(f.device)}",
            "\"tasks\":[$tasks]",
        ).joinToString(",") + "}"
    }

    private fun q(v: String): String = buildString {
        append('"')
        v.forEach { c ->
            when {
                c == '"' -> append("\\\"")
                c == '\\' -> append("\\\\")
                c == '\n' -> append("\\n")
                c == '\r' -> append("\\r")
                c == '\t' -> append("\\t")
                c < ' ' -> append("\\u%04x".format(c.code))
                else -> append(c)
            }
        }
        append('"')
    }
}
