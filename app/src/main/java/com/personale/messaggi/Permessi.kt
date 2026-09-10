package com.personale.messaggi

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build

/** Solo API di sistema: niente androidx.core, coerente con "nessuna libreria esterna". */
object Permessi {

    const val CODICE_RICHIESTA = 501

    fun necessari(): Array<String> {
        val base = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_CONTACTS,
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            base + Manifest.permission.POST_NOTIFICATIONS
        } else {
            base
        }
    }

    fun tuttiConcessi(activity: Activity): Boolean = necessari().all {
        activity.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
    }

    fun richiedi(activity: Activity) {
        activity.requestPermissions(necessari(), CODICE_RICHIESTA)
    }
}
