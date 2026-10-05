package com.personale.messaggi

import android.os.Build
import android.view.View
import android.view.WindowInsets

/**
 * Da Android 15 con targetSdk 35 le app si disegnano sotto barra di stato, barra del titolo e barra dei gesti:
 * senza questo, il banner e la prima riga della lista finiscono nascosti. Qui li spingiamo dentro l'area visibile
 * (i margini di sistema comprendono già l'altezza della barra del titolo).
 */
object Bordi {

    fun applica(radice: View) {
        radice.setOnApplyWindowInsetsListener { v, insets ->
            val sinistra: Int
            val alto: Int
            val destra: Int
            val basso: Int
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Barre di sistema + tastiera (la tastiera spinge su il campo di testo).
                val i = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime())
                sinistra = i.left; alto = i.top; destra = i.right; basso = i.bottom
            } else {
                @Suppress("DEPRECATION")
                run {
                    sinistra = insets.systemWindowInsetLeft; alto = insets.systemWindowInsetTop
                    destra = insets.systemWindowInsetRight; basso = insets.systemWindowInsetBottom
                }
            }
            v.setPadding(sinistra, alto, destra, basso)
            WindowInsets.CONSUMED
        }
        radice.requestApplyInsets()
    }
}
