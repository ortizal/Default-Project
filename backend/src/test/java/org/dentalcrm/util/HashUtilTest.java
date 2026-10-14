package org.dentalcrm.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashUtilTest {

    @Test
    void coincideConElVectorConocidoDeSha256() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                HashUtil.sha256Hex("abc"));
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                HashUtil.sha256Hex(new byte[0]));
    }

    @Test
    void textoYBytesProducenLaMismaHuella() {
        String texto = "Consulta de ortodoncia ñáé";
        assertEquals(HashUtil.sha256Hex(texto), HashUtil.sha256Hex(texto.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void produceHexMinusculasDe64Caracteres() {
        String huella = HashUtil.sha256Hex("radiografia".getBytes(StandardCharsets.UTF_8));
        assertEquals(64, huella.length());
        assertTrue(huella.matches("[0-9a-f]{64}"));
    }

    @Test
    void contenidosDistintosTienenHuellasDistintas() {
        assertNotEquals(HashUtil.sha256Hex("v1"), HashUtil.sha256Hex("v2"));
    }

    @Test
    void rechazaEntradasNulas() {
        assertThrows(IllegalArgumentException.class, () -> HashUtil.sha256Hex((String) null));
        assertThrows(IllegalArgumentException.class, () -> HashUtil.sha256Hex((byte[]) null));
    }
}
