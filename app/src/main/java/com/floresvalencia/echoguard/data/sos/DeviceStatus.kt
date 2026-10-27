package com.floresvalencia.echoguard.data.sos

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import android.os.PowerManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Batería y ubicación del teléfono. No usa internet ni Google Play Services. */
object DeviceStatus {

    fun battery(context: Context): BatteryInfo? {
        val bm = context.getSystemService(BatteryManager::class.java) ?: return null
        val percent = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (percent !in 0..100) return null
        val power = context.getSystemService(PowerManager::class.java)
        return BatteryInfo(percent, bm.isCharging, power?.isPowerSaveMode == true)
    }

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /**
     * Última ubicación conocida si es reciente; si no, pide una nueva (máx. [timeoutMs]).
     * El GPS funciona sin datos móviles: solo necesita cielo abierto o cerca de una ventana.
     */
    @SuppressLint("MissingPermission")
    suspend fun location(context: Context, timeoutMs: Long = 8_000): GeoFix? {
        if (!hasLocationPermission(context)) return null
        val lm = context.getSystemService(LocationManager::class.java) ?: return null
        val providers = lm.getProviders(true)
        val last = providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
        val now = System.currentTimeMillis()
        if (last != null && now - last.time < 2 * 60_000 && last.accuracy <= 100f) return last.toFix()

        val provider = when {
            LocationManager.GPS_PROVIDER in providers -> LocationManager.GPS_PROVIDER
            LocationManager.NETWORK_PROVIDER in providers -> LocationManager.NETWORK_PROVIDER
            else -> null
        } ?: return last?.toFix()

        val fresh = withTimeoutOrNull(timeoutMs) { currentLocation(context, lm, provider) }
        return (fresh ?: last)?.toFix()
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(context: Context, lm: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { cont ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val signal = CancellationSignal()
                lm.getCurrentLocation(provider, signal, context.mainExecutor) { loc -> if (cont.isActive) cont.resume(loc) }
                cont.invokeOnCancellation { signal.cancel() }
            } else {
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        lm.removeUpdates(this)
                        if (cont.isActive) cont.resume(location)
                    }
                }
                @Suppress("DEPRECATION")
                lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                cont.invokeOnCancellation { lm.removeUpdates(listener) }
            }
        }

    private fun Location.toFix() = GeoFix(latitude, longitude, if (hasAccuracy()) accuracy.toInt() else null, time)
}
