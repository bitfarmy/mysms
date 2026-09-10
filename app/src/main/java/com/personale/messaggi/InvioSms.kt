package com.personale.messaggi

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager

object InvioSms {

    const val AZIONE_INVIATO = "com.personale.messaggi.SMS_INVIATO"
    const val EXTRA_URI_MESSAGGIO = "uri_messaggio"

    /** Scrive subito il messaggio come "in corso" nel provider, poi lo invia davvero. Ritorna il suo Uri. */
    fun invia(context: Context, numero: String, testo: String): Uri? {
        val valori = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, numero)
            put(Telephony.Sms.BODY, testo)
            put(Telephony.Sms.DATE, System.currentTimeMillis())
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_OUTBOX)
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
        }
        val uriMessaggio = context.contentResolver.insert(Telephony.Sms.CONTENT_URI, valori) ?: return null

        val gestore = gestoreSms(context) ?: run {
            segnaFallito(context, uriMessaggio)
            return uriMessaggio
        }

        val parti = gestore.divideMessage(testo)
        val intentInvio = Intent(context, StatoInvioReceiver::class.java).apply {
            action = AZIONE_INVIATO
            putExtra(EXTRA_URI_MESSAGGIO, uriMessaggio.toString())
        }
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pendingIntents = ArrayList<PendingIntent>()
        parti.forEachIndexed { i, _ ->
            pendingIntents.add(
                PendingIntent.getBroadcast(context, (uriMessaggio.toString() + i).hashCode(), intentInvio, PendingIntent.FLAG_UPDATE_CURRENT or flag),
            )
        }

        try {
            if (parti.size > 1) {
                gestore.sendMultipartTextMessage(numero, null, parti, pendingIntents, null)
            } else {
                gestore.sendTextMessage(numero, null, testo, pendingIntents.firstOrNull(), null)
            }
        } catch (e: Exception) {
            segnaFallito(context, uriMessaggio)
        }
        return uriMessaggio
    }

    fun segnaInviato(context: Context, uriMessaggio: Uri) {
        val valori = ContentValues().apply { put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT) }
        context.contentResolver.update(uriMessaggio, valori, null, null)
    }

    fun segnaFallito(context: Context, uriMessaggio: Uri) {
        val valori = ContentValues().apply { put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_FAILED) }
        context.contentResolver.update(uriMessaggio, valori, null, null)
    }

    private fun gestoreSms(context: Context): SmsManager? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
    } catch (e: Exception) {
        null
    }
}
