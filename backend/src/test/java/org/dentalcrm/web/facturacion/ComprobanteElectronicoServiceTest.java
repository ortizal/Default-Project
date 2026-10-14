package org.dentalcrm.web.facturacion;

import org.dentalcrm.domain.comprobante.AnulacionComprobante;
import org.dentalcrm.domain.comprobante.AnulacionComprobanteRepository;
import org.dentalcrm.domain.comprobante.ComprobanteElectronico;
import org.dentalcrm.domain.comprobante.ComprobanteElectronicoRepository;
import org.dentalcrm.domain.comprobante.EstadoGeneracion;
import org.dentalcrm.domain.comprobante.TipoComprobanteSri;
import org.dentalcrm.domain.comprobante.EstadoTributario;
import org.dentalcrm.domain.configuracion.AmbienteSri;
import org.dentalcrm.domain.configuracion.ConfiguracionFacturacion;
import org.dentalcrm.domain.configuracion.ConfiguracionFacturacionRepository;
import org.dentalcrm.domain.configuracion.Consultorio;
import org.dentalcrm.domain.configuracion.ConsultorioRepository;
import org.dentalcrm.domain.configuracion.TipoEmisionSri;
import org.dentalcrm.domain.cuenta.CuentaDetalle;
import org.dentalcrm.domain.cuenta.CuentaDetalleRepository;
import org.dentalcrm.domain.cuenta.CuentaPaciente;
import org.dentalcrm.domain.cuenta.CuentaPacienteRepository;
import org.dentalcrm.domain.cuenta.EstadoCuenta;
import org.dentalcrm.domain.cuenta.FormaPago;
import org.dentalcrm.domain.cuenta.TipoMovimientoCuenta;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.CurrentUserService;
import org.dentalcrm.web.facturacion.dto.ComprobanteElectronicoResponse;
import org.dentalcrm.web.facturacion.dto.ComprobanteFiltroRequest;
import org.dentalcrm.web.facturacion.dto.ComprobanteLineaResponse;
import org.dentalcrm.web.facturacion.dto.ComprobanteResumenResponse;
import org.dentalcrm.web.facturacion.dto.AnularComprobanteRequest;
import org.dentalcrm.web.facturacion.dto.EmitirFacturaRequest;
import org.dentalcrm.web.facturacion.dto.EmitirNotaCreditoRequest;
import org.dentalcrm.web.facturacion.sri.AuthorizationService;
import org.dentalcrm.web.facturacion.sri.ClaveAccesoService;
import org.dentalcrm.web.facturacion.sri.ElectronicSignatureService;
import org.dentalcrm.web.facturacion.sri.RideGenerator;
import org.dentalcrm.web.facturacion.sri.SriXmlValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Fase G.4: emisión end-to-end de la factura (plan §3.5).
 *
 * <p>Se simula sólo la parte externa (firma y SRI); la generación real —
 * secuencial, clave de acceso, XML, XSD y persistencia — corre entera.
 */
class ComprobanteElectronicoServiceTest {

    private ComprobanteElectronicoService servicio;

    private CuentaPacienteRepository cuentaRepo;
    private CuentaDetalleRepository detalleRepo;
    private ConsultorioRepository consultorioRepo;
    private ConfiguracionFacturacionRepository configRepo;
    private ComprobanteElectronicoRepository comprobanteRepo;
    private AnulacionComprobanteRepository anulacionRepo;
    private PacienteRepository pacienteRepo;
    private AuthorizationService authorizationService;
    private AuditService auditoria;

    private CuentaPaciente cuenta;
    private Consultorio consultorio;
    private ConfiguracionFacturacion config;
    private final List<CuentaDetalle> libro = new ArrayList<>();

    /** Último comprobante guardado y el estado que tenía al guardarse. */
    private ComprobanteElectronico guardado;
    private EstadoGeneracion estadoAlGuardar;
    private String xmlAlGuardar;
        private String modoFirmaFixture;

    @BeforeEach
    void setUp() {
        libro.clear();
        guardado = null;
        estadoAlGuardar = null;
        xmlAlGuardar = null;
        modoFirmaFixture = ElectronicSignatureService.MODO_SIMULADA;

        Paciente paciente = new Paciente();
        paciente.setId(10L);
        paciente.setNombres("Ana");
        paciente.setApellidos("Pérez");
        paciente.setCedula("1712345678");
        paciente.setDireccion("Calle Falsa 123");
        paciente.setEmail("ana@ejemplo.com");

        cuenta = new CuentaPaciente();
        cuenta.setId(1L);
        cuenta.setPaciente(paciente);
        cuenta.setEstado(EstadoCuenta.ABIERTA);

        consultorio = new Consultorio();
        consultorio.setNombre("Little Smile Odontología");
        consultorio.setRazonSocial("Little Smile S.A.");
        consultorio.setRuc("1791234567001");
        consultorio.setDireccion("Av. República 123 y Av. Principal, Quito");

        config = new ConfiguracionFacturacion();
        config.setAmbiente(AmbienteSri.PRUEBAS);
        config.setTipoEmision(TipoEmisionSri.NORMAL);
        config.setEstablecimiento("001");
        config.setPuntoEmision("001");
        config.setSecuencialActual(0);
        config.setObligadoContabilidad(true);

        libro.add(movimiento(TipoMovimientoCuenta.CARGO, "Consulta odontológica",
                1, "100.00", "100.00", null));
        libro.add(movimiento(TipoMovimientoCuenta.CARGO, "Profilaxis",
                2, "25.00", "50.00", null));
        libro.add(movimiento(TipoMovimientoCuenta.ABONO, "Abono en efectivo",
                1, null, "40.00", FormaPago.EFECTIVO));

        cuentaRepo = mock(CuentaPacienteRepository.class);
        when(cuentaRepo.buscarParaFacturar(1L)).thenAnswer(inv -> Optional.of(cuenta));

        detalleRepo = mock(CuentaDetalleRepository.class);
        when(detalleRepo.findByCuentaIdOrderByFechaAscIdAsc(1L))
                .thenAnswer(inv -> List.copyOf(libro));

        consultorioRepo = mock(ConsultorioRepository.class);
        when(consultorioRepo.findFirstByOrderByIdAsc()).thenAnswer(inv -> Optional.of(consultorio));

        configRepo = mock(ConfiguracionFacturacionRepository.class);
        when(configRepo.buscarParaSecuencial(anyLong())).thenAnswer(inv -> Optional.of(config));
        when(configRepo.save(any(ConfiguracionFacturacion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        comprobanteRepo = mock(ComprobanteElectronicoRepository.class);
        when(comprobanteRepo.findByCuentaIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(comprobanteRepo.save(any(ComprobanteElectronico.class))).thenAnswer(inv -> {
            guardado = inv.getArgument(0);
            estadoAlGuardar = guardado.getEstadoGeneracion();
            xmlAlGuardar = guardado.getXmlFirmado();
            return guardado;
        });

        anulacionRepo = mock(AnulacionComprobanteRepository.class);
        when(anulacionRepo.save(any(AnulacionComprobante.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(anulacionRepo.findFirstByComprobanteIdAndEstadoInOrderBySolicitadoEnDesc(anyLong(), any()))
                .thenReturn(Optional.empty());

        pacienteRepo = mock(PacienteRepository.class);
        when(pacienteRepo.findAllById(any())).thenReturn(List.of());

        ElectronicSignatureService firma = mock(ElectronicSignatureService.class);
        when(firma.firmar(anyString(), any(), any())).thenAnswer(inv ->
                new ElectronicSignatureService.FirmaResultado(inv.getArgument(0),
                        modoFirmaFixture));

        authorizationService = mock(AuthorizationService.class);
        when(authorizationService.enviar(any(ComprobanteElectronico.class))).thenAnswer(inv -> {
            ComprobanteElectronico c = inv.getArgument(0);
                        if (ElectronicSignatureService.MODO_SIMULADA.equals(c.getModoFirma())) {
                                c.setSriEstado("SIMULADA");
                                c.setSriRespuesta("Prueba interna: no se contactó al SRI.");
                                c.setSimulado(true);
                        } else {
                                c.setEstadoGeneracion(EstadoGeneracion.ENVIADO);
                                c.setSriEstado("RECIBIDA");
                                c.setSimulado(false);
                        }
            return c;
        });
        when(authorizationService.autorizar(any(ComprobanteElectronico.class))).thenAnswer(inv -> {
            ComprobanteElectronico c = inv.getArgument(0);
                        if (ElectronicSignatureService.MODO_SIMULADA.equals(c.getModoFirma())) {
                                c.setEstadoTributario(EstadoTributario.PENDIENTE);
                                c.setSriEstado("SIMULADA");
                                c.setSriRespuesta("Prueba interna: no se consultó al SRI.");
                        } else {
                                c.setEstadoTributario(EstadoTributario.AUTORIZADO);
                                c.setAutorizacionNumero(c.getClaveAcceso());
                                c.setXmlAutorizado(c.getXmlFirmado());
                        }
            return c;
        });

        auditoria = mock(AuditService.class);
        CurrentUserService usuarioActual = mock(CurrentUserService.class);
        when(usuarioActual.idUsuarioActual()).thenReturn(Optional.of(7L));

        PlatformTransactionManager transacciones = mock(PlatformTransactionManager.class);
        when(transacciones.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        servicio = new ComprobanteElectronicoService(cuentaRepo, detalleRepo, consultorioRepo,
                configRepo, comprobanteRepo, anulacionRepo, pacienteRepo, new ClaveAccesoService(),
                new SriXmlValidator(), firma, authorizationService, auditoria, usuarioActual,
                new RideGenerator(), transacciones);
    }

    @Test
        void emiteComprobanteLocalSinAfirmarRecepcionNiAutorizacionDelSri() {
        ComprobanteElectronicoResponse respuesta = servicio.emitir(
                new EmitirFacturaRequest(1L, null, null));

        assertEquals("001-001-000000001", respuesta.numeroComprobante());
        assertEquals(49, respuesta.claveAcceso().length());
        assertEquals("1", respuesta.claveAcceso().substring(23, 24));   // ambiente de pruebas
        assertEquals("001001", respuesta.claveAcceso().substring(24, 30));
        assertEquals("000000001", respuesta.claveAcceso().substring(30, 39));
        assertEquals(1, config.getSecuencialActual(), "el secuencial se consume al emitir");

        assertEquals(ElectronicSignatureService.MODO_SIMULADA, respuesta.modoFirma());
        assertTrue(respuesta.simulado());
        assertEquals(EstadoGeneracion.FIRMADO, estadoAlGuardar,
                "el comprobante se guarda firmado antes de hablar con el SRI");
        assertEquals(EstadoTributario.PENDIENTE.name(), respuesta.estadoTributario());
        assertEquals(EstadoGeneracion.FIRMADO.name(), respuesta.estadoGeneracion());
        assertNull(respuesta.autorizacionNumero());
        assertTrue(respuesta.sriRespuesta().contains("no existe recepción"));

        assertEquals(0, respuesta.total().compareTo(new BigDecimal("150.00")),
                "los precios ya llevan IVA: 100.00 + 50.00");
        assertNotNull(respuesta.fechaEmision());
        assertEquals(new ClaveAccesoService().fechaEmisionValida(LocalDate.now()),
                respuesta.fechaEmision());

        assertNotNull(xmlAlGuardar);
        assertDoesNotThrow(() -> new SriXmlValidator().validar(xmlAlGuardar));
        assertTrue(xmlAlGuardar.contains("<importeTotal>150.00</importeTotal>"));
        assertTrue(xmlAlGuardar.contains("<identificacionComprador>1712345678</identificacionComprador>"));
        assertTrue(xmlAlGuardar.contains("<formaPago>01</formaPago>"));
        verify(auditoria).registrar(eq("GENERAR_COMPROBANTE_SRI"), eq("FACTURACION"),
                eq("COMPROBANTE_ELECTRONICO"), eq(guardado.getId()), isNull(),
                argThat((Map<String, Object> datos) -> respuesta.claveAcceso().equals(datos.get("claveAcceso"))
                        && "001-001-000000001".equals(datos.get("numeroComprobante"))
                        && EstadoGeneracion.FIRMADO.name().equals(datos.get("estadoGeneracion"))));
    }

    @Test
    void noEmiteSiLaFacturacionNoEstaConfigurada() {
        when(configRepo.buscarParaSecuencial(anyLong())).thenReturn(Optional.empty());

        BusinessException error = assertThrows(BusinessException.class,
                () -> servicio.emitir(new EmitirFacturaRequest(1L, null, null)));

        assertEquals("FACTURACION_NO_CONFIGURADA", error.getCode());
        verificarQueNoSeGeneroNada();
    }

    @Test
    void noConsumeElSecuencialSiLaCuentaNoTieneCargos() {
        libro.clear();

        BusinessException error = assertThrows(BusinessException.class,
                () -> servicio.emitir(new EmitirFacturaRequest(1L, null, null)));

        assertEquals("SIN_DETALLE", error.getCode());
        assertEquals(0, config.getSecuencialActual());
        verify(configRepo, never()).save(any(ConfiguracionFacturacion.class));
        verify(comprobanteRepo, never()).save(any(ComprobanteElectronico.class));
    }

    @Test
    void enProduccionExigeElCertificado() {
        config.setAmbiente(AmbienteSri.PRODUCCION);

        BusinessException error = assertThrows(BusinessException.class,
                () -> servicio.emitir(new EmitirFacturaRequest(1L, null, null)));

        assertEquals("SRI_FIRMA_REAL_REQUERIDA", error.getCode());
        assertEquals(0, config.getSecuencialActual());
        verify(comprobanteRepo, never()).save(any(ComprobanteElectronico.class));
    }

    @Test
    void noPermiteEmitirDosVecesLaMismaCuenta() {
        ComprobanteElectronico previo = new ComprobanteElectronico();
        previo.setEstadoGeneracion(EstadoGeneracion.ENVIADO);
        previo.setEstadoTributario(EstadoTributario.AUTORIZADO);
        when(comprobanteRepo.findByCuentaIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(previo));

        BusinessException error = assertThrows(BusinessException.class,
                () -> servicio.emitir(new EmitirFacturaRequest(1L, null, null)));

        assertEquals("CUENTA_YA_FACTURADA", error.getCode());
        verificarQueNoSeGeneroNada();
    }

    @Test
    void siElEnvioFallaElComprobanteQuedaEnError() {
        when(authorizationService.enviar(any(ComprobanteElectronico.class)))
                .thenThrow(new BusinessException("SRI_SIN_CONEXION", "Sin conexión con el SRI"));

        BusinessException error = assertThrows(BusinessException.class,
                () -> servicio.emitir(new EmitirFacturaRequest(1L, null, null)));

        assertEquals("SRI_SIN_CONEXION", error.getCode());
        assertNotNull(guardado, "el comprobante generado no se pierde");
        assertEquals(EstadoGeneracion.ERROR, guardado.getEstadoGeneracion());
        assertEquals("Sin conexión con el SRI", guardado.getErrorMensaje());
        verify(auditoria).registrar(eq("ERROR_COMPROBANTE_SRI"), eq("FACTURACION"),
                eq("COMPROBANTE_ELECTRONICO"), eq(guardado.getId()), isNull(),
                argThat((Map<String, Object> datos) -> "Sin conexión con el SRI".equals(datos.get("error"))
                        && EstadoGeneracion.ERROR.name().equals(datos.get("estadoGeneracion"))));
    }

    @Test
    void noEmiteCuentasAnuladas() {
        cuenta.setEstado(EstadoCuenta.ANULADA);

        BusinessException error = assertThrows(BusinessException.class,
                () -> servicio.emitir(new EmitirFacturaRequest(1L, null, null)));

        assertEquals("CUENTA_ANULADA", error.getCode());
        verificarQueNoSeGeneroNada();
    }

    // ------------------------------------------------------------------
    // Bandeja (§3.7): listado, XML, reenvío y consulta
    // ------------------------------------------------------------------

    @Test
    void listaLaBandejaConElNombreDelPaciente() {
        ComprobanteElectronico emitido = comprobanteEmitido();
        emitido.setId(9L);
        emitido.setEstadoTributario(EstadoTributario.AUTORIZADO);
        emitido.setTotal(new BigDecimal("150.00"));
        when(comprobanteRepo.buscar(any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(emitido)));
        when(pacienteRepo.idsPorTexto("ana")).thenReturn(List.of(10L));

        Paciente paciente = new Paciente();
        paciente.setId(10L);
        paciente.setNombres("Ana");
        paciente.setApellidos("Pérez");
        paciente.setCedula("1712345678");
        when(pacienteRepo.findAllById(any())).thenReturn(List.of(paciente));

        Page<ComprobanteResumenResponse> pagina = servicio.listar(
                new ComprobanteFiltroRequest("ana", null, null, null, null, null,
                        null, null, null, 0, 20));

        assertEquals(1, pagina.getTotalElements());
        ComprobanteResumenResponse fila = pagina.getContent().get(0);
        assertEquals("01", fila.tipo());
        assertEquals("001-001-000000001", fila.numeroComprobante());
        assertEquals("Ana Pérez", fila.paciente());
        assertEquals("1712345678", fila.identificacion());
        assertEquals("AUTORIZADO", fila.estadoTributario());
        verify(pacienteRepo).idsPorTexto("ana");
    }

    @Test
    void laBandejaSinTextoNoBuscaPacientes() {
        when(comprobanteRepo.buscar(any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        Page<ComprobanteResumenResponse> pagina = servicio.listar(
                new ComprobanteFiltroRequest("", null, null, null, null, null,
                        null, null, null, null, null));

        assertEquals(0, pagina.getTotalElements());
        verify(pacienteRepo, never()).idsPorTexto(any());
    }

    @Test
    void laBandejaRechazaEstadosDesconocidos() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> servicio.listar(new ComprobanteFiltroRequest(null, null, null, null,
                        "CUALQUIERA", null, null, null, null, 0, 20)));

        assertEquals("ESTADO_INVALIDO", error.getCode());
    }

    @Test
    void laBandejaRechazaTiposDesconocidos() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> servicio.listar(new ComprobanteFiltroRequest(null, null, null, "99",
                        null, null, null, null, null, 0, 20)));

        assertEquals("TIPO_COMPROBANTE_INVALIDO", error.getCode());
    }

    @Test
    void descargaElXmlAutorizadoSiExiste() {
        ComprobanteElectronico comprobante = comprobanteEmitido();
        comprobante.setXmlFirmado("<factura>firmado</factura>");
        comprobante.setXmlAutorizado("<factura>autorizado</factura>");
        when(comprobanteRepo.findById(5L)).thenReturn(Optional.of(comprobante));

        assertEquals("<factura>autorizado</factura>", servicio.xml(5L));
        assertEquals("comprobante-001-001-000000001.xml", servicio.nombreXml(5L));
    }

    @Test
    void fallaElXmlSiElComprobanteNoTiene() {
        when(comprobanteRepo.findById(6L)).thenReturn(Optional.of(comprobanteEmitido()));

        BusinessException error = assertThrows(BusinessException.class, () -> servicio.xml(6L));

        assertEquals("COMPROBANTE_SIN_XML", error.getCode());
    }

    @Test
    void noReenviaComprobantesAutorizados() {
        ComprobanteElectronico comprobante = comprobanteEmitido();
        comprobante.setEstadoTributario(EstadoTributario.AUTORIZADO);
        when(comprobanteRepo.findById(5L)).thenReturn(Optional.of(comprobante));

        BusinessException error = assertThrows(BusinessException.class,
                () -> servicio.reenviar(5L));

        assertEquals("COMPROBANTE_YA_AUTORIZADO", error.getCode());
        verify(authorizationService, never()).enviar(any(ComprobanteElectronico.class));
    }

    @Test
    void noReenviaLosQueYaEstanEnviados() {
        when(comprobanteRepo.findById(5L)).thenReturn(Optional.of(comprobanteEmitido()));

        BusinessException error = assertThrows(BusinessException.class,
                () -> servicio.reenviar(5L));

        assertEquals("COMPROBANTE_NO_REINTENTABLE", error.getCode());
        verify(authorizationService, never()).enviar(any(ComprobanteElectronico.class));
    }

        @Test
        void noReenviaNiConsultaUnaPruebaLocal() {
                ComprobanteElectronico comprobante = comprobanteEmitido();
                comprobante.setModoFirma(ElectronicSignatureService.MODO_SIMULADA);
                comprobante.setSimulado(true);
                when(comprobanteRepo.findById(5L)).thenReturn(Optional.of(comprobante));

                BusinessException error = assertThrows(BusinessException.class, () -> servicio.reenviar(5L));

                assertEquals("COMPROBANTE_SIMULADO_NO_REINTENTABLE", error.getCode());
                verify(authorizationService, never()).enviar(any(ComprobanteElectronico.class));
                verify(authorizationService, never()).autorizar(any(ComprobanteElectronico.class));
        }

        @Test
        void noGeneraRideParaComprobanteSimuladoNiPendiente() {
                ComprobanteElectronico comprobante = comprobanteEmitido();
                comprobante.setSimulado(true);
                comprobante.setXmlFirmado("<factura/>");
                when(comprobanteRepo.findById(5L)).thenReturn(Optional.of(comprobante));

                BusinessException error = assertThrows(BusinessException.class, () -> servicio.ride(5L));

                assertEquals("RIDE_NO_AUTORIZADO", error.getCode());
        }

        @Test
        void normalizaAutorizacionSimuladaHistoricaEnRespuestas() {
                ComprobanteElectronico comprobante = comprobanteEmitido();
                comprobante.setSimulado(true);
                comprobante.setEstadoGeneracion(EstadoGeneracion.ENVIADO);
                comprobante.setEstadoTributario(EstadoTributario.AUTORIZADO);
                comprobante.setAutorizacionNumero(comprobante.getClaveAcceso());
                comprobante.setAutorizacionFecha(java.time.Instant.now());
                comprobante.setXmlAutorizado("<factura>simulada</factura>");

                ComprobanteElectronicoResponse respuesta = ComprobanteElectronicoResponse.from(comprobante);

                // §3.8: el estado de generación sí se conserva (el XML se
                // generó y firmó); lo que no se puede exponer como hecho es
                // la autorización, porque en modo simulado el SRI nunca
                // respondió. Por eso se degrada el estado tributario.
                assertEquals(EstadoGeneracion.ENVIADO.name(), respuesta.estadoGeneracion());
                assertEquals(EstadoTributario.PENDIENTE.name(), respuesta.estadoTributario());
                assertNull(respuesta.autorizacionNumero());
                assertNull(respuesta.autorizacionFecha());
                assertTrue(respuesta.sriRespuesta().contains("no existe recepción"));
        }

    @Test
    void reenviaUnComprobanteQueFallo() {
        ComprobanteElectronico comprobante = comprobanteEmitido();
        comprobante.setEstadoGeneracion(EstadoGeneracion.ERROR);
        comprobante.setErrorMensaje("Sin conexión con el SRI");
        when(comprobanteRepo.findById(5L)).thenReturn(Optional.of(comprobante));

        ComprobanteElectronicoResponse respuesta = servicio.reenviar(5L);

        assertEquals("AUTORIZADO", respuesta.estadoTributario());
        verify(authorizationService).enviar(comprobante);
        verify(authorizationService).autorizar(comprobante);
    }

    @Test
    void consultarDevuelveElEstadoQueDiceElSri() {
        when(comprobanteRepo.findById(5L)).thenReturn(Optional.of(comprobanteEmitido()));

        ComprobanteElectronicoResponse respuesta = servicio.consultar(5L);

        assertEquals("AUTORIZADO", respuesta.estadoTributario());
        verify(authorizationService).autorizar(any(ComprobanteElectronico.class));
    }

    // ------------------------------------------------------------------
    // Fase H.1 — nota de crédito (§3.9) y anulación (§3.10)
    // ------------------------------------------------------------------

    @Test
    void emiteUnaNotaDeCreditoQueReferenciaLaFacturaAutorizada() {
        servicio.emitir(new EmitirFacturaRequest(1L, null, null));
        ComprobanteElectronico original = facturaAutorizada();

        ComprobanteElectronicoResponse respuesta = servicio.emitirNotaCredito(
                new EmitirNotaCreditoRequest(original.getId(),
                        "Devolución total por error de diagnóstico", null, null));

        assertEquals("04", respuesta.claveAcceso().substring(8, 10), "el tipo de documento es 04");
        assertEquals("001-001-000000001", respuesta.numeroComprobante(),
                "las notas de crédito tienen su propio secuencial");
        assertEquals(original.getId(), respuesta.comprobanteReferenciaId());
        assertEquals("Devolución total por error de diagnóstico", respuesta.motivo());
        assertEquals("04", respuesta.tipo());
        assertEquals(0, respuesta.total().compareTo(new BigDecimal("150.00")),
                "la devolución total devuelve lo que se facturó");
        assertEquals(1, config.getSecuencialNotaCredito(), "consume el secuencial de notas de crédito");
        assertEquals(1, config.getSecuencialActual(), "no toca el secuencial de facturas");
        assertEquals(EstadoTributario.AUTORIZADO.name(), respuesta.estadoTributario());

        assertNotNull(xmlAlGuardar);
        assertDoesNotThrow(() -> new SriXmlValidator().validar(xmlAlGuardar));
        assertTrue(xmlAlGuardar.contains("<notaCredito id=\"comprobante\" version=\"1.1.0\">"));
        assertTrue(xmlAlGuardar.contains("<codDocModificado>01</codDocModificado>"));
        assertTrue(xmlAlGuardar.contains("<numDocModificado>001-001-000000001</numDocModificado>"));
        assertTrue(xmlAlGuardar.contains("<motivo>Devolución total por error de diagnóstico</motivo>"));
        assertTrue(xmlAlGuardar.contains("<valorModificacion>150.00</valorModificacion>"));
        assertFalse(xmlAlGuardar.contains("<importeTotal>"), "la nota no tiene importe total de factura");
        verify(auditoria).registrar(eq("GENERAR_NOTA_CREDITO_SRI"), eq("FACTURACION"),
                eq("COMPROBANTE_ELECTRONICO"), eq(guardado.getId()), isNull(), any());
    }

    @Test
    void emiteUnaNotaDeCreditoParcial() {
        servicio.emitir(new EmitirFacturaRequest(1L, null, null));
        ComprobanteElectronico original = facturaAutorizada();

        ComprobanteElectronicoResponse respuesta = servicio.emitirNotaCredito(
                new EmitirNotaCreditoRequest(original.getId(), "Se devuelve la profilaxis",
                        List.of(new EmitirNotaCreditoRequest.Devolucion(2, new BigDecimal("2"))), null));

        assertEquals(0, respuesta.total().compareTo(new BigDecimal("50.00")),
                "la línea 2 vale 50.00 con IVA incluido");
        assertTrue(xmlAlGuardar.contains("<valorModificacion>50.00</valorModificacion>"));
        assertTrue(xmlAlGuardar.contains("<cantidad>2</cantidad>"));
        assertFalse(xmlAlGuardar.contains("Consulta odontológica"),
                "la línea no devuelta no aparece en la nota");
    }

    @Test
    void noEmiteNotaDeCreditoSobreUnaFacturaNoAutorizada() {
        ComprobanteElectronico original = comprobanteEmitido();
        when(comprobanteRepo.buscarParaModificar(5L)).thenReturn(Optional.of(original));

        BusinessException error = assertThrows(BusinessException.class, () ->
                servicio.emitirNotaCredito(new EmitirNotaCreditoRequest(5L,
                        "Devolución total", null, null)));

        assertEquals("COMPROBANTE_NO_AUTORIZADO", error.getCode());
        assertEquals(0, config.getSecuencialNotaCredito());
        verify(comprobanteRepo, never()).save(any(ComprobanteElectronico.class));
    }

    @Test
    void noEmiteNotaDeCreditoSobreOtroTipoDeComprobante() {
        ComprobanteElectronico original = comprobanteEmitido();
        original.setTipoComprobante(TipoComprobanteSri.NOTA_CREDITO);
        original.setEstadoTributario(EstadoTributario.AUTORIZADO);
        when(comprobanteRepo.buscarParaModificar(5L)).thenReturn(Optional.of(original));

        BusinessException error = assertThrows(BusinessException.class, () ->
                servicio.emitirNotaCredito(new EmitirNotaCreditoRequest(5L,
                        "Devolución total", null, null)));

        assertEquals("TIPO_COMPROBANTE_INVALIDO", error.getCode());
    }

    @Test
    void noDevuelveMasDeLoFacturado() {
        servicio.emitir(new EmitirFacturaRequest(1L, null, null));
        ComprobanteElectronico original = facturaAutorizada();
        ComprobanteElectronico previa = new ComprobanteElectronico();
        previa.setTotal(new BigDecimal("150.00"));
        previa.setEstadoTributario(EstadoTributario.AUTORIZADO);
        when(comprobanteRepo.findByComprobanteReferenciaIdOrderByCreatedAtDesc(original.getId()))
                .thenReturn(List.of(previa));

        BusinessException error = assertThrows(BusinessException.class, () ->
                servicio.emitirNotaCredito(new EmitirNotaCreditoRequest(original.getId(),
                        "Otra devolución más", null, null)));

        assertEquals("NOTA_CREDITO_EXCEDE", error.getCode());
        assertEquals(TipoComprobanteSri.FACTURA, guardado.getTipoComprobante(),
                "no se llegó a guardar la nota que excede el total");
    }

    @Test
    void noEmiteNotaDeCreditoSinMotivo() {
        servicio.emitir(new EmitirFacturaRequest(1L, null, null));
        ComprobanteElectronico original = facturaAutorizada();

        BusinessException error = assertThrows(BusinessException.class, () ->
                servicio.emitirNotaCredito(new EmitirNotaCreditoRequest(original.getId(),
                        "  ", null, null)));

        assertEquals("MOTIVO_REQUERIDO", error.getCode());
        assertEquals(0, config.getSecuencialNotaCredito());
    }

    @Test
        void noMarcaAnulacionComoAplicadaSinRespuestaDelSri() {
        ComprobanteElectronico comprobante = comprobanteEmitido();
        comprobante.setEstadoTributario(EstadoTributario.AUTORIZADO);
        when(comprobanteRepo.buscarParaModificar(5L)).thenReturn(Optional.of(comprobante));

        BusinessException error = assertThrows(BusinessException.class, () -> servicio.anular(5L,
                new AnularComprobanteRequest("Paciente canceló el tratamiento")));

        assertEquals("SRI_ANULACION_NO_DISPONIBLE", error.getCode());
        assertEquals(EstadoTributario.AUTORIZADO, comprobante.getEstadoTributario());
        verify(comprobanteRepo, never()).save(comprobante);
        verify(anulacionRepo, never()).save(any(AnulacionComprobante.class));
    }

    @Test
    void noAnulaUnComprobanteQueNoEstaAutorizado() {
        ComprobanteElectronico comprobante = comprobanteEmitido();
        when(comprobanteRepo.buscarParaModificar(5L)).thenReturn(Optional.of(comprobante));

        BusinessException error = assertThrows(BusinessException.class,
                () -> servicio.anular(5L, new AnularComprobanteRequest("Motivo cualquiera")));

        assertEquals("COMPROBANTE_NO_AUTORIZADO", error.getCode());
        verify(anulacionRepo, never()).save(any(AnulacionComprobante.class));
    }

    @Test
    void noAnulaDosVecesElMismoComprobante() {
        ComprobanteElectronico comprobante = comprobanteEmitido();
        comprobante.setEstadoTributario(EstadoTributario.ANULADO);
        when(comprobanteRepo.buscarParaModificar(5L)).thenReturn(Optional.of(comprobante));

        BusinessException error = assertThrows(BusinessException.class,
                () -> servicio.anular(5L, new AnularComprobanteRequest("Motivo cualquiera")));

        assertEquals("COMPROBANTE_YA_ANULADO", error.getCode());
        verify(anulacionRepo, never()).save(any(AnulacionComprobante.class));
    }

    @Test
    void noAnulaSiYaHayUnTramiteVigente() {
        ComprobanteElectronico comprobante = comprobanteEmitido();
        comprobante.setEstadoTributario(EstadoTributario.AUTORIZADO);
        when(comprobanteRepo.buscarParaModificar(5L)).thenReturn(Optional.of(comprobante));
        when(anulacionRepo.findFirstByComprobanteIdAndEstadoInOrderBySolicitadoEnDesc(eq(5L), any()))
                .thenReturn(Optional.of(new AnulacionComprobante()));

        BusinessException error = assertThrows(BusinessException.class,
                () -> servicio.anular(5L, new AnularComprobanteRequest("Motivo cualquiera")));

        assertEquals("ANULACION_YA_SOLICITADA", error.getCode());
    }

    @Test
    void laCuentaSeVuelveAFacturarSiElComprobanteQuedoAnulado() {
        ComprobanteElectronico anulado = comprobanteEmitido();
        anulado.setEstadoTributario(EstadoTributario.ANULADO);
        when(comprobanteRepo.findByCuentaIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(anulado));

        ComprobanteElectronicoResponse respuesta = servicio.emitir(
                new EmitirFacturaRequest(1L, null, null));

        assertEquals("001-001-000000001", respuesta.numeroComprobante());
        assertEquals(EstadoTributario.PENDIENTE.name(), respuesta.estadoTributario());
    }

    @Test
    void detalleDevuelveLasLineasDelXmlParaUnaDevolucionParcial() {
        servicio.emitir(new EmitirFacturaRequest(1L, null, null));
        guardado.setId(9L);
        when(comprobanteRepo.findById(9L)).thenReturn(Optional.of(guardado));

        ComprobanteElectronicoResponse detalle = servicio.detalle(9L);

        assertEquals(2, detalle.detalles().size(), "la factura tiene dos líneas");
        assertEquals(1L, detalle.detalles().get(0).linea().longValue(),
                "las líneas se numeran desde 1, como las pide la nota");
        assertEquals("Consulta odontológica", detalle.detalles().get(0).descripcion());
        assertEquals(0, detalle.detalles().get(1).total()
                        .add(detalle.detalles().get(1).impuesto())
                        .compareTo(new BigDecimal("50.00")),
                "la línea 2 vale 50.00 con IVA incluido (43.48 + 6.52)");
        assertEquals(0, detalle.detalles().stream()
                        .map(l -> l.total().add(l.impuesto()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .compareTo(detalle.total()),
                "las líneas suman el total que muestran los estados");
    }

    @Test
    void detalleSinXmlDevuelveLasLineasVacias() {
        when(comprobanteRepo.findById(6L)).thenReturn(Optional.of(comprobanteEmitido()));

        ComprobanteElectronicoResponse detalle = servicio.detalle(6L);

        assertTrue(detalle.detalles().isEmpty());
        assertNull(detalle.comprobanteReferenciaId());
        assertNull(detalle.motivo());
    }

    // ------------------------------------------------------------------

    /** La factura emitida arriba, con id y estado autorizado por el SRI. */
    private ComprobanteElectronico facturaAutorizada() {
        guardado.setId(5L);
                guardado.setModoFirma(ElectronicSignatureService.MODO_CERTIFICADO);
                guardado.setSimulado(false);
                guardado.setEstadoGeneracion(EstadoGeneracion.ENVIADO);
                guardado.setEstadoTributario(EstadoTributario.AUTORIZADO);
                guardado.setAutorizacionNumero(guardado.getClaveAcceso());
                guardado.setXmlAutorizado(guardado.getXmlFirmado());
                modoFirmaFixture = ElectronicSignatureService.MODO_CERTIFICADO;
        when(comprobanteRepo.buscarParaModificar(5L)).thenReturn(Optional.of(guardado));
        when(comprobanteRepo.findByComprobanteReferenciaIdOrderByCreatedAtDesc(5L)).thenReturn(List.of());
        return guardado;
    }

    private ComprobanteElectronico comprobanteEmitido() {
        ComprobanteElectronico comprobante = new ComprobanteElectronico();
        comprobante.setId(5L);
        comprobante.setTipoComprobante(TipoComprobanteSri.FACTURA);
        comprobante.setNumeroComprobante("001-001-000000001");
        comprobante.setClaveAcceso("0810202601040150117600110010010000000013522407211");
        comprobante.setPacienteId(10L);
        comprobante.setCuentaId(1L);
        comprobante.setEstadoGeneracion(EstadoGeneracion.ENVIADO);
        comprobante.setEstadoTributario(EstadoTributario.PENDIENTE);
        return comprobante;
    }

    private void verificarQueNoSeGeneroNada() {
        assertEquals(0, config.getSecuencialActual());
        verify(configRepo, never()).save(any(ConfiguracionFacturacion.class));
        verify(comprobanteRepo, never()).save(any(ComprobanteElectronico.class));
    }

    private static CuentaDetalle movimiento(TipoMovimientoCuenta tipo, String concepto,
                                            Integer cantidad, String precio, String total,
                                            FormaPago formaPago) {
        CuentaDetalle movimiento = new CuentaDetalle();
        movimiento.setTipo(tipo);
        movimiento.setConcepto(concepto);
        movimiento.setCantidad(cantidad);
        if (precio != null) movimiento.setPrecioUnitario(new BigDecimal(precio));
        movimiento.setTotal(new BigDecimal(total));
        movimiento.setFormaPago(formaPago);
        return movimiento;
    }
}
