package com.personale.messaggi

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsDeliverReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val parti = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (parti.isEmpty()) return

        val numero = parti[0].originatingAddress ?: return
        val corpo = parti.joinToString("") { it.messageBody ?: "" }
        val data = parti[0].timestampMillis

        if (Messaggi.esisteGia(context, numero, corpo, data)) return // doppione dell'operatore

        val bloccato = Preferenze(context).bloccato(numero)
        val valori = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, numero)
            put(Telephony.Sms.BODY, corpo)
            put(Telephony.Sms.DATE, data)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
            // Un numero bloccato viene conservato ma già letto e senza notifica.
            put(Telephony.Sms.READ, if (bloccato) 1 else 0)
            put(Telephony.Sms.SEEN, if (bloccato) 1 else 0)
        }
        context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, valori)
        if (bloccato) return

        val threadId = Messaggi.threadIdPerNumero(context, numero)
        val nome = Contatti.cerca(context, numero)?.nome

        Notifiche.mostraMessaggioRicevuto(context, threadId ?: -1L, numero, nome, corpo, Codici.estrai(corpo))
    }
}
