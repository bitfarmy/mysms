package com.personale.messaggi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CodiciTest {
    @Test fun codiceItaliano() = assertEquals("482915", Codici.estrai("Il tuo codice di verifica è 482915. Non condividerlo."))
    @Test fun codiceConTrattino() = assertEquals("123456", Codici.estrai("Your code is 123-456"))
    @Test fun codiceQuattroCifre() = assertEquals("7391", Codici.estrai("PIN: 7391"))
    @Test fun senzaParolaChiave() = assertNull(Codici.estrai("Ci vediamo alle 2030 davanti al bar"))
    @Test fun ignoraNumeriDiTelefono() = assertNull(Codici.estrai("Per il codice chiama 3331234567"))
    @Test fun ignoraPrefisso() = assertNull(Codici.estrai("Codice cliente +393331234 non valido"))
    @Test fun senzaCifre() = assertNull(Codici.estrai("Inserisci il codice che trovi in app"))
}
