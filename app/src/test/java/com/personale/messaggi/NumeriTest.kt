package com.personale.messaggi

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NumeriTest {
    @Test fun prefissoInternazionaleEquivale() = assertTrue(Numeri.uguali("+393331234567", "333 123 4567"))
    @Test fun numeriDiversi() = assertFalse(Numeri.uguali("+393331234567", "+393331234568"))
    @Test fun numeriBreviSoloEsatti() {
        assertTrue(Numeri.uguali("4880", "4880"))
        assertFalse(Numeri.uguali("4880", "+394880"))
    }
    @Test fun alfanumericoIgnoraMaiuscole() = assertTrue(Numeri.uguali("Poste", "POSTE"))
    @Test fun alfanumericoNonEquivaleANumero() = assertFalse(Numeri.uguali("POSTE", "3331234567"))
    @Test fun riconosceAlfanumerico() {
        assertTrue(Numeri.alfanumerico("AMAZON"))
        assertFalse(Numeri.alfanumerico("+39 333 1234567"))
    }
    @Test fun vuotoNonEquivale() = assertFalse(Numeri.uguali("", "123456789"))
}
