package org.dentalcrm.web.facturacion;

import org.dentalcrm.domain.configuracion.AmbienteSri;
import org.dentalcrm.domain.configuracion.ConfiguracionFacturacion;
import org.dentalcrm.domain.configuracion.ConfiguracionFacturacionRepository;
import org.dentalcrm.domain.configuracion.Consultorio;
import org.dentalcrm.domain.configuracion.ConsultorioRepository;
import org.dentalcrm.domain.configuracion.TipoEmisionSri;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.CurrentUserService;
import org.dentalcrm.service.IntegracionSecretCipher;
import org.dentalcrm.service.PrivateFileCipher;
import org.dentalcrm.web.facturacion.sri.ElectronicSignatureService;
import org.dentalcrm.web.facturacion.dto.ConfiguracionFacturacionRequest;
import org.dentalcrm.web.facturacion.dto.FirmaPasswordRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfiguracionFacturacionServiceTest {

    private static final String KEY = "unit-test-key-with-at-least-32-characters";

    @Mock
    private ConfiguracionFacturacionRepository repository;
    @Mock
    private ConsultorioRepository consultorioRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private CurrentUserService currentUserService;

    private ConfiguracionFacturacionService service;
    private ConfiguracionFacturacion guardada;
    private PrivateFileCipher archivoCipher;

    @BeforeEach
    void setUp() {
        guardada = null;
        archivoCipher = new PrivateFileCipher("unit-test-signing-key-with-at-least-32-characters");
        ElectronicSignatureService firma = new ElectronicSignatureService(archivoCipher,
            new IntegracionSecretCipher(KEY));
        service = new ConfiguracionFacturacionService(repository, consultorioRepository,
            auditService, new IntegracionSecretCipher(KEY), currentUserService, firma);
        lenient().when(repository.save(any(ConfiguracionFacturacion.class)))
                .thenAnswer(invocation -> {
                    guardada = invocation.getArgument(0);
                    return guardada;
                });
    }

    @AfterEach
    void limpiarTenant() {
        org.dentalcrm.multitenant.TenantContext.clear();
    }

    @Test
    void obtenerCreaLaFilaPorDefectoEnAmbienteDePruebas() {
        when(repository.findByTenantId(anyLong())).thenReturn(Optional.empty());
        when(consultorioRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        var response = service.obtener();

        assertNotNull(guardada);
        assertEquals(AmbienteSri.PRUEBAS, guardada.getAmbiente());
        assertEquals(TipoEmisionSri.NORMAL, guardada.getTipoEmision());
        assertEquals("001", response.establecimiento());
        assertEquals("001-001-000000001", response.secuencialSiguiente());
        assertEquals(response.sriUrlPruebas(), response.sriUrlActiva());
        assertFalse(response.sriCredencialesConfiguradas());
        verify(auditService, never()).registrar(anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void actualizarGuardaAmbienteSecuencialYBanderas() {
        when(repository.findByTenantId(anyLong())).thenReturn(Optional.of(guardada()));
        when(consultorioRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(currentUserService.idUsuarioActual()).thenReturn(Optional.of(9L));

        var response = service.actualizar(peticion(AmbienteSri.PRODUCCION, "002", "005", 42));

        assertEquals(AmbienteSri.PRODUCCION, guardada.getAmbiente());
        assertEquals("002", guardada.getEstablecimiento());
        assertEquals("005", guardada.getPuntoEmision());
        assertEquals(42, guardada.getSecuencialActual());
        assertTrue(guardada.isContribuyenteEspecial());
        assertEquals(9L, guardada.getUpdatedBy());
        assertEquals("002-005-000000043", response.secuencialSiguiente());
        assertEquals(response.sriUrlProduccion(), response.sriUrlActiva());
        verify(auditService).registrar("ACTUALIZAR_CONFIGURACION_SRI", "FACTURACION",
                "CONFIGURACION_FACTURACION", 1L);
    }

    @Test
    void actualizarRechazaUrlInvalida() {
        var peticion = new ConfiguracionFacturacionRequest(
                AmbienteSri.PRUEBAS, TipoEmisionSri.NORMAL, false, false, false,
                "001", "001", 0, "ftp://sri.gob.ec", "https://cel.sri.gob.ec", null, null, false);

        var error = assertThrows(BusinessException.class, () -> service.actualizar(peticion));

        assertEquals("URL_INVALIDA", error.getCode());
        assertNull(guardada);
    }

    @Test
    void claveDelSriSeCifraYNuncaSeDevuelve() {
        when(repository.findByTenantId(anyLong())).thenReturn(Optional.of(guardada()));
        when(consultorioRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(currentUserService.idUsuarioActual()).thenReturn(Optional.empty());

        var response = service.actualizar(peticionConClave("secreta-sri", false));

        assertTrue(response.sriCredencialesConfiguradas());
        assertTrue(guardada.getSriClave().startsWith("enc:v1:"));
        assertNotEquals("secreta-sri", guardada.getSriClave());
    }

    @Test
    void claveEnBlancoSeConservaYConLimpiarSeElimina() {
        ConfiguracionFacturacion existente = guardada();
        existente.setSriClave("enc:v1:previa");
        when(repository.findByTenantId(anyLong())).thenReturn(Optional.of(existente));
        when(consultorioRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(currentUserService.idUsuarioActual()).thenReturn(Optional.empty());

        service.actualizar(peticion(AmbienteSri.PRUEBAS, "001", "001", 0));
        assertEquals("enc:v1:previa", existente.getSriClave());

        service.actualizar(peticionConClave(null, true));
        assertNull(existente.getSriClave());
    }

    @Test
    void firmaQuedaSimuladaSinCertificado() {
        when(repository.findByTenantId(anyLong())).thenReturn(Optional.of(guardada()));
        when(consultorioRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(consultorio(null)));
        BusinessException error = assertThrows(BusinessException.class,
            () -> service.definirPasswordFirma(new FirmaPasswordRequest("pruebas123")));
        assertEquals("FIRMA_CERTIFICADO_NO_CARGADO", error.getCode());

        var response = service.obtener();

        assertEquals("SIMULADA", response.firmaModo());
        assertTrue(response.firmaMotivo().contains(".p12"));
        assertFalse(response.firmaPasswordConfigurada());
        assertNull(response.firmaArchivo());
    }

    @Test
    void firmaEsCertificadoConP12YPassword() {
        when(repository.findByTenantId(anyLong())).thenReturn(Optional.of(guardada()));
        when(consultorioRepository.findFirstByOrderByIdAsc())
            .thenReturn(Optional.of(consultorio(certificado())));
        when(currentUserService.idUsuarioActual()).thenReturn(Optional.empty());

        service.definirPasswordFirma(new FirmaPasswordRequest("pruebas123"));
        var response = service.obtener();

        assertEquals("CERTIFICADO", response.firmaModo());
        assertNull(response.firmaMotivo());
        assertEquals("certificado.p12", response.firmaArchivo());
        assertNotNull(response.firmaTitular());
        assertNotNull(response.firmaValidoHasta());
        assertTrue(guardada.getFirmaPassword().startsWith("enc:v1:"));
    }

    @Test
    void firmaQuedaSimuladaSinPasswordDelCertificado() {
        when(repository.findByTenantId(anyLong())).thenReturn(Optional.of(guardada()));
        when(consultorioRepository.findFirstByOrderByIdAsc())
            .thenReturn(Optional.of(consultorio(certificado())));

        var response = service.obtener();

        assertEquals("SIMULADA", response.firmaModo());
        assertTrue(response.firmaMotivo().contains("contraseña"));
        assertFalse(response.firmaPasswordConfigurada());
    }

    @Test
    void passwordIncorrectaNoSeGuarda() {
        ConfiguracionFacturacion existente = guardada();
        when(repository.findByTenantId(anyLong())).thenReturn(Optional.of(existente));
        when(consultorioRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(consultorio(certificado())));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.definirPasswordFirma(new FirmaPasswordRequest("incorrecta")));

        assertEquals("FIRMA_PASSWORD_INVALIDA", error.getCode());
        assertNull(existente.getFirmaPassword());
        verify(repository, never()).save(any(ConfiguracionFacturacion.class));
    }

    @Test
    void limpiarPasswordVuelveAFirmaSimulada() {
        ConfiguracionFacturacion existente = guardada();
        existente.setFirmaPassword("enc:v1:algo");
        when(repository.findByTenantId(anyLong())).thenReturn(Optional.of(existente));
        when(consultorioRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(consultorio(certificado())));
        when(currentUserService.idUsuarioActual()).thenReturn(Optional.empty());

        var response = service.limpiarPasswordFirma();

        assertNull(guardada.getFirmaPassword());
        assertEquals("SIMULADA", response.firmaModo());
        verify(auditService).registrar("LIMPIAR_PASSWORD_FIRMA", "FACTURACION",
                "CONFIGURACION_FACTURACION", 1L);
    }

    @Test
    void datosFiscalesSeLeenDelConsultorio() {
        when(repository.findByTenantId(anyLong())).thenReturn(Optional.of(guardada()));
        Consultorio consultorio = consultorio(null);
        consultorio.setRazonSocial("Little Smile S.A.");
        consultorio.setRuc("1791234567001");
        consultorio.setDireccion("Av. Principal 123");
        consultorio.setTelefono("0991112222");
        consultorio.setCorreoElectronico("facturacion@littlesmile.ec");
        when(consultorioRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(consultorio));

        var response = service.obtener();

        assertEquals("Little Smile S.A.", response.razonSocial());
        assertEquals("1791234567001", response.ruc());
        assertEquals("Av. Principal 123", response.direccion());
        assertEquals("0991112222", response.telefono());
        assertEquals("facturacion@littlesmile.ec", response.correo());
    }

    private ConfiguracionFacturacion guardada() {
        ConfiguracionFacturacion config = new ConfiguracionFacturacion();
        config.setId(1L);
        config.setTenantId(1L);
        return config;
    }

    private Consultorio consultorio(byte[] certificado) {
        Consultorio consultorio = new Consultorio();
        consultorio.setId(1L);
        consultorio.setNombre("Little Smile");
        consultorio.setFirmaDigital(certificado == null ? null : archivoCipher.encrypt(certificado));
        consultorio.setFirmaDigitalNombre(certificado == null ? null : "certificado.p12");
        return consultorio;
    }

    private byte[] certificado() {
        try (InputStream entrada = getClass().getResourceAsStream("/firma-test.p12")) {
            assertNotNull(entrada);
            return entrada.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private ConfiguracionFacturacionRequest peticion(AmbienteSri ambiente,
                                                     String establecimiento,
                                                     String puntoEmision,
                                                     int secuencial) {
        return new ConfiguracionFacturacionRequest(ambiente, TipoEmisionSri.NORMAL,
                true, false, false, establecimiento, puntoEmision, secuencial,
                "https://celpruebas.sri.gob.ec", "https://cel.sri.gob.ec", null, null, false);
    }

    private ConfiguracionFacturacionRequest peticionConClave(String clave, boolean limpiar) {
        return new ConfiguracionFacturacionRequest(AmbienteSri.PRUEBAS, TipoEmisionSri.NORMAL,
                false, false, false, "001", "001", 0,
                "https://celpruebas.sri.gob.ec", "https://cel.sri.gob.ec", null, clave, limpiar);
    }
}
