package com.personale.messaggi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Android richiede questo ricevitore per proporci come app SMS predefinita,
 * anche se questa versione non mostra il contenuto degli MMS (foto, video).
 * Ci limitiamo ad avvisare che è arrivato qualcosa che non sappiamo aprire.
 */
class MmsWapPushReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Notifiche.mostraMmsNonSupportato(context)
    }
}
