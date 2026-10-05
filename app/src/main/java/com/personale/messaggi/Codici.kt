package com.personale.messaggi

/** Riconosce i codici di verifica (OTP, 2FA) nel testo di un SMS. */
object Codici {

    private val parole = Regex(
        "(?i)codice|code|otp|\\bpin\\b|password|verific|conferma|autorizz|one.?time|token|sicurezza|security",
    )

    // 4-8 cifre isolate, oppure 3+3 cifre separate da trattino o spazio (es. 123-456).
    // Non deve far parte di un numero più lungo o di un numero di telefono (+39…).
    private val candidato = Regex("(?<![\\d+])(\\d{3}[- ]\\d{3}|\\d{4,8})(?!\\d)")

    fun estrai(testo: String): String? {
        if (!parole.containsMatchIn(testo)) return null
        val trovato = candidato.find(testo) ?: return null
        return trovato.value.replace(Regex("[- ]"), "")
    }
}
