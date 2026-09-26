package org.dentalcrm.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelefonoUtilTest {

    @Test
    void movilLocalConCeroPasaAE164() {
        assertEquals("+593985096366", TelefonoUtil.aE164("0985096366"));
    }

    @Test
    void movilSinCeroInicialPasaAE164() {
        assertEquals("+593991112233", TelefonoUtil.aE164("991112233"));
    }

    @Test
    void numeroInternacionalConEspaciosSeCompacta() {
        assertEquals("+593991112233", TelefonoUtil.aE164("+593 99 111 2233"));
    }

    @Test
    void numeroConPrefijoDePaisSeConserva() {
        assertEquals("+593991112233", TelefonoUtil.aE164("593991112233"));
    }

    @Test
    void fijoLocalSeConvierteAPrefijoDePais() {
        assertEquals("+59322345678", TelefonoUtil.aE164("02 234 5678"));
    }

    @Test
    void otroPaisTambienSeCompactaAE164() {
        assertEquals("+14155552671", TelefonoUtil.aE164("+1 415 555 2671"));
    }

    @Test
    void textoSinDigitosNoEsTelefono() {
        assertNull(TelefonoUtil.aE164("no es telefono"));
    }

    @Test
    void textoQueNoReconocemosSeDevuelveTalCual() {
        assertEquals("ext 123", TelefonoUtil.aE164("ext 123"));
    }

    @Test
    void vacioYNuloDanNulo() {
        assertNull(TelefonoUtil.aE164(null));
        assertNull(TelefonoUtil.aE164("   "));
    }

    @Test
    void mostrarAgrupaElNumeroEcuatoriano() {
        assertEquals("+593 99 111 2233", TelefonoUtil.mostrar("+593991112233"));
        assertEquals("+593 99 111 2233", TelefonoUtil.mostrar("0991112233"));
    }

    @Test
    void mostrarNoRompeLoDemas() {
        assertEquals("+1 415 555 2671", TelefonoUtil.mostrar("+1 415 555 2671"));
        assertNull(TelefonoUtil.mostrar(null));
    }

    @Test
    void correspondenSiguenFuncionandoConElFormatoNuevo() {
        assertTrue(TelefonoUtil.corresponden("+593985096366", "593985096366"));
        assertTrue(TelefonoUtil.corresponden("0985096366", "+593985096366"));
    }
}
