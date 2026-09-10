package com.personale.messaggi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/** Solo l'app SMS predefinita riceve questo broadcast: qui arrivano davvero i messaggi. */
class SmsDeliverReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val parti = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (parti.isEmpty()) return

        // Più SMS consecutivi con lo stesso mittente ravvicinati nel tempo vengono uniti in un solo messaggio.
        val numero = parti[0].originatingAddress ?: return
        val corpo = parti.joinToString("") { it.messageBody ?: "" }
        val data = parti[0].timestampMillis

        val uri = Telephony.Sms.Inbox.addMessage(
            context.contentResolver, numero, corpo, null, data,
        )

        val threadId = uri?.let { Messaggi.threadIdPerNumero(context, numero) }
        val nome = Contatti.cerca(context, numero)?.nome

        Notifiche.mostraMessaggioRicevuto(context, threadId ?: -1L, numero, nome, corpo)
    }
}
