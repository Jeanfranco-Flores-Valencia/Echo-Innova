package com.floresvalencia.echoguard.notification

import android.content.Context
import com.floresvalencia.echoguard.data.model.isVisibleTo
import com.floresvalencia.echoguard.data.repository.EchoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Vigila las alertas a nivel de proceso (no de pantalla): mientras la app esté viva,
 * aunque esté en segundo plano, cada alerta crítica nueva llega como notificación.
 * AlertService mantiene el proceso vivo.
 */
object AlertWatcher {
    private var started = false
    private val notified = mutableSetOf<Int>()

    fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        val repo = EchoRepository
        AlarmNotifier.ensureChannel(app)
        notified += repo.alerts.value.map { it.id }
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            combine(repo.alerts, repo.session) { a, u -> a to u }.collect { (alerts, user) ->
                if (user == null) return@collect
                alerts.filter { it.active && it.critical && it.id !in notified && it.createdBy != user.username && it.isVisibleTo(user) }
                    .forEach { a ->
                        notified += a.id
                        AlarmNotifier.notify(app, a.id, a.title, a.detail)
                    }
            }
        }
    }
}
