package com.personale.messaggi

import android.app.Service
import android.content.Intent
import android.os.IBinder

class RispondiViaMessaggioService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val dati = intent?.data
        val testo = intent?.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        val numero = dati?.schemeSpecificPart?.substringBefore('?')

        if (!numero.isNullOrBlank() && !testo.isNullOrBlank()) {
            InvioSms.invia(applicationContext, numero, testo)
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
