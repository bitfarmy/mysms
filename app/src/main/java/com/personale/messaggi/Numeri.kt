package com.personale.messaggi

/** Logica sui numeri/mittenti senza dipendenze Android, così si prova con test normali. */
object Numeri {

    /** Mittenti come "POSTE" o "Amazon": non si può rispondere e non sono in rubrica. */
    fun alfanumerico(numero: String): Boolean = numero.any { it.isLetter() }

    fun cifre(numero: String): String = numero.filter { it.isDigit() }

    /** Stesso mittente anche se scritto con o senza prefisso internazionale (+39333… = 333…). */
    fun uguali(a: String, b: String): Boolean {
        if (alfanumerico(a) || alfanumerico(b)) return a.trim().equals(b.trim(), ignoreCase = true)
        val ca = cifre(a)
        val cb = cifre(b)
        if (ca.isEmpty() || cb.isEmpty()) return false
        // Numeri brevi (servizi): confronto esatto. Altrimenti contano le ultime 9 cifre.
        if (minOf(ca.length, cb.length) < 9) return ca == cb
        return ca.takeLast(9) == cb.takeLast(9)
    }
}
