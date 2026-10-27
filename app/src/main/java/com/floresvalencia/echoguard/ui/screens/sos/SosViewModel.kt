package com.floresvalencia.echoguard.ui.screens.sos

import android.app.Application
import android.content.ActivityNotFoundException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.floresvalencia.echoguard.AppGraph
import com.floresvalencia.echoguard.data.db.EmergencyContactEntity
import com.floresvalencia.echoguard.data.db.SosLogEntity
import com.floresvalencia.echoguard.data.sos.BatteryInfo
import com.floresvalencia.echoguard.data.sos.DeviceStatus
import com.floresvalencia.echoguard.data.sos.GeoFix
import com.floresvalencia.echoguard.data.sos.SmsSender
import com.floresvalencia.echoguard.data.sos.SosKind
import com.floresvalencia.echoguard.data.sos.SosStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SosDraft(
    val kind: SosKind? = null,
    val locating: Boolean = false,
    val location: GeoFix? = null,
    val locationPermission: Boolean = false,
    val battery: BatteryInfo? = null,
    val preview: String = "",
    val sending: Boolean = false,
    val result: SosStatus? = null,
)

data class SosUiState(
    val contacts: List<EmergencyContactEntity> = emptyList(),
    val history: List<SosLogEntity> = emptyList(),
    val draft: SosDraft = SosDraft(),
)

class SosViewModel(app: Application) : AndroidViewModel(app) {
    private val sos = AppGraph.sos
    private val draft = MutableStateFlow(SosDraft())
    private var locateJob: Job? = null

    val ui: StateFlow<SosUiState> = combine(sos.contacts, sos.history, draft) { c, h, d -> SosUiState(c, h, d) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SosUiState())

    val maxContacts: Int get() = sos.maxContacts

    /** Al abrir el SOS: lee la batería y empieza a buscar la ubicación (así el envío es inmediato). */
    fun prepare() {
        val ctx = getApplication<Application>()
        val hasPermission = DeviceStatus.hasLocationPermission(ctx)
        draft.update { it.copy(battery = DeviceStatus.battery(ctx), locationPermission = hasPermission, result = null) }
        if (!hasPermission || locateJob?.isActive == true) return
        locateJob = viewModelScope.launch {
            draft.update { it.copy(locating = true) }
            val fix = DeviceStatus.location(ctx)
            draft.update { it.copy(locating = false, location = fix ?: it.location) }
            refreshPreview()
        }
    }

    fun select(kind: SosKind) {
        draft.update { it.copy(kind = kind, result = null) }
        refreshPreview()
    }

    private fun refreshPreview() {
        val d = draft.value
        val kind = d.kind ?: return
        draft.update { it.copy(preview = sos.buildMessage(kind, d.location, d.battery)) }
    }

    fun canSendDirectly(): Boolean = SmsSender.canSendDirectly(getApplication())

    /** SMS directo desde la app (con permiso). */
    fun sendDirect(onDone: (SosStatus) -> Unit) {
        val d = draft.value
        val kind = d.kind ?: return
        if (d.sending) return
        draft.update { it.copy(sending = true) }
        viewModelScope.launch {
            refreshPreview()
            val status = sos.sendDirect(kind, draft.value.preview, sos.contactsNow())
            draft.update { it.copy(sending = false, result = status) }
            onDone(status)
        }
    }

    /** Sin permiso de SMS: abre la app de mensajes con todo escrito (también funciona sin internet). */
    fun sendWithSmsApp(onError: (String) -> Unit) {
        val d = draft.value
        val kind = d.kind ?: return
        viewModelScope.launch {
            val contacts = sos.contactsNow()
            try {
                getApplication<Application>().startActivity(SmsSender.composeIntent(contacts.map { it.phone }, d.preview))
                sos.logHandoff(kind, d.preview, contacts)
                draft.update { it.copy(result = SosStatus.HANDED_TO_SMS_APP) }
            } catch (_: ActivityNotFoundException) {
                onError("Este teléfono no tiene app de mensajes")
            }
        }
    }

    fun reset() { draft.update { SosDraft(location = it.location, battery = it.battery, locationPermission = it.locationPermission) } }

    fun addContact(name: String, phone: String, relation: String, onResult: (String?) -> Unit) {
        viewModelScope.launch { onResult(sos.addContact(name, phone, relation)) }
    }

    fun deleteContact(id: Long) { viewModelScope.launch { sos.deleteContact(id) } }
}
