package com.personale.messaggi

import android.app.Application

class MessaggiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifiche.creaCanali(this)
        // Fuori dal thread principale; senza essere l'app predefinita il sistema rifiuta la scrittura, e va bene.
        Thread {
            try {
                Messaggi.segnaInSospesoComeFalliti(this)
            } catch (e: Exception) {
            }
        }.start()
    }
}
