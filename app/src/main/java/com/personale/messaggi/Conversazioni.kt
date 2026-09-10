package com.personale.messaggi

import android.content.Context
import android.provider.Telephony

object Conversazioni {

    /**
     * Un elemento per ogni conversazione, con l'ultimo messaggio come anteprima.
     * Costruito leggendo tutti gli SMS ordinati per data, tenendo il più recente di ogni thread_id.
     */
    fun elenco(context: Context): List<Conversazione> {
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
        return risultato.values.sortedByDescending { it.data }
    }
}
