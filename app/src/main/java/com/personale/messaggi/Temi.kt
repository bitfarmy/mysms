package com.personale.messaggi

import android.content.Context

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
            angoloBolla = 14f,
        ),
        Tema(
            "scuro", "Scuro",
            sfondo = 0xFF121212.toInt(), superficie = 0xFF2A2A2E.toInt(), bordo = 0x00000000,
            testo = 0xFFF2F2F2.toInt(), testoSecondario = 0xFF9A9AA3.toInt(),
            accento = 0xFF4C8DFF.toInt(), testoSuAccento = 0xFF0B1220.toInt(),
            bannerSfondo = 0xFF2A2210.toInt(), bannerTesto = 0xFFFFD54F.toInt(),
            angoloBolla = 14f,
        ),
        // Teal del desktop, grigio "Silver" delle finestre, blu navy della selezione, giallo degli avvisi.
        Tema(
            "windows95", "Windows 95",
            sfondo = 0xFF008080.toInt(), superficie = 0xFFC0C0C0.toInt(), bordo = 0xFF808080.toInt(),
            testo = 0xFF000000.toInt(), testoSecondario = 0xFF404040.toInt(),
            accento = 0xFF000080.toInt(), testoSuAccento = 0xFFFFFFFF.toInt(),
            bannerSfondo = 0xFFFFFF00.toInt(), bannerTesto = 0xFF000000.toInt(),
            angoloBolla = 0f,
        ),
        // Grigio "Platinum" e blu di selezione di System 7 / Mac OS 8-9.
        Tema(
            "mac_classico", "Mac Classico",
            sfondo = 0xFFDDDDDD.toInt(), superficie = 0xFFFFFFFF.toInt(), bordo = 0xFF999999.toInt(),
            testo = 0xFF000000.toInt(), testoSecondario = 0xFF666666.toInt(),
            accento = 0xFF3366CC.toInt(), testoSuAccento = 0xFFFFFFFF.toInt(),
            bannerSfondo = 0xFFFFFFCC.toInt(), bannerTesto = 0xFF000000.toInt(),
            angoloBolla = 0f,
        ),
    )

    val predefinito: Tema get() = tutti.first()

    fun perId(id: String): Tema = tutti.firstOrNull { it.id == id } ?: predefinito
}

class Preferenze(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("impostazioni", Context.MODE_PRIVATE)

    var tema: String
        get() = sp.getString("tema", null) ?: "chiaro"
        set(valore) = sp.edit().putString("tema", valore).apply()
}
