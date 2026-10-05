package com.personale.messaggi

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class Tema(
    val id: String,
    val nome: String,
    val sfondo: Int,
    val superficie: Int,
    /** 0 = nessun bordo (stile moderno). Un colore = bordo visibile (stile vintage). */
    val bordo: Int,
    val testo: Int,
    val testoSecondario: Int,
    val accento: Int,
    val testoSuAccento: Int,
    val bannerSfondo: Int,
    val bannerTesto: Int,
    /** Raggio degli angoli delle bolle, in dp. 0 = squadrato, fedele agli stili vintage. */
    val angoloBolla: Float,
    /** Colore della riga divisoria tra una conversazione e l'altra. */
    val divisore: Int,
)

/** Aggiungi un tema alla lista: comparirà da solo nel menu "Tema". */
object Temi {
    val tutti: List<Tema> = listOf(
        Tema(
            "chiaro", "Chiaro",
            sfondo = 0xFFFFFFFF.toInt(), superficie = 0xFFE4E6EB.toInt(), bordo = 0x00000000,
            testo = 0xFF1F1F1F.toInt(), testoSecondario = 0xFF6B6F75.toInt(),
            accento = 0xFF1A73E8.toInt(), testoSuAccento = 0xFFFFFFFF.toInt(),
            bannerSfondo = 0xFFE8F0FE.toInt(), bannerTesto = 0xFF1A73E8.toInt(),
            angoloBolla = 14f, divisore = 0xFFDADCE0.toInt(),
        ),
        Tema(
            "scuro", "Scuro",
            sfondo = 0xFF121212.toInt(), superficie = 0xFF2A2A2E.toInt(), bordo = 0x00000000,
            testo = 0xFFF2F2F2.toInt(), testoSecondario = 0xFF9A9AA3.toInt(),
            accento = 0xFF4C8DFF.toInt(), testoSuAccento = 0xFF0B1220.toInt(),
            bannerSfondo = 0xFF2A2210.toInt(), bannerTesto = 0xFFFFD54F.toInt(),
            angoloBolla = 14f, divisore = 0xFF3A3A3E.toInt(),
        ),
        // Teal del desktop, grigio "Silver" delle finestre, blu navy della selezione, giallo degli avvisi.
        Tema(
            "windows95", "Windows 95",
            sfondo = 0xFF008080.toInt(), superficie = 0xFFC0C0C0.toInt(), bordo = 0xFF808080.toInt(),
            testo = 0xFF000000.toInt(), testoSecondario = 0xFF404040.toInt(),
            accento = 0xFF000080.toInt(), testoSuAccento = 0xFFFFFFFF.toInt(),
            bannerSfondo = 0xFFFFFF00.toInt(), bannerTesto = 0xFF000000.toInt(),
            angoloBolla = 0f, divisore = 0xFF808080.toInt(),
        ),
        // Grigio "Platinum" e blu di selezione di System 7 / Mac OS 8-9.
        Tema(
            "mac_classico", "Mac Classico",
            sfondo = 0xFFDDDDDD.toInt(), superficie = 0xFFFFFFFF.toInt(), bordo = 0xFF999999.toInt(),
            testo = 0xFF000000.toInt(), testoSecondario = 0xFF666666.toInt(),
            accento = 0xFF3366CC.toInt(), testoSuAccento = 0xFFFFFFFF.toInt(),
            bannerSfondo = 0xFFFFFFCC.toInt(), bannerTesto = 0xFF000000.toInt(),
            angoloBolla = 0f, divisore = 0xFF999999.toInt(),
        ),
    )

    val predefinito: Tema get() = tutti.first()

    fun perId(id: String): Tema = tutti.firstOrNull { it.id == id } ?: predefinito
}

class Preferenze(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("impostazioni", Context.MODE_PRIVATE)

    // I numeri bloccati non vengono salvati: ne resta solo un'impronta (PBKDF2 con sale casuale
    // di questa installazione). L'app riconosce un mittente bloccato ma l'elenco non rivela i numeri.
    private val sale: ByteArray by lazy {
        val salvato = sp.getString("sale", null)
        if (salvato != null) {
            Base64.decode(salvato, Base64.NO_WRAP)
        } else {
            ByteArray(16).also {
                SecureRandom().nextBytes(it)
                sp.edit().putString("sale", Base64.encodeToString(it, Base64.NO_WRAP)).apply()
            }
        }
    }

    private fun impronta(numero: String): String {
        val chiave = Numeri.chiave(numero)
        return cacheImpronte.getOrPut(chiave) {
            val spec = PBEKeySpec(chiave.toCharArray(), sale, 10_000, 256)
            val derivata = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            derivata.joinToString("") { "%02x".format(it) }
        }
    }

    private var blocchi: Set<String>
        get() {
            migraVecchioElenco()
            return sp.getStringSet("blocchi", emptySet()) ?: emptySet()
        }
        set(valore) = sp.edit().putStringSet("blocchi", HashSet(valore)).apply()

    fun bloccato(numero: String): Boolean {
        val b = blocchi
        return b.isNotEmpty() && Numeri.chiave(numero).isNotEmpty() && impronta(numero) in b
    }

    fun blocca(numero: String) {
        if (Numeri.chiave(numero).isNotEmpty()) blocchi = blocchi + impronta(numero)
    }

    fun sblocca(numero: String) {
        blocchi = blocchi - impronta(numero)
    }

    /** Le versioni precedenti salvavano i numeri in chiaro: li trasformiamo in impronte e cancelliamo l'originale. */
    private fun migraVecchioElenco() {
        val vecchi = sp.getStringSet("bloccati", null) ?: return
        val nuovi = (sp.getStringSet("blocchi", emptySet()) ?: emptySet()) + vecchi.filter { Numeri.chiave(it).isNotEmpty() }.map { impronta(it) }
        sp.edit().putStringSet("blocchi", HashSet(nuovi)).remove("bloccati").apply()
    }

    private companion object {
        val cacheImpronte = ConcurrentHashMap<String, String>()
    }

    var tema: String
        get() = sp.getString("tema", null) ?: "chiaro"
        set(valore) = sp.edit().putString("tema", valore).apply()
}
