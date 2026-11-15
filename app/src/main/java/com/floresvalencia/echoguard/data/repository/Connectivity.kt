package com.floresvalencia.echoguard.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** ¿Hay internet ahora? Para avisar que el SOS por SMS, el mapa y la guía siguen funcionando sin él. */
object Connectivity {
    private val _online = MutableStateFlow(true)
    val online: StateFlow<Boolean> = _online.asStateFlow()

    fun start(context: Context) {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
        fun check() {
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            _online.value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        }
        check()
        try {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = check()
                override fun onLost(network: Network) { _online.value = false }
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    _online.value = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                }
            })
        } catch (_: SecurityException) {
            // Sin permiso de estado de red: se asume conexión y no se muestra el aviso.
        }
    }
}
