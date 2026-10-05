package com.personale.messaggi

/** Logica sui numeri/mittenti senza dipendenze Android, così si prova con test normali. */
object Numeri {

    /** Mittenti come "POSTE" o "Amazon": non si può rispondere e non sono in rubrica. */
    fun alfanumerico(numero: String): Boolean = numero.any { it.isLetter() }

    fun cifre(numero: String): String = numero.filter { it.isDigit() }

    /**
     * Forma canonica di un mittente: due scritture dello stesso numero (+39333… e 333…) danno la stessa chiave.
     * Numeri brevi (servizi): tutte le cifre. Altrimenti le ultime 9. Mittenti con nome: minuscolo.
     */
    fun chiave(numero: String): String {
        if (alfanumerico(numero)) return "a:" + numero.trim().lowercase()
        val c = cifre(numero)
        return if (c.length < 9) c else c.takeLast(9)
    }

    /** Stesso mittente anche se scritto con o senza prefisso internazionale (+39333… = 333…). */
    fun uguali(a: String, b: String): Boolean {
        val ka = chiave(a)
        return ka.isNotEmpty() && ka == chiave(b)
    }
}
