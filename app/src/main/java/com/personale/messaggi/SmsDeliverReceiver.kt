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

        val valori = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, numero)
            put(Telephony.Sms.BODY, corpo)
            put(Telephony.Sms.DATE, data)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
            put(Telephony.Sms.READ, 0)
            put(Telephony.Sms.SEEN, 0)
        }
        context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, valori)

        val threadId = Messaggi.threadIdPerNumero(context, numero)
        val nome = Contatti.cerca(context, numero)?.nome

        Notifiche.mostraMessaggioRicevuto(context, threadId ?: -1L, numero, nome, corpo)
    }
}
