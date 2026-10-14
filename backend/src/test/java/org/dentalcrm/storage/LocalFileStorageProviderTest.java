package org.dentalcrm.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileStorageProviderTest {

    @TempDir
    Path tmp;

    private LocalFileStorageProvider almacen;

    @BeforeEach
    void setUp() {
        almacen = new LocalFileStorageProvider(tmp.toString());
    }

    @Test
    void guardaYLeeUnArchivo() {
        byte[] contenido = "radiografia-x".getBytes(StandardCharsets.UTF_8);

        String clave = almacen.guardar("1/historia/paciente-7", "rx.png", contenido);

        assertTrue(clave.startsWith("1/historia/paciente-7/"));
        assertTrue(clave.endsWith("rx.png"));
        assertTrue(almacen.existe(clave));
        assertArrayEquals(contenido, almacen.leer(clave).orElseThrow());
        assertTrue(Files.isRegularFile(tmp.resolve(clave)));
    }

    @Test
    void carpetaNulaUsaLaRaiz() {
        String clave = almacen.guardar(null, "consentimiento.pdf", "ok".getBytes(StandardCharsets.UTF_8));
        assertEquals("consentimiento.pdf", clave);
        assertArrayEquals("ok".getBytes(StandardCharsets.UTF_8), almacen.leer(clave).orElseThrow());
    }

    @Test
    void leerClaveInexistenteDevuelveVacio() {
        assertFalse(almacen.leer("no/existe.bin").isPresent());
        assertFalse(almacen.existe("no/existe.bin"));
    }

    @Test
    void eliminarNoFallaSiNoExisteYQuitaElArchivo() {
        assertFalse(almacen.existe("temporal.txt"));
        String clave = almacen.guardar("temp", "temporal.txt", "x".getBytes(StandardCharsets.UTF_8));
        assertTrue(almacen.existe(clave));
        almacen.eliminar(clave);
        assertFalse(almacen.existe(clave));
        almacen.eliminar(clave);
    }

    @Test
    void sobrescribeContenidoSinDejarRestos() {
        String clave = almacen.guardar("c", "informe.pdf", "v1".getBytes(StandardCharsets.UTF_8));
        almacen.guardar("c", "informe.pdf", "version2-mas-larga".getBytes(StandardCharsets.UTF_8));
        assertEquals("version2-mas-larga", new String(almacen.leer(clave).orElseThrow(), StandardCharsets.UTF_8));
    }

    @Test
    void rechazaTravesiasDeDirectorio() {
        assertThrows(IllegalArgumentException.class,
                () -> almacen.guardar("c", "../fuera.png", new byte[] {1}));
        assertThrows(IllegalArgumentException.class,
                () -> almacen.guardar("c", "sub/archivo.png", new byte[] {1}));
        assertThrows(IllegalArgumentException.class,
                () -> almacen.leer("../../etc/passwd"));
        assertThrows(IllegalArgumentException.class,
                () -> almacen.leer(""));
        assertThrows(IllegalArgumentException.class,
                () -> almacen.guardar("c", " ", new byte[] {1}));
    }

    @Test
    void elDirectorioRaizNoSeCreaHastaElPrimerGuardado() throws IOException {
        Path raiz = Path.of(tmp.toString(), "almacen").toAbsolutePath();
        LocalFileStorageProvider perezoso = new LocalFileStorageProvider(raiz.toString());
        assertFalse(Files.exists(raiz));
        perezoso.guardar(null, "a.txt", "a".getBytes(StandardCharsets.UTF_8));
        assertTrue(Files.isDirectory(raiz));
    }
}
