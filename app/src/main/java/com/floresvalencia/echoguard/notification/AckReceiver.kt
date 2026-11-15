package com.floresvalencia.echoguard.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.floresvalencia.echoguard.data.repository.EchoRepository

/** Botón "Confirmar recepción" de la notificación: avisa a la brigada que la persona la vio. */
class AckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_ALERT_ID, -1)
        if (id < 0) return
        EchoRepository.confirmAlert(id)
        NotificationManagerCompat.from(context).cancel(id)
    }

    companion object {
        private const val EXTRA_ALERT_ID = "alert_id"

        fun pendingIntent(context: Context, alertId: Int): PendingIntent = PendingIntent.getBroadcast(
            context, alertId,
            Intent(context, AckReceiver::class.java).putExtra(EXTRA_ALERT_ID, alertId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
