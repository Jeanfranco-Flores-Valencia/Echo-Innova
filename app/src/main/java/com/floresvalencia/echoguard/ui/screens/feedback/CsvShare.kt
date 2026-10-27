package com.floresvalencia.echoguard.ui.screens.feedback

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Guarda el CSV y abre el menú Compartir (WhatsApp, correo, Drive...). */
fun shareCsv(context: Context, csv: String, fileName: String = "echo-guard-opiniones.csv") {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, fileName).apply { writeText(csv, Charsets.UTF_8) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/csv")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_SUBJECT, "Opiniones de la prueba de Echo Guard")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, "Compartir respuestas"))
}
