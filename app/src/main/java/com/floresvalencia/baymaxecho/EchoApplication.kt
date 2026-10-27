package com.floresvalencia.baymaxecho

import android.app.Application
import com.floresvalencia.baymaxecho.data.repository.EchoRepository
import com.floresvalencia.baymaxecho.notification.AlarmNotifier

class EchoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Carga el perfil guardado antes de mostrar cualquier pantalla.
        EchoRepository.init(this)
        AlarmNotifier.ensureChannel(this)
    }
}
