package com.floresvalencia.baymaxecho.ui.screens.access

import androidx.lifecycle.ViewModel
import com.floresvalencia.baymaxecho.data.model.HazardType
import com.floresvalencia.baymaxecho.data.repository.EchoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class RegisterUiState(
    val name: String = "",
    val student: Boolean? = null,
    val classroomId: String? = null,
    val error: String? = null,
) {
    val canContinue: Boolean
        get() = name.isNotBlank() && student != null && (student == false || classroomId != null)
}

/** Registro único del perfil regular (sin PIN). */
class RegisterViewModel(private val repo: EchoRepository = EchoRepository) : ViewModel() {
    private val _ui = MutableStateFlow(RegisterUiState())
    val ui: StateFlow<RegisterUiState> = _ui.asStateFlow()

    val classrooms: List<String> get() = repo.classrooms.value.map { it.id }

    fun onName(v: String) = _ui.update { it.copy(name = v.take(40), error = null) }
    fun onStudent(v: Boolean) = _ui.update { it.copy(student = v, classroomId = if (v) it.classroomId else null) }
    fun onClassroom(id: String) = _ui.update { it.copy(classroomId = id) }

    /** La navegación la decide EchoApp al observar la sesión. */
    fun register() {
        val s = _ui.value
        if (!s.canContinue) {
            _ui.update { it.copy(error = "Completa tu nombre y tu tipo de perfil") }
            return
        }
        repo.register(s.name, s.student == true, s.classroomId)
    }

    fun reportAnonymous(type: HazardType) = repo.reportEmergency(null, type)
}

data class StaffUiState(val pin: String = "", val error: Boolean = false)

/** Acceso de personal: docente, brigadista o dirección. */
class StaffAccessViewModel(private val repo: EchoRepository = EchoRepository) : ViewModel() {
    private val _ui = MutableStateFlow(StaffUiState())
    val ui: StateFlow<StaffUiState> = _ui.asStateFlow()

    /** Devuelve true si el PIN es válido (la sesión pasa a modo personal). */
    fun onDigit(d: Char): Boolean {
        val pin = (_ui.value.pin + d).take(4)
        _ui.update { StaffUiState(pin = pin) }
        if (pin.length < 4) return false
        return if (repo.enterStaffMode(pin) != null) {
            _ui.value = StaffUiState()
            true
        } else {
            _ui.value = StaffUiState(error = true)
            false
        }
    }

    fun onDelete() = _ui.update { it.copy(pin = it.pin.dropLast(1), error = false) }
}
