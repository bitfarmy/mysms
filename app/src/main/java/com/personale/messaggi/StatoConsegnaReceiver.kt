package com.personale.messaggi

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony

/** Riceve la conferma di consegna al destinatario (dove l'operatore la fornisce). */
class StatoConsegnaReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val uri = Uri.parse(intent.getStringExtra(InvioSms.EXTRA_URI_MESSAGGIO) ?: return)
        val parte = intent.getIntExtra(InvioSms.EXTRA_PARTE, 0)
        val totali = intent.getIntExtra(InvioSms.EXTRA_PARTI_TOTALI, 1)

        if (resultCode != Activity.RESULT_OK) {
            aggiorna(context, uri, Telephony.Sms.STATUS_FAILED, null)
        } else if (parte >= totali - 1) {
            // "Consegnato" non deve sovrascrivere un fallimento già registrato.
            aggiorna(context, uri, Telephony.Sms.STATUS_COMPLETE, Telephony.Sms.STATUS_FAILED)
        }
    }

    private fun aggiorna(context: Context, uri: Uri, stato: Int, tranne: Int?) {
        val valori = ContentValues().apply { put(Telephony.Sms.STATUS, stato) }
        if (tranne == null) {
            context.contentResolver.update(uri, valori, null, null)
        } else {
            context.contentResolver.update(uri, valori, "${Telephony.Sms.STATUS} != ?", arrayOf(tranne.toString()))
        }
    }
}
