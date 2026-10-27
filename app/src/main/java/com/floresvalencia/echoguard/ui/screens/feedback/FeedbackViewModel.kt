package com.floresvalencia.echoguard.ui.screens.feedback

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.floresvalencia.echoguard.AppGraph
import com.floresvalencia.echoguard.BuildConfig
import com.floresvalencia.echoguard.data.db.FeedbackEntity
import com.floresvalencia.echoguard.data.db.FeedbackWithTasks
import com.floresvalencia.echoguard.data.db.TaskResultEntity
import com.floresvalencia.echoguard.data.feedback.FeedbackStats
import com.floresvalencia.echoguard.data.feedback.FeedbackSummary
import com.floresvalencia.echoguard.data.feedback.RatingQuestion
import com.floresvalencia.echoguard.data.feedback.TaskOutcome
import com.floresvalencia.echoguard.data.feedback.UsabilityTask
import com.floresvalencia.echoguard.data.feedback.UsabilityTasks
import com.floresvalencia.echoguard.data.model.Permission
import com.floresvalencia.echoguard.data.model.User
import com.floresvalencia.echoguard.data.repository.EchoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Lo que la persona va llenando. */
data class FeedbackForm(
    val outcomes: Map<String, TaskOutcome> = emptyMap(),
    val ratings: Map<RatingQuestion, Int> = emptyMap(),
    val recommend: Int? = null,
    val liked: String = "",
    val improvements: String = "",
    val bugs: String = "",
    val sending: Boolean = false,
    val sent: Boolean = false,
    val error: String? = null,
) {
    val missing: List<String>
        get() = buildList {
            RatingQuestion.entries.filter { it !in ratings }.takeIf { it.isNotEmpty() }?.let { add("${it.size} pregunta(s) de 1 a 5") }
            if (recommend == null) add("la pregunta de 0 a 10")
        }
}

data class FeedbackUiState(
    val user: User? = null,
    val tasks: List<UsabilityTask> = emptyList(),
    val form: FeedbackForm = FeedbackForm(),
    val myResponses: Int = 0,
    val canSeeResults: Boolean = false,
    val summary: FeedbackSummary? = null,
    val recentComments: List<FeedbackEntity> = emptyList(),
    val syncEnabled: Boolean = false,
    val pendingSync: Int = 0,
)

class FeedbackViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = EchoRepository
    private val feedback = AppGraph.feedback
    private val form = MutableStateFlow(FeedbackForm())

    val ui: StateFlow<FeedbackUiState> = combine(repo.session, form, feedback.all) { user, f, all ->
        val results = user?.role?.has(Permission.VIEW_TEST_RESULTS) == true
        FeedbackUiState(
            user = user,
            tasks = user?.let { UsabilityTasks.forRole(it.role) } ?: emptyList(),
            form = f,
            myResponses = all.count { it.feedback.userId == user?.username },
            canSeeResults = results,
            summary = if (results) FeedbackStats.summarize(all) else null,
            recentComments = if (results) all.map { it.feedback }
                .filter { it.liked.isNotBlank() || it.improvements.isNotBlank() || it.bugs.isNotBlank() }.take(8) else emptyList(),
            syncEnabled = feedback.syncEnabled,
            pendingSync = all.count { !it.feedback.synced },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedbackUiState())

    private var all: List<FeedbackWithTasks> = emptyList()

    init {
        viewModelScope.launch { feedback.all.collect { all = it } }
        feedback.sync()
    }

    fun setOutcome(taskId: String, o: TaskOutcome) = form.update { it.copy(outcomes = it.outcomes + (taskId to o), error = null) }
    fun setRating(q: RatingQuestion, v: Int) = form.update { it.copy(ratings = it.ratings + (q to v), error = null) }
    fun setRecommend(v: Int) = form.update { it.copy(recommend = v, error = null) }
    fun setLiked(v: String) = form.update { it.copy(liked = v.take(500)) }
    fun setImprovements(v: String) = form.update { it.copy(improvements = v.take(500)) }
    fun setBugs(v: String) = form.update { it.copy(bugs = v.take(500)) }

    fun submit() {
        val f = form.value
        val user = repo.session.value ?: return
        if (f.sending) return
        if (f.missing.isNotEmpty()) {
            form.update { it.copy(error = "Falta responder: " + f.missing.joinToString(" y ")) }
            return
        }
        form.update { it.copy(sending = true) }
        viewModelScope.launch {
            val entity = FeedbackEntity(
                createdAt = System.currentTimeMillis(),
                userId = user.username, userName = user.name, role = user.role.name, classroomId = user.classroomId,
                ease = f.ratings.getValue(RatingQuestion.EASE),
                clarity = f.ratings.getValue(RatingQuestion.CLARITY),
                confidence = f.ratings.getValue(RatingQuestion.CONFIDENCE),
                design = f.ratings.getValue(RatingQuestion.DESIGN),
                recommend = f.recommend ?: 0,
                liked = f.liked.trim(), improvements = f.improvements.trim(), bugs = f.bugs.trim(),
                appVersion = BuildConfig.VERSION_NAME,
                device = "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}",
            )
            val tasks = UsabilityTasks.forRole(user.role).map { t ->
                TaskResultEntity(taskId = t.id, outcome = (f.outcomes[t.id] ?: TaskOutcome.SKIPPED).name)
            }
            feedback.submit(entity, tasks)
            form.value = FeedbackForm(sent = true)
        }
    }

    fun newResponse() { form.value = FeedbackForm() }

    /** CSV de todas las respuestas (quien ve resultados) o solo las propias. */
    fun csv(): String {
        val user = repo.session.value
        val rows = if (user?.role?.has(Permission.VIEW_TEST_RESULTS) == true) all else all.filter { it.feedback.userId == user?.username }
        return FeedbackStats.toCsv(rows)
    }

    fun deleteAll() {
        if (!repo.can(Permission.VIEW_TEST_RESULTS)) return
        viewModelScope.launch {
            feedback.deleteAll()
            repo.recordAudit("Respuestas de prueba borradas")
        }
    }

    /** Deja el teléfono listo para el siguiente participante: demo reiniciada y sin perfil. */
    fun nextParticipant() {
        repo.resetDemo()
        repo.clearProfile()
        form.value = FeedbackForm()
    }

    fun resetDemo() {
        if (repo.can(Permission.RESET_DEMO)) repo.resetDemo()
    }
}
