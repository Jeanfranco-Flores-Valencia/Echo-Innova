package com.floresvalencia.echoguard

import android.content.Context
import com.floresvalencia.echoguard.data.db.EchoDatabase
import com.floresvalencia.echoguard.data.feedback.FeedbackRepository
import com.floresvalencia.echoguard.data.repository.EchoRepository
import com.floresvalencia.echoguard.data.repository.StaffRepository
import com.floresvalencia.echoguard.data.sos.SosRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Dependencias de la app (inyección manual, sin librerías).
 * EchoApplication lo inicializa antes de mostrar cualquier pantalla.
 */
object AppGraph {
    lateinit var db: EchoDatabase
        private set
    lateinit var staff: StaffRepository
        private set
    lateinit var feedback: FeedbackRepository
        private set
    lateinit var sos: SosRepository
        private set

    fun init(context: Context) {
        if (::db.isInitialized) return
        val app = context.applicationContext
        db = EchoDatabase.get(app)
        staff = StaffRepository(db)
        feedback = FeedbackRepository(db, BuildConfig.FEEDBACK_URL)
        sos = SosRepository(app, db)

        EchoRepository.init(app)
        EchoRepository.staffDirectory = staff
        EchoRepository.auditLog = staff
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            staff.seedIfEmpty()
            feedback.sync()
        }
    }
}
