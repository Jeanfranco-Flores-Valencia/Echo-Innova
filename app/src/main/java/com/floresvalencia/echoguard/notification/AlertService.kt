package com.floresvalencia.echoguard.notification

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Servicio en primer plano: mantiene a Echo Guard atento a las alertas aunque la persona
 * cambie de app o apague la pantalla (recomendación del especialista de bomberos).
 * Muestra un aviso discreto y permanente que se puede detener.
 */
class AlertService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        AlertWatcher.start(this)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        try {
            ServiceCompat.startForeground(this, AlarmNotifier.MONITOR_NOTIFICATION_ID, AlarmNotifier.monitorNotification(this), type)
        } catch (_: Exception) {
            // Sin permiso de notificaciones o el sistema no lo permite ahora: las alertas siguen con la app abierta.
            stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val ACTION_STOP = "com.floresvalencia.echoguard.STOP_MONITOR"

        /** Llamar con la app en primer plano (Android no permite iniciarlo desde segundo plano). */
        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, AlertService::class.java))
            } catch (_: Exception) {
                // Restricción del sistema: se reintenta la próxima vez que se abra la app.
            }
        }

        fun stopIntent(context: Context): PendingIntent = PendingIntent.getService(
            context, 1, Intent(context, AlertService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
