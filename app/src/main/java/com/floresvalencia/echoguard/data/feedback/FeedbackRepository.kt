package com.floresvalencia.echoguard.data.feedback

import com.floresvalencia.echoguard.data.db.EchoDatabase
import com.floresvalencia.echoguard.data.db.FeedbackEntity
import com.floresvalencia.echoguard.data.db.FeedbackWithTasks
import com.floresvalencia.echoguard.data.db.TaskResultEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Opiniones de las pruebas con usuarios. Siempre se guardan en el teléfono (sin conexión);
 * si hay un servidor configurado (FEEDBACK_URL) se envían en cuanto haya internet.
 */
class FeedbackRepository(private val db: EchoDatabase, private val serverUrl: String) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncLock = Mutex()
    private val dao get() = db.feedbackDao()

    val all: Flow<List<FeedbackWithTasks>> = dao.observeAll()

    val syncEnabled: Boolean get() = serverUrl.isNotBlank()

    suspend fun submit(feedback: FeedbackEntity, tasks: List<TaskResultEntity>): Long {
        val id = withContext(Dispatchers.IO) { dao.insertWithTasks(feedback, tasks) }
        sync()
        return id
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) { dao.deleteAll() }

    /** Envía las respuestas pendientes. Si falla (sin internet), se reintenta en el próximo envío o al abrir la app. */
    fun sync() {
        if (!syncEnabled) return
        scope.launch {
            syncLock.withLock {
                for (fw in dao.pending()) {
                    if (post(FeedbackStats.toJson(fw))) dao.markSynced(fw.feedback.id) else break
                }
            }
        }
    }

    private fun post(json: String): Boolean = try {
        val conn = (URL(serverUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 15_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        conn.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        conn.disconnect()
        code in 200..399
    } catch (_: Exception) {
        false
    }
}
