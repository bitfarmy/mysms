package com.personale.messaggi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Build

object Notifiche {

    private const val CANALE_MESSAGGI = "messaggi"
    private var prossimoId = 1000

    fun creaCanali(context: Context) {
        val gestore = context.getSystemService(NotificationManager::class.java) ?: return
        val canale = NotificationChannel(
            CANALE_MESSAGGI, "Messaggi", NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifiche per i nuovi SMS ricevuti"
        }
        gestore.createNotificationChannel(canale)
    }

    fun mostraMessaggioRicevuto(context: Context, threadId: Long, numero: String, nome: String?, testo: String) {
        val apri = Intent(context, ConversazioneActivity::class.java).apply {
            putExtra(ConversazioneActivity.EXTRA_THREAD_ID, threadId)
            putExtra(ConversazioneActivity.EXTRA_NUMERO, numero)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pending = PendingIntent.getActivity(context, threadId.toInt(), apri, PendingIntent.FLAG_UPDATE_CURRENT or flag)

        val notifica = Notification.Builder(context, CANALE_MESSAGGI)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setContentTitle(nome ?: numero)
            .setContentText(testo)
            .setStyle(Notification.BigTextStyle().bigText(testo))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        posta(context, threadId.toInt().takeIf { threadId > 0 } ?: prossimoId++, notifica)
    }

    fun mostraMmsNonSupportato(context: Context) {
        val notifica = Notification.Builder(context, CANALE_MESSAGGI)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setContentTitle("Messaggio multimediale ricevuto")
            .setContentText("Questa versione dell'app non mostra ancora foto e video negli MMS.")
            .setAutoCancel(true)
            .build()
        posta(context, prossimoId++, notifica)
    }

    private fun posta(context: Context, id: Int, notifica: Notification) {
        val gestore = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }
        gestore.notify(id, notifica)
    }
}
