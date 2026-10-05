package com.personale.messaggi

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.Telephony

object Messaggi {

    const val PAGINA = 200
    private const val ATTESA_MASSIMA_INVIO_MS = 5 * 60_000L

    /** Gli ultimi [limite] messaggi di una conversazione, in ordine cronologico. */
    fun diConversazione(context: Context, threadId: Long, limite: Int = PAGINA): List<Messaggio> {
        val proiezione = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.STATUS,
        )
        val lista = ArrayList<Messaggio>()
        // Dal più recente: ci fermiamo dopo [limite] righe anche se il provider ignorasse un LIMIT.
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI, proiezione,
            "${Telephony.Sms.THREAD_ID} = ?", arrayOf(threadId.toString()),
            "${Telephony.Sms.DATE} DESC",
        )?.use { c ->
            val iId = c.getColumnIndexOrThrow(Telephony.Sms._ID)
            val iBody = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val iDate = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val iType = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            val iStatus = c.getColumnIndexOrThrow(Telephony.Sms.STATUS)

            while (lista.size < limite && c.moveToNext()) {
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
                        consegnato = inviato && c.getInt(iStatus) == Telephony.Sms.STATUS_COMPLETE,
                    ),
                )
            }
        }
        lista.reverse()
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
        // Il sistema sa che +39333… e 333… sono lo stesso numero: la corrispondenza esatta no.
        try {
            val id = Telephony.Threads.getOrCreateThreadId(context, numero)
            if (id > 0) return id
        } catch (e: Exception) {
            // Ripiego sulla ricerca per indirizzo qui sotto.
        }
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI, arrayOf(Telephony.Sms.THREAD_ID),
            "${Telephony.Sms.ADDRESS} = ?", arrayOf(numero), "${Telephony.Sms.DATE} DESC",
        )?.use { c ->
            if (c.moveToFirst()) return c.getLong(c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID))
        }
        return null
    }

    /** Alcuni operatori consegnano lo stesso SMS due volte: stesso mittente, testo e orario del centro servizi. */
    fun esisteGia(context: Context, numero: String, corpo: String, data: Long): Boolean {
        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI, arrayOf(Telephony.Sms._ID),
            "${Telephony.Sms.ADDRESS} = ? AND ${Telephony.Sms.BODY} = ? AND ${Telephony.Sms.DATE} = ?",
            arrayOf(numero, corpo, data.toString()), null,
        )?.use { return it.count > 0 }
        return false
    }

    /** Se l'app o il telefono si fermano durante un invio, il messaggio resterebbe "in corso" per sempre. */
    fun segnaInSospesoComeFalliti(context: Context) {
        val valori = ContentValues().apply { put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_FAILED) }
        context.contentResolver.update(
            Telephony.Sms.CONTENT_URI, valori,
            "${Telephony.Sms.TYPE} IN (${Telephony.Sms.MESSAGE_TYPE_OUTBOX}, ${Telephony.Sms.MESSAGE_TYPE_QUEUED}) AND ${Telephony.Sms.DATE} < ?",
            arrayOf((System.currentTimeMillis() - ATTESA_MASSIMA_INVIO_MS).toString()),
        )
    }

    /** Elimina una conversazione intera. Funziona solo se siamo l'app SMS predefinita. */
    fun eliminaConversazione(context: Context, threadId: Long): Boolean = try {
        context.contentResolver.delete(
            Telephony.Sms.CONTENT_URI, "${Telephony.Sms.THREAD_ID} = ?", arrayOf(threadId.toString()),
        ) > 0
    } catch (e: SecurityException) {
        false
    }

    fun eliminaMessaggio(context: Context, id: Long): Boolean = try {
        context.contentResolver.delete(ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, id), null, null) > 0
    } catch (e: SecurityException) {
        false
    }

    /** Conversazioni (thread_id) con almeno un messaggio che contiene [testo]. */
    fun threadConTesto(context: Context, testo: String): Set<Long> {
        val trovati = HashSet<Long>()
        val ricerca = testo.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI, arrayOf(Telephony.Sms.THREAD_ID),
            "${Telephony.Sms.BODY} LIKE ? ESCAPE '\\'", arrayOf("%$ricerca%"), null,
        )?.use { c ->
            val i = c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            while (c.moveToNext()) trovati.add(c.getLong(i))
        }
        return trovati
    }
}
