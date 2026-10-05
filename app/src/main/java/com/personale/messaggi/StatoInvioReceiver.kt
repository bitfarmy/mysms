package com.personale.messaggi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager

/** Riceve, per ogni parte di un SMS inviato, l'esito dell'operazione. */
class StatoInvioReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val uriTesto = intent.getStringExtra(InvioSms.EXTRA_URI_MESSAGGIO) ?: return
        val uri = Uri.parse(uriTesto)

        if (resultCode == android.app.Activity.RESULT_OK) {
            // Con più parti, il messaggio è inviato solo quando l'ultima parte è confermata.
            val parte = intent.getIntExtra(InvioSms.EXTRA_PARTE, 0)
            val totali = intent.getIntExtra(InvioSms.EXTRA_PARTI_TOTALI, 1)
            if (parte >= totali - 1) InvioSms.segnaInviato(context, uri)
        } else {
            // Qualsiasi codice diverso da RESULT_OK (SmsManager.RESULT_ERROR_*) è un fallimento.
            InvioSms.segnaFallito(context, uri)
        }
    }
}
