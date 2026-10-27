package com.floresvalencia.baymaxecho.ui.screens.classroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floresvalencia.baymaxecho.data.model.Classroom
import com.floresvalencia.baymaxecho.data.model.RollStatus
import com.floresvalencia.baymaxecho.data.repository.EchoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ClassroomUiState(
    val classroom: Classroom? = null,
    val emergencyActive: Boolean = false,
)

class ClassroomViewModel(private val repo: EchoRepository = EchoRepository) : ViewModel() {
    val ui: StateFlow<ClassroomUiState> = combine(repo.session, repo.classrooms, repo.emergency) { u, rooms, e ->
        ClassroomUiState(rooms.firstOrNull { it.id == u?.classroomId }, e.active)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClassroomUiState())

    fun reportComplete() {
        ui.value.classroom?.let { repo.setRoll(it.id, RollStatus.COMPLETE, 0) }
    }

    fun reportMissing(n: Int) {
        ui.value.classroom?.let { repo.setRoll(it.id, if (n > 0) RollStatus.MISSING else RollStatus.COMPLETE, n) }
    }

    fun toggleAssist(personId: Int) {
        ui.value.classroom?.let { repo.toggleAssistLocated(it.id, personId) }
    }
}
