package com.personale.messaggi

import android.app.PendingIntent
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager

object InvioSms {

    const val AZIONE_INVIATO = "com.personale.messaggi.SMS_INVIATO"
    const val AZIONE_CONSEGNATO = "com.personale.messaggi.SMS_CONSEGNATO"
    const val EXTRA_URI_MESSAGGIO = "uri_messaggio"

    const val EXTRA_PARTE = "parte"
    const val EXTRA_PARTI_TOTALI = "parti_totali"

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
        spedisci(context, uriMessaggio, numero, testo)
        return uriMessaggio
    }

    /** Ritenta un messaggio fallito riusando la stessa riga, senza crearne un duplicato. */
    fun riprova(context: Context, idMessaggio: Long, numero: String, testo: String) {
        val uri = ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, idMessaggio)
        val valori = ContentValues().apply {
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_OUTBOX)
            put(Telephony.Sms.DATE, System.currentTimeMillis())
            put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_NONE)
        }
        context.contentResolver.update(uri, valori, null, null)
        spedisci(context, uri, numero, testo)
    }

    private fun spedisci(context: Context, uriMessaggio: Uri, numero: String, testo: String) {
        val gestore = gestoreSms(context) ?: run {
            segnaFallito(context, uriMessaggio)
            return
        }

        val parti = gestore.divideMessage(testo)
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pendingIntents = ArrayList<PendingIntent>()
        val pendingConsegna = ArrayList<PendingIntent>()
        val salt = System.nanoTime()
        parti.forEachIndexed { i, _ ->
            fun creaIntent(ricevitore: Class<*>, azione: String) = Intent(context, ricevitore).apply {
                action = azione
                putExtra(EXTRA_URI_MESSAGGIO, uriMessaggio.toString())
                putExtra(EXTRA_PARTE, i)
                putExtra(EXTRA_PARTI_TOTALI, parti.size)
            }
            pendingIntents.add(
                PendingIntent.getBroadcast(
                    context, ("inv" + uriMessaggio + i + salt).hashCode(),
                    creaIntent(StatoInvioReceiver::class.java, AZIONE_INVIATO), PendingIntent.FLAG_UPDATE_CURRENT or flag,
                ),
            )
            pendingConsegna.add(
                PendingIntent.getBroadcast(
                    context, ("con" + uriMessaggio + i + salt).hashCode(),
                    creaIntent(StatoConsegnaReceiver::class.java, AZIONE_CONSEGNATO), PendingIntent.FLAG_UPDATE_CURRENT or flag,
                ),
            )
        }

        try {
            if (parti.size > 1) {
                gestore.sendMultipartTextMessage(numero, null, parti, pendingIntents, pendingConsegna)
            } else {
                gestore.sendTextMessage(numero, null, testo, pendingIntents.firstOrNull(), pendingConsegna.firstOrNull())
            }
        } catch (e: Exception) {
            segnaFallito(context, uriMessaggio)
        }
    }

    /** Passa a "inviato" solo se non è già fallito (una parte fallita rende fallito tutto il messaggio). */
    fun segnaInviato(context: Context, uriMessaggio: Uri) {
        val valori = ContentValues().apply { put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT) }
        context.contentResolver.update(
            uriMessaggio, valori, "${Telephony.Sms.TYPE} = ?", arrayOf(Telephony.Sms.MESSAGE_TYPE_OUTBOX.toString()),
        )
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
