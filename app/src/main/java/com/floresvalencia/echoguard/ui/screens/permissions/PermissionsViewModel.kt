package com.floresvalencia.echoguard.ui.screens.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floresvalencia.echoguard.AppGraph
import com.floresvalencia.echoguard.data.model.AuditEntry
import com.floresvalencia.echoguard.data.model.Permission
import com.floresvalencia.echoguard.data.model.Role
import com.floresvalencia.echoguard.data.model.StaffAccount
import com.floresvalencia.echoguard.data.model.User
import com.floresvalencia.echoguard.data.repository.EchoRepository
import com.floresvalencia.echoguard.data.repository.StaffRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PermissionsUiState(
    val user: User? = null,
    val accounts: List<StaffAccount> = emptyList(),
    val audit: List<AuditEntry> = emptyList(),
) {
    val role: Role get() = user?.role ?: Role.GENERAL
    val canManage: Boolean get() = role.has(Permission.MANAGE_STAFF)
    val canAudit: Boolean get() = role.has(Permission.VIEW_AUDIT_LOG)
}

class PermissionsViewModel(
    private val repo: EchoRepository = EchoRepository,
    private val staff: StaffRepository = AppGraph.staff,
) : ViewModel() {

    val ui: StateFlow<PermissionsUiState> = combine(repo.session, staff.accounts, staff.audit) { u, accounts, audit ->
        val role = u?.role
        PermissionsUiState(
            user = u,
            // Los datos sensibles solo llegan a quien tiene el permiso.
            accounts = if (role?.has(Permission.MANAGE_STAFF) == true) accounts else emptyList(),
            audit = if (role?.has(Permission.VIEW_AUDIT_LOG) == true) audit.take(60) else emptyList(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PermissionsUiState(user = repo.session.value))

    val classrooms: List<String> get() = repo.classrooms.value.map { it.id }

    fun create(name: String, role: Role, classroomId: String?, pin: String, onResult: (String?) -> Unit) {
        viewModelScope.launch { onResult(staff.create(name, role, classroomId, pin)) }
    }

    fun setActive(username: String, active: Boolean, onResult: (String?) -> Unit) {
        viewModelScope.launch { onResult(staff.setActive(username, active)) }
    }

    fun changePin(username: String, pin: String, onResult: (String?) -> Unit) {
        viewModelScope.launch { onResult(staff.changePin(username, pin)) }
    }
}
