package com.floresvalencia.echoguard

import android.app.Application
import com.floresvalencia.echoguard.notification.AlarmNotifier

class EchoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Base de datos, perfil guardado y cuentas de personal antes de mostrar cualquier pantalla.
        AppGraph.init(this)
        AlarmNotifier.ensureChannel(this)
    }
}
