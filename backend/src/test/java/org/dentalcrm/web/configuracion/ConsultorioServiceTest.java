package org.dentalcrm.web.configuracion;

import org.dentalcrm.domain.configuracion.Consultorio;
import org.dentalcrm.domain.configuracion.ConsultorioRepository;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.PrivateFileCipher;
import org.dentalcrm.web.configuracion.dto.ConsultorioRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultorioServiceTest {

    @Mock
    private ConsultorioRepository repository;
    @Mock
    private AuditService auditService;
    private PrivateFileCipher privateFileCipher;

    private ConsultorioService service;
    private Consultorio consultorio;

    @BeforeEach
    void setUp() {
        privateFileCipher = new PrivateFileCipher("unit-test-signing-key-with-at-least-32-characters");
        service = new ConsultorioService(repository, auditService, privateFileCipher, "Clinica", "", "");
        consultorio = new Consultorio();
        consultorio.setId(1L);
        consultorio.setNombre("Clinica");
    }

    @Test
    void actualizarGuardaDatosLegalesDelConsultorio() {
        when(repository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(consultorio));
        when(repository.save(any(Consultorio.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.actualizar(new ConsultorioRequest(
                "Clinica", "Clinica Dental S.A.", "1790012345001", "info@clinica.ec",
                "Av. Central", "0991234567", null, null));

        assertEquals("Clinica Dental S.A.", response.razonSocial());
        assertEquals("1790012345001", response.ruc());
        assertEquals("info@clinica.ec", response.correoElectronico());
    }

    @Test
    void cargarFirmaAlmacenaP12PrivadoYDevuelveSoloMetadatos() {
        when(repository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(consultorio));
        when(repository.save(any(Consultorio.class))).thenAnswer(invocation -> invocation.getArgument(0));
        byte[] contenido = new byte[32];
        contenido[0] = 0x30;
        MockMultipartFile archivo = new MockMultipartFile("archivo", "firma.p12",
                "application/x-pkcs12", contenido);

        var response = service.cargarFirmaDigital(archivo);

        assertEquals("firma.p12", response.firmaDigitalNombre());
        assertTrue(response.firmaDigitalCargada());
        assertFalse(java.util.Arrays.equals(contenido, consultorio.getFirmaDigital()));
        assertArrayEquals(contenido, privateFileCipher.decrypt(consultorio.getFirmaDigital()));
        verify(auditService).registrar("CARGAR_FIRMA_DIGITAL", "CONFIGURACION", "CONSULTORIO", 1L);
    }

    @Test
    void cargarFirmaRechazaExtensionDistintaDeP12() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "firma.pfx",
                "application/octet-stream", new byte[32]);

        assertThrows(org.dentalcrm.exception.BusinessException.class,
                () -> service.cargarFirmaDigital(archivo));
        assertNull(consultorio.getFirmaDigital());
    }
}