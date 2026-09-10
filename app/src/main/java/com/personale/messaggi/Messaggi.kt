package com.personale.messaggi

import android.content.ContentValues
import android.content.Context
import android.provider.Telephony

object Messaggi {

    fun diConversazione(context: Context, threadId: Long): List<Messaggio> {
        val proiezione = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
        )
        val lista = ArrayList<Messaggio>()
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI, proiezione,
            "${Telephony.Sms.THREAD_ID} = ?", arrayOf(threadId.toString()),
            "${Telephony.Sms.DATE} ASC",
        )?.use { c ->
            val iId = c.getColumnIndexOrThrow(Telephony.Sms._ID)
            val iBody = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val iDate = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val iType = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)

            while (c.moveToNext()) {
                val tipo = c.getInt(iType)
                val inviato = tipo != Telephony.Sms.MESSAGE_TYPE_INBOX
                val stato = when (tipo) {
                    Telephony.Sms.MESSAGE_TYPE_INBOX -> StatoMessaggio.RICEVUTO
                    Telephony.Sms.MESSAGE_TYPE_OUTBOX, Telephony.Sms.MESSAGE_TYPE_QUEUED -> StatoMessaggio.IN_CORSO
                    Telephony.Sms.MESSAGE_TYPE_FAILED -> StatoMessaggio.FALLITO
                    else -> StatoMessaggio.INVIATO
                }
                lista.add(
                    Messaggio(
                        id = c.getLong(iId),
                        corpo = c.getString(iBody) ?: "",
                        data = c.getLong(iDate),
                        inviatoDaMe = inviato,
                        stato = stato,
                    ),
                )
            }
        }
        return lista
    }

    fun segnaComeLette(context: Context, threadId: Long) {
        val valori = ContentValues().apply { put(Telephony.Sms.READ, 1) }
        context.contentResolver.update(
            Telephony.Sms.CONTENT_URI, valori,
            "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0", arrayOf(threadId.toString()),
        )
    }

    /** Il thread_id di un numero, cercandolo tra i messaggi esistenti (utile dopo il primo invio). */
    fun threadIdPerNumero(context: Context, numero: String): Long? {
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI, arrayOf(Telephony.Sms.THREAD_ID),
            "${Telephony.Sms.ADDRESS} = ?", arrayOf(numero), "${Telephony.Sms.DATE} DESC",
        )?.use { c ->
            if (c.moveToFirst()) return c.getLong(c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID))
        }
        return null
    }
}
