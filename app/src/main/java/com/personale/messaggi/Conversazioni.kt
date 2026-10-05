package com.personale.messaggi

import android.content.Context
import android.net.Uri
import android.provider.Telephony

object Conversazioni {

    /**
     * Un elemento per ogni conversazione, con l'ultimo messaggio come anteprima.
     * [nascondi] esclude i mittenti bloccati.
     */
    fun elenco(context: Context, nascondi: (String) -> Boolean = { false }): List<Conversazione> {
        val veloce = try {
            veloce(context)
        } catch (e: Exception) {
            null
        }
        val lista = if (!veloce.isNullOrEmpty()) veloce else completo(context)
        return lista.filter { !nascondi(it.numero) }.sortedByDescending { it.data }
    }

    /**
     * Percorso rapido: la tabella delle conversazioni del sistema ha già l'ultimo messaggio di ognuna,
     * quindi non serve leggere tutti gli SMS. Ritorna null se il telefono non risponde come previsto
     * (allora si usa [completo]).
     */
    private fun veloce(context: Context): List<Conversazione>? {
        val indirizzi = HashMap<String, String>()
        context.contentResolver.query(
            Uri.parse("content://mms-sms/canonical-addresses"), arrayOf("_id", "address"), null, null, null,
        )?.use { c ->
            while (c.moveToNext()) indirizzi[c.getString(0)] = c.getString(1) ?: continue
        } ?: return null

        val uri = Telephony.Threads.CONTENT_URI.buildUpon().appendQueryParameter("simple", "true").build()
        val risultato = ArrayList<Conversazione>()
        context.contentResolver.query(
            uri,
            arrayOf(Telephony.Threads._ID, Telephony.Threads.DATE, Telephony.Threads.SNIPPET, Telephony.Threads.READ, Telephony.Threads.RECIPIENT_IDS),
            null, null, "${Telephony.Threads.DATE} DESC",
        )?.use { c ->
            while (c.moveToNext()) {
                val primoDestinatario = (c.getString(4) ?: "").trim().split(" ").firstOrNull() ?: return null
                val numero = indirizzi[primoDestinatario] ?: return null
                risultato.add(
                    Conversazione(
                        threadId = c.getLong(0),
                        numero = numero,
                        nome = Contatti.cerca(context, numero)?.nome,
                        ultimoTesto = c.getString(2) ?: "",
                        data = c.getLong(1),
                        nonLetta = c.getInt(3) == 0,
                    ),
                )
            }
        } ?: return null
        return risultato
    }

    /** Percorso sicuro: legge tutti gli SMS e tiene il più recente di ogni thread_id. */
    private fun completo(context: Context): List<Conversazione> {
        val proiezione = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ,
            Telephony.Sms.TYPE,
        )
        val risultato = LinkedHashMap<Long, Conversazione>()

        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI, proiezione, null, null, "${Telephony.Sms.DATE} DESC",
        )?.use { c ->
            val iThread = c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val iAddress = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val iBody = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val iDate = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val iRead = c.getColumnIndexOrThrow(Telephony.Sms.READ)
            val iType = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)

            while (c.moveToNext()) {
                val threadId = c.getLong(iThread)
                val esistente = risultato[threadId]
                val nonLettoQui = c.getInt(iRead) == 0 && c.getInt(iType) == Telephony.Sms.MESSAGE_TYPE_INBOX

                if (esistente == null) {
                    // Il primo che troviamo per questo thread è il più recente (ordinati per data DESC).
                    val numero = c.getString(iAddress) ?: continue
                    risultato[threadId] = Conversazione(
                        threadId = threadId,
                        numero = numero,
                        nome = Contatti.cerca(context, numero)?.nome,
                        ultimoTesto = c.getString(iBody) ?: "",
                        data = c.getLong(iDate),
                        nonLetta = nonLettoQui,
                    )
                } else if (nonLettoQui && !esistente.nonLetta) {
                    // Un messaggio più vecchio nello stesso thread è non letto: la conversazione lo è comunque.
                    risultato[threadId] = esistente.copy(nonLetta = true)
                }
            }
        }
        return risultato.values.toList()
    }
}
