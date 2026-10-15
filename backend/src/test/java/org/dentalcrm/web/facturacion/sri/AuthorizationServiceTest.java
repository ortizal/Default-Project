package org.dentalcrm.web.facturacion.sri;

import org.dentalcrm.domain.comprobante.ComprobanteElectronico;
import org.dentalcrm.domain.comprobante.ComprobanteElectronicoRepository;
import org.dentalcrm.domain.comprobante.EstadoGeneracion;
import org.dentalcrm.domain.comprobante.EstadoTributario;
import org.dentalcrm.domain.configuracion.AmbienteSri;
import org.dentalcrm.domain.configuracion.ConfiguracionFacturacion;
import org.dentalcrm.domain.configuracion.ConfiguracionFacturacionRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    private static final String CLAVE = "0810202601179123456700110010010000000011234567814";
    private static final String XML = "<factura/>";

    @Mock
    private SriGateway gateway;
    @Mock
    private ComprobanteElectronicoRepository repository;
    @Mock
    private ConfiguracionFacturacionRepository configuracionRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private CurrentUserService currentUserService;

    private AuthorizationService servicio;

    @BeforeEach
    void setUp() {
        servicio = new AuthorizationService(gateway, repository, configuracionRepository,
                auditService, currentUserService);
        lenient().when(repository.save(any(ComprobanteElectronico.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        lenient().when(currentUserService.idUsuarioActual()).thenReturn(Optional.of(7L));
        lenient().when(configuracionRepository.findByTenantId(anyLong()))
                .thenReturn(Optional.of(configuracion(AmbienteSri.PRUEBAS)));
    }

    @Test
        void envioSimuladoPermaneceLocalSinMarcarRecibidoPorElSri() {
        ComprobanteElectronico comprobante = comprobante(AmbienteSri.PRUEBAS,
                ElectronicSignatureService.MODO_SIMULADA, EstadoGeneracion.FIRMADO);

        servicio.enviar(comprobante);

        // §3.8: el XML ya estaba firmado; el modo simulado no lo deshace ni
        // retrocede a GENERADO. Sólo marca que no salió a la red.
        assertEquals(EstadoGeneracion.FIRMADO, comprobante.getEstadoGeneracion());
        assertEquals(EstadoTributario.PENDIENTE, comprobante.getEstadoTributario());
        assertTrue(comprobante.isSimulado());
        assertEquals("SIMULADA", comprobante.getSriEstado());
        assertTrue(comprobante.getSriRespuesta().contains("no salió de este equipo"));
        verifyNoInteractions(gateway);
        verify(repository).save(comprobante);
    }

    @Test
    void envioRealRecibidaPasaAEnviado() {
        when(gateway.recepcionar(anyString(), any()))
                .thenReturn(new RespuestaRecepcion(RespuestaRecepcion.RECIBIDA, List.of()));
        ComprobanteElectronico comprobante = comprobante(AmbienteSri.PRUEBAS,
                ElectronicSignatureService.MODO_CERTIFICADO, EstadoGeneracion.FIRMADO);

        servicio.enviar(comprobante);

        assertEquals(EstadoGeneracion.ENVIADO, comprobante.getEstadoGeneracion());
        assertEquals(EstadoTributario.PENDIENTE, comprobante.getEstadoTributario());
        assertEquals("RECIBIDA", comprobante.getSriEstado());
        assertNull(comprobante.getErrorMensaje());
        assertFalse(comprobante.isSimulado());
    }

    @Test
    void envioRealDevueltaMarcaErrorYDevuelto() {
        MensajeSri mensaje = new MensajeSri("43", "CLAVE ACCESO REGISTRADA",
                "Comprobante ya enviado", "ERROR");
        when(gateway.recepcionar(anyString(), any()))
                .thenReturn(new RespuestaRecepcion(RespuestaRecepcion.DEVUELTA, List.of(mensaje)));
        ComprobanteElectronico comprobante = comprobante(AmbienteSri.PRUEBAS,
                ElectronicSignatureService.MODO_CERTIFICADO, EstadoGeneracion.FIRMADO);

        servicio.enviar(comprobante);

        assertEquals(EstadoGeneracion.ERROR, comprobante.getEstadoGeneracion());
        assertEquals(EstadoTributario.DEVUELTO, comprobante.getEstadoTributario());
        assertEquals("DEVUELTA", comprobante.getSriEstado());
        assertTrue(comprobante.getErrorMensaje().contains("43"));
        assertTrue(comprobante.getErrorMensaje().contains("CLAVE ACCESO REGISTRADA"));
    }

    @Test
        void autorizarSimuladoAutorizaLocalmenteSinLlamarAlSri() {
        ComprobanteElectronico comprobante = comprobante(AmbienteSri.PRUEBAS,
                ElectronicSignatureService.MODO_SIMULADA, EstadoGeneracion.FIRMADO);

        servicio.autorizar(comprobante);

        // §3.8/§3.9: el "SRI local" recibe y autoriza para poder ejercitar
        // RIDE, notas de crédito y anulaciones sin salir de la máquina.
        assertEquals(EstadoTributario.AUTORIZADO, comprobante.getEstadoTributario());
        assertEquals(EstadoGeneracion.ENVIADO, comprobante.getEstadoGeneracion());
        assertTrue(comprobante.isSimulado());
        assertEquals("SIMULADA", comprobante.getSriEstado());
        assertEquals(CLAVE, comprobante.getAutorizacionNumero(),
                "el número de autorización simulado es la clave de acceso, como en el SRI real");
        assertNotNull(comprobante.getAutorizacionFecha());
        assertEquals(XML, comprobante.getXmlAutorizado(),
                "el XML autorizado es el firmado: nadie más lo firmó");
        assertNull(comprobante.getErrorMensaje());
        assertTrue(comprobante.getSriRespuesta().contains("Autorización simulada"));
        verifyNoInteractions(gateway);
    }

    @Test
    void autorizarSimuladoEnProduccionSeBloquea() {
        ComprobanteElectronico comprobante = comprobante(AmbienteSri.PRODUCCION,
                ElectronicSignatureService.MODO_SIMULADA, EstadoGeneracion.ENVIADO);

        var error = assertThrows(BusinessException.class, () -> servicio.autorizar(comprobante));

        assertEquals("SRI_FIRMA_REAL_REQUERIDA", error.getCode());
        assertEquals(EstadoTributario.PENDIENTE, comprobante.getEstadoTributario());
        verifyNoInteractions(gateway);
    }

    @Test
    void autorizarRealReflejaLaAutorizacionDelSri() {
        Instant fecha = Instant.parse("2026-10-08T15:15:30Z");
        String xmlAutorizado = SriXmlGenerator.generarFactura(FixturesFactura.factura());
        when(gateway.autorizar(anyString(), any())).thenReturn(new RespuestaAutorizacion(
                RespuestaAutorizacion.AUTORIZADO, CLAVE, CLAVE, fecha, "PRUEBAS",
                xmlAutorizado, List.of(), false));
        ComprobanteElectronico comprobante = comprobante(AmbienteSri.PRUEBAS,
                ElectronicSignatureService.MODO_CERTIFICADO, EstadoGeneracion.ENVIADO);

        servicio.autorizar(comprobante);

        assertEquals(EstadoTributario.AUTORIZADO, comprobante.getEstadoTributario());
        assertEquals(CLAVE, comprobante.getAutorizacionNumero());
        assertEquals(fecha, comprobante.getAutorizacionFecha());
        assertEquals(xmlAutorizado, comprobante.getXmlAutorizado());
        assertNull(comprobante.getErrorMensaje());
    }

    @Test
    void autorizarRealSinResultadoMantienePendiente() {
        when(gateway.autorizar(anyString(), any())).thenReturn(new RespuestaAutorizacion(
                RespuestaAutorizacion.EN_PROCESO, CLAVE, null, null, null, null, List.of(), true));
        ComprobanteElectronico comprobante = comprobante(AmbienteSri.PRUEBAS,
                ElectronicSignatureService.MODO_CERTIFICADO, EstadoGeneracion.ENVIADO);

        servicio.autorizar(comprobante);

        assertEquals(EstadoTributario.PENDIENTE, comprobante.getEstadoTributario());
        assertEquals("EN_PROCESO", comprobante.getSriEstado());
        assertNull(comprobante.getAutorizacionNumero());
    }

    @Test
    void autorizarRealNoAutorizadoRegistraElError() {
        MensajeSri mensaje = new MensajeSri("39", "RUC EMISOR NO VALIDO", null, "ERROR");
        when(gateway.autorizar(anyString(), any())).thenReturn(new RespuestaAutorizacion(
                RespuestaAutorizacion.NO_AUTORIZADO, CLAVE, null, null, "PRUEBAS",
                null, List.of(mensaje), false));
        ComprobanteElectronico comprobante = comprobante(AmbienteSri.PRUEBAS,
                ElectronicSignatureService.MODO_CERTIFICADO, EstadoGeneracion.ENVIADO);

        servicio.autorizar(comprobante);

        assertEquals(EstadoTributario.NO_AUTORIZADO, comprobante.getEstadoTributario());
        assertEquals("NO_AUTORIZADO", comprobante.getSriEstado());
        assertTrue(comprobante.getErrorMensaje().contains("RUC EMISOR NO VALIDO"));
    }

    @Test
    void autorizacionConClaveConsultadaDistintaQuedaEnErrorPendiente() {
        when(gateway.autorizar(anyString(), any())).thenReturn(new RespuestaAutorizacion(
                RespuestaAutorizacion.AUTORIZADO, "0000000000000000000000000000000000000000000000000",
                CLAVE, Instant.now(), "PRUEBAS", XML, List.of(), false));
        ComprobanteElectronico comprobante = comprobante(AmbienteSri.PRUEBAS,
                ElectronicSignatureService.MODO_CERTIFICADO, EstadoGeneracion.ENVIADO);

        servicio.autorizar(comprobante);

        assertEquals(EstadoGeneracion.ERROR, comprobante.getEstadoGeneracion());
        assertEquals(EstadoTributario.PENDIENTE, comprobante.getEstadoTributario());
        assertNull(comprobante.getAutorizacionNumero());
        assertTrue(comprobante.getSriRespuesta().contains("no corresponde"));
    }

    @Test
    void autorizarExigeEstarEnviado() {
        ComprobanteElectronico comprobante = comprobante(AmbienteSri.PRUEBAS,
                ElectronicSignatureService.MODO_CERTIFICADO, EstadoGeneracion.FIRMADO);

        var error = assertThrows(BusinessException.class, () -> servicio.autorizar(comprobante));

        assertEquals("COMPROBANTE_NO_ENVIADO", error.getCode());
        verifyNoInteractions(gateway);
    }

    @Test
    void enviarExigeXmlFirmado() {
        ComprobanteElectronico comprobante = comprobante(AmbienteSri.PRUEBAS,
                ElectronicSignatureService.MODO_CERTIFICADO, EstadoGeneracion.GENERADO);
        comprobante.setXmlFirmado(null);

        var error = assertThrows(BusinessException.class, () -> servicio.enviar(comprobante));

        assertEquals("COMPROBANTE_SIN_XML", error.getCode());
        verifyNoInteractions(gateway);
    }

    @Test
    void elEntornoSaleDeLaConfiguracionDelTenant() {
        when(configuracionRepository.findByTenantId(anyLong()))
                .thenReturn(Optional.of(configuracion(AmbienteSri.PRODUCCION)));
        when(gateway.recepcionar(anyString(), any()))
                .thenReturn(new RespuestaRecepcion(RespuestaRecepcion.RECIBIDA, List.of()));
        ComprobanteElectronico comprobante = comprobante(AmbienteSri.PRODUCCION,
                ElectronicSignatureService.MODO_CERTIFICADO, EstadoGeneracion.FIRMADO);

        servicio.enviar(comprobante);

        var entorno = org.mockito.ArgumentCaptor.forClass(SriEntorno.class);
        verify(gateway).recepcionar(anyString(), entorno.capture());
        assertEquals("http://sri-produccion.test", entorno.getValue().urlBase());
        assertEquals(AmbienteSri.PRODUCCION, entorno.getValue().ambiente());
    }

    private ComprobanteElectronico comprobante(AmbienteSri ambiente, String modo, EstadoGeneracion generacion) {
        ComprobanteElectronico comprobante = new ComprobanteElectronico();
        comprobante.setTenantId(1L);
        comprobante.setClaveAcceso(CLAVE);
        comprobante.setAmbiente(ambiente);
        comprobante.setModoFirma(modo);
        comprobante.setEstadoGeneracion(generacion);
        comprobante.setEstadoTributario(EstadoTributario.PENDIENTE);
        comprobante.setXmlFirmado(XML);
        comprobante.setEstablecimiento("001");
        comprobante.setPuntoEmision("001");
        comprobante.setSecuencial(1);
        comprobante.setNumeroComprobante("001-001-000000001");
        return comprobante;
    }

    private ConfiguracionFacturacion configuracion(AmbienteSri ambiente) {
        ConfiguracionFacturacion config = new ConfiguracionFacturacion();
        config.setTenantId(1L);
        config.setAmbiente(ambiente);
        config.setSriUrlPruebas("http://sri-pruebas.test");
        config.setSriUrlProduccion("http://sri-produccion.test");
        return config;
    }
}
