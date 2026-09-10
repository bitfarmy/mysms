package com.personale.messaggi

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/** Non ha interfaccia: gira solo il numero ricevuto verso ConversazioneActivity. */
class ComponiActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val numero = intent?.data?.schemeSpecificPart?.substringBefore('?') ?: ""
        val apri = Intent(this, ConversazioneActivity::class.java).apply {
            putExtra(ConversazioneActivity.EXTRA_NUMERO, numero)
        }
        startActivity(apri)
        finish()
    }
}
