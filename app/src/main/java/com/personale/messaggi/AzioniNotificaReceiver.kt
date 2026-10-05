package com.personale.messaggi

import android.app.NotificationManager
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast

/** Le azioni dei pulsanti nelle notifiche: copia codice, segna come letto, rispondi. */
class AzioniNotificaReceiver : BroadcastReceiver() {

    companion object {
        const val COPIA = "com.personale.messaggi.COPIA_CODICE"
        const val SEGNA_LETTO = "com.personale.messaggi.SEGNA_LETTO"
        const val RISPONDI = "com.personale.messaggi.RISPONDI"
        const val EXTRA_THREAD_ID = "thread_id"
        const val EXTRA_NUMERO = "numero"
        const val EXTRA_CODICE = "codice"
        const val EXTRA_ID_NOTIFICA = "id_notifica"
        const val CHIAVE_RISPOSTA = "risposta"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val threadId = intent.getLongExtra(EXTRA_THREAD_ID, -1)
        val idNotifica = intent.getIntExtra(EXTRA_ID_NOTIFICA, -1)

        when (intent.action) {
            COPIA -> {
                val codice = intent.getStringExtra(EXTRA_CODICE) ?: return
                val appunti = context.getSystemService(ClipboardManager::class.java)
                val clip = ClipData.newPlainText("Codice", codice)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Chiede al sistema di non mostrare il codice nell'anteprima degli appunti.
                    clip.description.extras = PersistableBundle().apply {
                        putBoolean("android.content.extra.IS_SENSITIVE", true)
                    }
                }
                appunti.setPrimaryClip(clip)
                Toast.makeText(context, "Codice copiato", Toast.LENGTH_SHORT).show()
            }
            SEGNA_LETTO -> if (threadId > 0) Messaggi.segnaComeLette(context, threadId)
            RISPONDI -> {
                val numero = intent.getStringExtra(EXTRA_NUMERO) ?: return
                val testo = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(CHIAVE_RISPOSTA)?.toString()?.trim()
                if (testo.isNullOrEmpty()) return
                InvioSms.invia(context, numero, testo)
                if (threadId > 0) Messaggi.segnaComeLette(context, threadId)
            }
            else -> return
        }
        if (idNotifica >= 0) context.getSystemService(NotificationManager::class.java)?.cancel(idNotifica)
    }
}
