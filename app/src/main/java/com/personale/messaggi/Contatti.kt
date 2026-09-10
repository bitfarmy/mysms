package com.personale.messaggi

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

object Contatti {

    private val cache = HashMap<String, Contatto?>()

    /** Nome e foto del contatto associato a un numero, oppure null se non è in rubrica. */
    fun cerca(context: Context, numero: String): Contatto? {
        cache[numero]?.let { return it }
        if (cache.containsKey(numero)) return null // era già stato cercato e non trovato

        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(numero))
        val proiezione = arrayOf(
            ContactsContract.PhoneLookup._ID,
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.PHOTO_URI,
        )
        var risultato: Contatto? = null
        try {
            context.contentResolver.query(uri, proiezione, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val nome = c.getString(c.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                    val foto = c.getString(c.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_URI))
                    risultato = Contatto(nome, foto)
                }
            }
        } catch (e: SecurityException) {
            // Permesso Contatti non ancora concesso: va bene, mostreremo solo il numero.
        }
        cache[numero] = risultato
        return risultato
    }

    fun svuotaCache() = cache.clear()
}
