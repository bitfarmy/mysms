package com.personale.messaggi

import android.app.Application

class MessaggiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifiche.creaCanali(this)
    }
}
