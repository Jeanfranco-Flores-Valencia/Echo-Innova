package com.floresvalencia.baymaxecho.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.floresvalencia.baymaxecho.MainActivity
import com.floresvalencia.baymaxecho.R

/**
 * Notificación de alarma con "full-screen intent": con el teléfono bloqueado abre la app
 * directamente en la alarma (en Android 14+ el sistema puede mostrarla como aviso flotante
 * si el usuario no concede el permiso de pantalla completa).
 */
object AlarmNotifier {
    private const val CHANNEL_ID = "echo_alarm"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(CHANNEL_ID, "Alarmas de emergencia", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Evacuaciones y detecciones de los rovers"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 600)
            setBypassDnd(true)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
            )
        }
        manager.createNotificationChannel(channel)
    }

    fun notify(context: Context, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFFE60012.toInt())
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setFullScreenIntent(pending, true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Permiso revocado: la alarma dentro de la app sigue funcionando.
        }
    }
}
