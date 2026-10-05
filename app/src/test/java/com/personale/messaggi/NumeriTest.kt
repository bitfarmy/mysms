package com.personale.messaggi

import org.junit.Assert.assertEquals
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
    @Test fun chiaveUgualePerScrittureDiverse() = assertEquals(Numeri.chiave("+393331234567"), Numeri.chiave("333 123 4567"))
    @Test fun chiaveNonContieneIlNumeroCompleto() = assertEquals("331234567", Numeri.chiave("+393331234567"))
    @Test fun chiaveNumeroBreve() = assertEquals("4880", Numeri.chiave("4880"))
    @Test fun chiaveAlfanumerica() = assertEquals("a:poste", Numeri.chiave(" POSTE "))
}

