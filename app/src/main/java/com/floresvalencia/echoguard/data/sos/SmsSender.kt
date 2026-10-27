package com.floresvalencia.echoguard.data.sos

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

/**
 * Envío de SMS. Un SMS NO necesita internet: basta con señal del operador.
 * Si la app no tiene permiso de SMS, se abre la app de mensajes con el texto listo.
 */
object SmsSender {
    private const val ACTION_SENT = "com.floresvalencia.echoguard.SMS_SENT"
    private val requestCodes = AtomicInteger(4_000)

    fun canSendDirectly(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    /** Intent para abrir la app de mensajes con destinatarios y texto ya escritos. */
    fun composeIntent(phones: List<String>, text: String): Intent =
        Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + phones.joinToString(";")))
            .putExtra("sms_body", text)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Envía el texto a cada número. Devuelve true/false por número (en el mismo orden). */
    suspend fun send(context: Context, phones: List<String>, text: String): List<Boolean> = coroutineScope {
        val sms = smsManager(context) ?: return@coroutineScope phones.map { false }
        val parts = sms.divideMessage(text)
        phones.map { phone -> async { sendOne(context, sms, phone, parts) } }.awaitAll()
    }

    @Suppress("DEPRECATION")
    private fun smsManager(context: Context): SmsManager? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) context.getSystemService(SmsManager::class.java) else SmsManager.getDefault()
    }.getOrNull()

    private suspend fun sendOne(context: Context, sms: SmsManager, phone: String, parts: ArrayList<String>): Boolean =
        withTimeoutOrNull(30_000) {
            suspendCancellableCoroutine { cont ->
                val code = requestCodes.incrementAndGet()
                val action = "$ACTION_SENT.$code"
                val done = AtomicBoolean(false)
                lateinit var receiver: BroadcastReceiver
                fun finish(ok: Boolean) {
                    if (!done.compareAndSet(false, true)) return
                    runCatching { context.unregisterReceiver(receiver) }
                    if (cont.isActive) cont.resume(ok)
                }
                receiver = object : BroadcastReceiver() {
                    override fun onReceive(c: Context, intent: Intent) = finish(resultCode == Activity.RESULT_OK)
                }
                ContextCompat.registerReceiver(context, receiver, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED)
                val sentIntent = PendingIntent.getBroadcast(
                    context, code, Intent(action).setPackage(context.packageName), PendingIntent.FLAG_IMMUTABLE,
                )
                // El resultado de la última parte indica si el mensaje completo salió.
                val sent = ArrayList<PendingIntent?>(parts.size).apply {
                    repeat(parts.size - 1) { add(null) }
                    add(sentIntent)
                }
                try {
                    sms.sendMultipartTextMessage(phone, null, parts, sent, null)
                } catch (_: Exception) {
                    finish(false)
                }
                cont.invokeOnCancellation {
                    if (done.compareAndSet(false, true)) runCatching { context.unregisterReceiver(receiver) }
                }
            }
        } ?: false
}
