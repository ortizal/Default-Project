package org.dentalcrm.web.facturacion;

import org.dentalcrm.domain.comprobante.AnulacionComprobante;
import org.dentalcrm.domain.comprobante.AnulacionComprobanteRepository;
import org.dentalcrm.domain.comprobante.ComprobanteElectronico;
import org.dentalcrm.domain.comprobante.ComprobanteElectronicoRepository;
import org.dentalcrm.domain.comprobante.EstadoGeneracion;
import org.dentalcrm.domain.comprobante.EstadoTributario;
import org.dentalcrm.domain.comprobante.TipoComprobanteSri;
import org.dentalcrm.domain.configuracion.AmbienteSri;
import org.dentalcrm.domain.configuracion.ConfiguracionFacturacion;
import org.dentalcrm.domain.configuracion.ConfiguracionFacturacionRepository;
import org.dentalcrm.domain.configuracion.Consultorio;
import org.dentalcrm.domain.configuracion.ConsultorioRepository;
import org.dentalcrm.domain.cuenta.CuentaDetalle;
import org.dentalcrm.domain.cuenta.CuentaDetalleRepository;
import org.dentalcrm.domain.cuenta.CuentaPaciente;
import org.dentalcrm.domain.cuenta.CuentaPacienteRepository;
import org.dentalcrm.domain.cuenta.EstadoCuenta;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.multitenant.TenantContext;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.CurrentUserService;
import org.dentalcrm.web.facturacion.dto.AnularComprobanteRequest;
import org.dentalcrm.web.facturacion.dto.ComprobanteElectronicoResponse;
import org.dentalcrm.web.facturacion.dto.ComprobanteLineaResponse;
import org.dentalcrm.web.facturacion.dto.ComprobanteFiltroRequest;
import org.dentalcrm.web.facturacion.dto.ComprobanteResumenResponse;
import org.dentalcrm.web.facturacion.dto.EmitirFacturaRequest;
import org.dentalcrm.web.facturacion.dto.EmitirNotaCreditoRequest;
import org.dentalcrm.web.facturacion.sri.AuthorizationService;
import org.dentalcrm.web.facturacion.sri.ClaveAccesoService;
import org.dentalcrm.web.facturacion.sri.DatosFactura;
import org.dentalcrm.web.facturacion.sri.ElectronicSignatureService;
import org.dentalcrm.web.facturacion.sri.FacturaBuilder;
import org.dentalcrm.web.facturacion.sri.NotaCreditoBuilder;
import org.dentalcrm.web.facturacion.sri.RideDatos;
import org.dentalcrm.web.facturacion.sri.RideGenerator;
import org.dentalcrm.web.facturacion.sri.RideXmlParser;
import org.dentalcrm.web.facturacion.sri.SriXmlGenerator;
import org.dentalcrm.web.facturacion.sri.SriXmlValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Emisión de la factura electrónica — plan §3.5/§3.6
 * {@code ComprobanteElectronicoService}.
 *
 * <p>El flujo completo, en este orden:
 * <pre>
 * cuenta → validación → secuencial (con bloqueo) → clave de acceso →
 * datos → XML → XSD → firma → guardar FIRMADO → envío → autorización
 * </pre>
 *
 * <p>El secuencial se reserva con bloqueo de escritura sobre la
 * configuración, así que dos emisiones simultáneas nunca comparten número.
 * La generación ocurre en su propia transacción: si algo falla antes de
 * firmar, no queda comprobante ni se consume el secuencial. El envío y la
 * autorización van después, fuera de esa transacción, para que un fallo de
 * red no arrastre el comprobante ya generado (queda en {@code ERROR} con su
 * mensaje, listo para reintentar desde la bandeja).
 */
@Service
public class ComprobanteElectronicoService {

    private static final String MODULO = "FACTURACION";
    private static final String ENTIDAD = "COMPROBANTE_ELECTRONICO";
    private static final int SECUENCIAL_MAXIMO = 999_999_999;
    private static final String IDENTIFICACION_PUBLICO = "9999999999001";
    private static final String PATRON_RUC = "[0-9]{10}001";

    private final CuentaPacienteRepository cuentaRepository;
    private final CuentaDetalleRepository detalleRepository;
    private final ConsultorioRepository consultorioRepository;
    private final ConfiguracionFacturacionRepository configuracionRepository;
    private final ComprobanteElectronicoRepository comprobanteRepository;
    private final AnulacionComprobanteRepository anulacionRepository;
    private final PacienteRepository pacienteRepository;
    private final ClaveAccesoService claveAccesoService;
    private final SriXmlValidator validador;
    private final ElectronicSignatureService firmaService;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;
    private final RideGenerator rideGenerator;
    private final TransactionTemplate transaccion;

    public ComprobanteElectronicoService(CuentaPacienteRepository cuentaRepository,
                                         CuentaDetalleRepository detalleRepository,
                                         ConsultorioRepository consultorioRepository,
                                         ConfiguracionFacturacionRepository configuracionRepository,
                                         ComprobanteElectronicoRepository comprobanteRepository,
                                         AnulacionComprobanteRepository anulacionRepository,
                                         PacienteRepository pacienteRepository,
                                         ClaveAccesoService claveAccesoService,
                                         SriXmlValidator validador,
                                         ElectronicSignatureService firmaService,
                                         AuthorizationService authorizationService,
                                         AuditService auditService,
                                         CurrentUserService currentUserService,
                                         RideGenerator rideGenerator,
                                         PlatformTransactionManager gestorTransacciones) {
        this.cuentaRepository = cuentaRepository;
        this.detalleRepository = detalleRepository;
        this.consultorioRepository = consultorioRepository;
        this.configuracionRepository = configuracionRepository;
        this.comprobanteRepository = comprobanteRepository;
        this.anulacionRepository = anulacionRepository;
        this.pacienteRepository = pacienteRepository;
        this.claveAccesoService = claveAccesoService;
        this.validador = validador;
        this.firmaService = firmaService;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
        this.rideGenerator = rideGenerator;
        this.transaccion = new TransactionTemplate(gestorTransacciones);
    }

    /**
     * Emite la factura de una cuenta: genera el XML, lo valida contra el
     * XSD, lo firma, lo guarda y lo lleva hasta la autorización del SRI.
     *
     * <p>En ambiente de pruebas sin certificado la firma es simulada: el
     * webservice del SRI no se llama y el comprobante sale marcado como
     * {@code simulado} (ver {@link AuthorizationService}).
     */
    public ComprobanteElectronicoResponse emitir(EmitirFacturaRequest request) {
        ComprobanteElectronico comprobante = transaccion.execute(estado -> generar(request));
        if (comprobante == null) {
            throw new BusinessException("COMPROBANTE_NO_GENERADO", "No se pudo generar el comprobante");
        }
        try {
            comprobante = authorizationService.enviar(comprobante);
            comprobante = authorizationService.autorizar(comprobante);
        } catch (RuntimeException e) {
            registrarError(comprobante, e);
            throw e;
        }
        return ComprobanteElectronicoResponse.from(comprobante);
    }

    /**
     * Emite una nota de crédito sobre una factura autorizada (plan §3.9):
     * devolución total —sin líneas— o parcial, con el mismo recorrido que
     * la factura (clave → XML → XSD → firma → envío → autorización).
     *
     * <p>La factura original se carga con bloqueo de escritura, así que dos
     * notas simultáneas nunca pasan ambas por «dentro del total» y no se
     * puede devolver más de lo facturado.
     */
    public ComprobanteElectronicoResponse emitirNotaCredito(EmitirNotaCreditoRequest request) {
        String motivo = request.motivo() == null ? "" : request.motivo().trim();
        if (motivo.isEmpty()) {
            throw new BusinessException("MOTIVO_REQUERIDO", "La nota de crédito necesita un motivo");
        }
        if (motivo.length() > 300) {
            throw new BusinessException("MOTIVO_INVALIDO", "El motivo no puede superar los 300 caracteres");
        }
        ComprobanteElectronico comprobante = transaccion.execute(estado -> generarNotaCredito(request));
        if (comprobante == null) {
            throw new BusinessException("COMPROBANTE_NO_GENERADO", "No se pudo generar la nota de crédito");
        }
        try {
            comprobante = authorizationService.enviar(comprobante);
            comprobante = authorizationService.autorizar(comprobante);
        } catch (RuntimeException e) {
            registrarError(comprobante, e);
            throw e;
        }
        return ComprobanteElectronicoResponse.from(comprobante);
    }

    /** Bloquea la anulación mientras no exista integración con respuesta verificable del SRI. */
    public ComprobanteElectronicoResponse anular(Long id, AnularComprobanteRequest request) {
        String motivo = request.motivo() == null ? "" : request.motivo().trim();
        if (motivo.isEmpty()) {
            throw new BusinessException("MOTIVO_REQUERIDO", "La anulación necesita un motivo");
        }
        ComprobanteElectronico comprobante = transaccion.execute(estado -> aplicarAnulacion(id, motivo));
        if (comprobante == null) {
            throw new BusinessException("COMPROBANTE_NO_GENERADO", "No se pudo anular el comprobante");
        }
        return ComprobanteElectronicoResponse.from(comprobante);
    }

    /** RIDE en PDF: solo se genera desde el XML autorizado devuelto por el SRI. */
    public byte[] ride(Long id) {
        ComprobanteElectronico comprobante = buscar(id);
        if (comprobante.isSimulado()
                || comprobante.getEstadoTributario() != EstadoTributario.AUTORIZADO
                || comprobante.getXmlAutorizado() == null
                || comprobante.getXmlAutorizado().isBlank()) {
            throw new BusinessException(HttpStatus.CONFLICT, "RIDE_NO_AUTORIZADO",
                    "El RIDE solo está disponible después de una autorización real del SRI");
        }
        return rideGenerator.generar(comprobante);
    }

    /** Nombre del archivo del RIDE, p. ej. {@code ride-001-001-000000001.pdf}. */
    public String nombreRide(Long id) {
        return rideGenerator.nombreArchivo(buscar(id));
    }

    // ------------------------------------------------------------------
    // Bandeja (§3.7)
    // ------------------------------------------------------------------

    /** Página de comprobantes con los filtros de la bandeja. */
    public Page<ComprobanteResumenResponse> listar(ComprobanteFiltroRequest filtro) {
        String q = filtro.q() == null ? "" : filtro.q().trim();
        List<Long> pacientes = q.isEmpty() ? List.of() : pacienteRepository.idsPorTexto(q);
        if (pacientes.isEmpty()) {
            // La consulta no admite listas vacías: -1 no casa con ningún id.
            pacientes = List.of(-1L);
        }
        Page<ComprobanteElectronico> pagina = comprobanteRepository.buscar(
                q, pacientes,
                filtro.desde() == null ? LocalDate.of(1900, 1, 1) : filtro.desde(),
                filtro.hasta() == null ? LocalDate.of(9999, 12, 31) : filtro.hasta(),
                tipoComprobante(filtro.tipo()),
                estadoTributario(filtro.estado()), estadoGeneracion(filtro.generacion()),
                filtro.pacienteId(),
                filtro.establecimiento() == null ? "" : filtro.establecimiento().trim(),
                filtro.usuarioId(),
                PageRequest.of(pagina(filtro), tamanio(filtro)));

        Map<Long, Paciente> porId = pacientesDe(pagina);
        return pagina.map(c -> ComprobanteResumenResponse.de(c, porId.get(c.getPacienteId())));
    }

    /**
     * Detalle completo del comprobante para la bandeja: además de los
     * estados, expone las líneas del XML (plan §3.9) para que la nota de
     * crédito parcial pueda elegir qué devuelve y con cuánto.
     */
    public ComprobanteElectronicoResponse detalle(Long id) {
        ComprobanteElectronico comprobante = buscar(id);
        return ComprobanteElectronicoResponse.detalle(comprobante, lineas(comprobante));
    }

    /** Líneas del XML del comprobante; vacías si aún no tiene XML legible. */
    private List<ComprobanteLineaResponse> lineas(ComprobanteElectronico comprobante) {
        String xml = xmlDe(comprobante);
        if (xml == null || xml.isBlank()) {
            return List.of();
        }
        try {
            List<RideDatos.Linea> lineas = RideXmlParser.parsear(xml).lineas();
            List<ComprobanteLineaResponse> resultado = new ArrayList<>(lineas.size());
            for (int i = 0; i < lineas.size(); i++) {
                resultado.add(ComprobanteLineaResponse.de(i + 1L, lineas.get(i)));
            }
            return List.copyOf(resultado);
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    /** XML autorizado (si existe) o firmado, listo para descargar. */
    public String xml(Long id) {
        ComprobanteElectronico comprobante = buscar(id);
        String xml = !comprobante.isSimulado() && comprobante.getXmlAutorizado() != null
                && !comprobante.getXmlAutorizado().isBlank()
                ? comprobante.getXmlAutorizado()
                : comprobante.getXmlFirmado();
        if (xml == null || xml.isBlank()) {
            throw new BusinessException("COMPROBANTE_SIN_XML",
                    "El comprobante aún no tiene XML para descargar");
        }
        return xml;
    }

    public String nombreXml(Long id) {
        ComprobanteElectronico comprobante = buscar(id);
        return "comprobante-" + (comprobante.getNumeroComprobante() == null
                ? comprobante.getClaveAcceso() : comprobante.getNumeroComprobante()) + ".xml";
    }

    /** Reenvía al SRI un comprobante real que falló antes de una respuesta definitiva. */
    public ComprobanteElectronicoResponse reenviar(Long id) {
        ComprobanteElectronico comprobante = buscar(id);
        AuthorizationService.exigirNoSimulado(comprobante);
        exigirReenvio(comprobante);
        comprobante = authorizationService.enviar(comprobante);
        comprobante = authorizationService.autorizar(comprobante);
        return ComprobanteElectronicoResponse.from(comprobante);
    }

    /** Consulta al SRI la autorización de un comprobante ya enviado. */
    public ComprobanteElectronicoResponse consultar(Long id) {
        ComprobanteElectronico comprobante = buscar(id);
        AuthorizationService.exigirNoSimulado(comprobante);
        return ComprobanteElectronicoResponse.from(authorizationService.autorizar(comprobante));
    }

    private void exigirReenvio(ComprobanteElectronico comprobante) {
        if ("AUTORIZADO".equalsIgnoreCase(comprobante.getSriEstado())
                || "EN_PROCESO".equalsIgnoreCase(comprobante.getSriEstado())) {
            throw new BusinessException(HttpStatus.CONFLICT, "COMPROBANTE_CONSULTA_REQUERIDA",
                    "El SRI pudo haber recibido el comprobante: consulte su autorización antes de reenviar");
        }
        if (comprobante.getEstadoTributario() == EstadoTributario.AUTORIZADO) {
            throw new BusinessException(HttpStatus.CONFLICT, "COMPROBANTE_YA_AUTORIZADO",
                    "El comprobante ya está autorizado por el SRI: no necesita reenvío");
        }
        if (comprobante.tributariamenteDefinitivo()) {
            throw new BusinessException(HttpStatus.CONFLICT, "COMPROBANTE_NO_REINTENTABLE",
                    "El comprobante ya tiene respuesta definitiva del SRI y no puede reenviarse");
        }
        if (comprobante.getEstadoGeneracion() == EstadoGeneracion.ENVIADO) {
            throw new BusinessException(HttpStatus.CONFLICT, "COMPROBANTE_NO_REINTENTABLE",
                    "El comprobante ya fue enviado: use la acción «Consultar SRI»");
        }
    }

    private int pagina(ComprobanteFiltroRequest filtro) {
        return filtro.page() == null || filtro.page() < 0 ? 0 : filtro.page();
    }

    private int tamanio(ComprobanteFiltroRequest filtro) {
        return filtro.size() == null || filtro.size() < 1 ? 20 : Math.min(filtro.size(), 100);
    }

    /** Codigo SRI del tipo de comprobante (01, 04, 05, 06, 07). */
    private TipoComprobanteSri tipoComprobante(String valor) {
        return valor == null || valor.isBlank()
                ? null
                : TipoComprobanteSri.porCodigo(valor.trim());
    }

    private EstadoTributario estadoTributario(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return EstadoTributario.valueOf(valor.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("ESTADO_INVALIDO", "Estado tributario desconocido: " + valor);
        }
    }

    private EstadoGeneracion estadoGeneracion(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return EstadoGeneracion.valueOf(valor.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("ESTADO_INVALIDO", "Estado de generación desconocido: " + valor);
        }
    }

    /** Carga los pacientes de la página para mostrar nombre e identificación. */
    private Map<Long, Paciente> pacientesDe(Page<ComprobanteElectronico> pagina) {
        Set<Long> ids = pagina.getContent().stream()
                .map(ComprobanteElectronico::getPacienteId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return pacienteRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Paciente::getId, p -> p, (a, b) -> a));
    }

    private ComprobanteElectronico buscar(Long id) {
        return comprobanteRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "COMPROBANTE_NO_ENCONTRADO", "El comprobante indicado no existe"));
    }

    // ------------------------------------------------------------------
    // Generación (transacción propia)
    // ------------------------------------------------------------------

    private ComprobanteElectronico generar(EmitirFacturaRequest request) {
        // Bloqueo de la cuenta: dos emisiones simultáneas no pueden pasar
        // ambas por "sin comprobante previo" y facturar la misma cuenta.
        CuentaPaciente cuenta = cuentaRepository.buscarParaFacturar(request.cuentaId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "CUENTA_NO_ENCONTRADA",
                        "La cuenta indicada no existe"));
        Paciente paciente = cuenta.getPaciente();
        if (cuenta.getEstado() == EstadoCuenta.ANULADA) {
            throw new BusinessException(HttpStatus.CONFLICT, "CUENTA_ANULADA",
                    "La cuenta está anulada y no puede facturarse");
        }
        exigirSinComprobantePrevio(cuenta.getId());

        ConfiguracionFacturacion config = configuracionRepository
                .buscarParaSecuencial(TenantContext.actualOrDefault())
                .orElseThrow(() -> new BusinessException("FACTURACION_NO_CONFIGURADA",
                        "Configure la facturación electrónica antes de emitir (Configuración → Facturación)"));
        Consultorio consultorio = consultorioRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new BusinessException("FACTURACION_INCOMPLETA",
                        "Falta el consultorio: registre sus datos fiscales en Configuración → Consultorio"));
        exigirDatosFiscales(consultorio);
        exigirFirmaDisponible(config, consultorio);

        List<FacturaBuilder.LineaCuenta> cargos = new ArrayList<>();
        List<FacturaBuilder.Cobro> cobros = new ArrayList<>();
        BigDecimal descuentoCuenta = BigDecimal.ZERO;
        for (CuentaDetalle movimiento : detalleRepository.findByCuentaIdOrderByFechaAscIdAsc(cuenta.getId())) {
            if (movimiento.isAnulado()) continue;
            switch (movimiento.getTipo()) {
                case CARGO -> cargos.add(new FacturaBuilder.LineaCuenta(movimiento.getConcepto(),
                        cantidad(movimiento.getCantidad()), nz(movimiento.getPrecioUnitario()),
                        nz(movimiento.getTotal())));
                case DESCUENTO -> descuentoCuenta = descuentoCuenta.add(nz(movimiento.getTotal()));
                case ABONO, ANTICIPO -> cobros.add(new FacturaBuilder.Cobro(movimiento.getFormaPago(),
                        nz(movimiento.getTotal())));
                case DEVOLUCION -> cobros.add(new FacturaBuilder.Cobro(movimiento.getFormaPago(),
                        nz(movimiento.getTotal()).negate()));
            }
        }

        // Sin cargos no hay factura: se comprueba antes de reservar el número.
        if (cargos.isEmpty()) {
            throw new BusinessException("SIN_DETALLE", "La cuenta no tiene cargos para facturar");
        }

        int secuencial = reservarSecuencial(config);
        ClaveAccesoService.ClaveDeAcceso clave = claveAccesoService.generar(
                request.fechaEmision() == null ? LocalDate.now() : request.fechaEmision(),
                consultorio.getRuc().trim(), ClaveAccesoService.COD_DOC_FACTURA, config.getAmbiente(),
                config.getEstablecimiento(), config.getPuntoEmision(), secuencial, config.getTipoEmision());

        FacturaBuilder.Entrada entrada = new FacturaBuilder.Entrada(
                clave.valor(), clave.fechaEmision(),
                emisor(consultorio, config, secuencial), comprador(paciente),
                cargos, descuentoCuenta, cobros, pagos(request), adicionales(paciente));
        FacturaBuilder.FacturaConstruida construida = FacturaBuilder.construir(entrada);

        String xml = SriXmlGenerator.generarFactura(construida.datos());
        validador.validar(xml);
        ElectronicSignatureService.FirmaResultado firma =
                firmaService.firmar(xml, consultorio, config.getFirmaPassword());
        validador.validar(firma.xml());

        ComprobanteElectronico comprobante = new ComprobanteElectronico();
        comprobante.setClaveAcceso(clave.valor());
        comprobante.setEstablecimiento(config.getEstablecimiento());
        comprobante.setPuntoEmision(config.getPuntoEmision());
        comprobante.setSecuencial(secuencial);
        comprobante.setNumeroComprobante(numeroComprobante(config, secuencial));
        comprobante.setAmbiente(config.getAmbiente());
        comprobante.setModoFirma(firma.modo());
        comprobante.setEstadoGeneracion(EstadoGeneracion.FIRMADO);
        comprobante.setEstadoTributario(EstadoTributario.PENDIENTE);
        comprobante.setSimulado(false);
        comprobante.setXmlFirmado(firma.xml());
        comprobante.setTotal(construida.totales().total());
        comprobante.setFechaEmision(clave.fechaEmision());
        comprobante.setCuentaId(cuenta.getId());
        comprobante.setPacienteId(paciente.getId());
        comprobante.setCreatedBy(currentUserService.idUsuarioActual().orElse(null));

        ComprobanteElectronico guardado = comprobanteRepository.save(comprobante);
        auditService.registrar("GENERAR_COMPROBANTE_SRI", MODULO, ENTIDAD, guardado.getId(),
                null, guardado.datosAuditoria());
        return guardado;
    }

    /** Reserva el siguiente número bajo bloqueo de escritura de la configuración. */
    private int reservarSecuencial(ConfiguracionFacturacion config) {
        int secuencial = config.getSecuencialActual() + 1;
        if (secuencial > SECUENCIAL_MAXIMO) {
            throw new BusinessException("SECUENCIAL_AGOTADO",
                    "Se agotó el secuencial del punto de emisión "
                            + config.getEstablecimiento() + "-" + config.getPuntoEmision());
        }
        config.setSecuencialActual(secuencial);
        configuracionRepository.save(config);
        return secuencial;
    }

    /**
     * Genera la nota de crédito dentro de su propia transacción: si algo
     * falla antes de firmar, no queda nota y no se consume el secuencial
     * (igual que la factura).
     */
    private ComprobanteElectronico generarNotaCredito(EmitirNotaCreditoRequest request) {
        ComprobanteElectronico original = comprobanteRepository.buscarParaModificar(request.comprobanteId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "COMPROBANTE_NO_ENCONTRADO",
                        "El comprobante indicado no existe"));
        exigirNotaCreditoSobre(original);

        ConfiguracionFacturacion config = configuracionRepository
                .buscarParaSecuencial(TenantContext.actualOrDefault())
                .orElseThrow(() -> new BusinessException("FACTURACION_NO_CONFIGURADA",
                        "Configure la facturación electrónica antes de emitir (Configuración → Facturación)"));
        Consultorio consultorio = consultorioRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new BusinessException("FACTURACION_INCOMPLETA",
                        "Falta el consultorio: registre sus datos fiscales en Configuración → Consultorio"));
        exigirDatosFiscales(consultorio);
        exigirFirmaDisponible(config, consultorio);

        int secuencial = reservarSecuencialNotaCredito(config);
        ClaveAccesoService.ClaveDeAcceso clave = claveAccesoService.generar(
                request.fechaEmision() == null ? LocalDate.now() : request.fechaEmision(),
                consultorio.getRuc().trim(), ClaveAccesoService.COD_DOC_NOTA_CREDITO, config.getAmbiente(),
                config.getEstablecimiento(), config.getPuntoEmision(), secuencial, config.getTipoEmision());

        NotaCreditoBuilder.NotaConstruida construida = NotaCreditoBuilder.construir(
                new NotaCreditoBuilder.Entrada(clave.valor(), clave.fechaEmision(),
                        emisor(consultorio, config, secuencial, ClaveAccesoService.COD_DOC_NOTA_CREDITO),
                        xmlDe(original), request.motivo(), devoluciones(request)));
        exigirDentroDelTotal(original, construida.total());

        String xml = SriXmlGenerator.generarNotaCredito(construida.datos());
        validador.validar(xml);
        ElectronicSignatureService.FirmaResultado firma =
                firmaService.firmar(xml, consultorio, config.getFirmaPassword());
        validador.validar(firma.xml());

        ComprobanteElectronico nota = new ComprobanteElectronico();
        nota.setTipoComprobante(TipoComprobanteSri.NOTA_CREDITO);
        nota.setClaveAcceso(clave.valor());
        nota.setEstablecimiento(config.getEstablecimiento());
        nota.setPuntoEmision(config.getPuntoEmision());
        nota.setSecuencial(secuencial);
        nota.setNumeroComprobante(numeroComprobante(config, secuencial));
        nota.setAmbiente(config.getAmbiente());
        nota.setModoFirma(firma.modo());
        nota.setEstadoGeneracion(EstadoGeneracion.FIRMADO);
        nota.setEstadoTributario(EstadoTributario.PENDIENTE);
        nota.setSimulado(false);
        nota.setXmlFirmado(firma.xml());
        nota.setTotal(construida.total());
        nota.setFechaEmision(clave.fechaEmision());
        nota.setCuentaId(original.getCuentaId());
        nota.setPacienteId(original.getPacienteId());
        nota.setComprobanteReferenciaId(original.getId());
        nota.setMotivo(request.motivo().trim());
        nota.setCreatedBy(currentUserService.idUsuarioActual().orElse(null));

        ComprobanteElectronico guardado = comprobanteRepository.save(nota);
        auditService.registrar("GENERAR_NOTA_CREDITO_SRI", MODULO, ENTIDAD, guardado.getId(),
                null, guardado.datosAuditoria());
        return guardado;
    }

    /** Reserva el siguiente número de nota de crédito con bloqueo de la configuración. */
    private int reservarSecuencialNotaCredito(ConfiguracionFacturacion config) {
        int secuencial = config.getSecuencialNotaCredito() + 1;
        if (secuencial > SECUENCIAL_MAXIMO) {
            throw new BusinessException("SECUENCIAL_AGOTADO",
                    "Se agotó el secuencial de notas de crédito del punto de emisión "
                            + config.getEstablecimiento() + "-" + config.getPuntoEmision());
        }
        config.setSecuencialNotaCredito(secuencial);
        configuracionRepository.save(config);
        return secuencial;
    }

    /** Sólo las facturas autorizadas y vigentes pueden recibir notas de crédito. */
    private void exigirNotaCreditoSobre(ComprobanteElectronico original) {
        if (original.getTipoComprobante() != TipoComprobanteSri.FACTURA) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TIPO_COMPROBANTE_INVALIDO",
                    "Sólo las facturas pueden recibir notas de crédito");
        }
        if (original.getEstadoTributario() == EstadoTributario.ANULADO) {
            throw new BusinessException(HttpStatus.CONFLICT, "COMPROBANTE_ANULADO",
                    "La factura ya fue anulada y no puede recibir otra nota de crédito");
        }
        if (original.isSimulado() || original.getEstadoTributario() != EstadoTributario.AUTORIZADO) {
            throw new BusinessException(HttpStatus.CONFLICT, "COMPROBANTE_NO_AUTORIZADO",
                    "Las notas de crédito sólo se emiten sobre facturas autorizadas por el SRI");
        }
        if (vacio(original.getXmlAutorizado())) {
            throw new BusinessException("COMPROBANTE_SIN_XML",
                    "La factura original no tiene XML: no se puede construir la nota de crédito");
        }
    }

    /**
     * Nunca se devuelve más de lo facturado: lo ya devuelto con notas
     * vigentes más la nota nueva no puede superar el total de la factura.
     */
    private void exigirDentroDelTotal(ComprobanteElectronico original, BigDecimal totalNueva) {
        if (original.getTotal() == null) {
            return;
        }
        BigDecimal yaDevuelto = comprobanteRepository
                .findByComprobanteReferenciaIdOrderByCreatedAtDesc(original.getId()).stream()
                .filter(nc -> nc.getEstadoTributario() == EstadoTributario.PENDIENTE
                        || nc.getEstadoTributario() == EstadoTributario.AUTORIZADO)
                .map(nc -> nc.getTotal() == null ? BigDecimal.ZERO : nc.getTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (yaDevuelto.add(totalNueva).compareTo(original.getTotal()) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "NOTA_CREDITO_EXCEDE",
                    "La factura ya tiene " + yaDevuelto.setScale(2, RoundingMode.HALF_UP)
                            + " devueltos de un total de "
                            + original.getTotal().setScale(2, RoundingMode.HALF_UP)
                            + ": la nota pedida no cabe en ella");
        }
    }

    private List<NotaCreditoBuilder.Devolucion> devoluciones(EmitirNotaCreditoRequest request) {
        if (request.lineas() == null || request.lineas().isEmpty()) {
            return List.of();
        }
        return request.lineas().stream()
                .map(l -> new NotaCreditoBuilder.Devolucion(l.linea(), l.cantidad()))
                .toList();
    }

    private static String xmlDe(ComprobanteElectronico comprobante) {
        String xml = !comprobante.isSimulado() && comprobante.getXmlAutorizado() != null
                && !comprobante.getXmlAutorizado().isBlank()
                ? comprobante.getXmlAutorizado()
                : comprobante.getXmlFirmado();
        return xml == null ? "" : xml;
    }

    /**
     * Registra el trámite de anulación y lo aplica sobre el comprobante
     * (plan §3.10). Todo dentro de la misma transacción: o queda el
     * trámite y el estado, o no queda nada.
     */
    private ComprobanteElectronico aplicarAnulacion(Long id, String motivo) {
        ComprobanteElectronico comprobante = comprobanteRepository.buscarParaModificar(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "COMPROBANTE_NO_ENCONTRADO",
                        "El comprobante indicado no existe"));
        exigirAnulable(comprobante);
        exigirSinAnulacionVigente(id);
        throw new BusinessException(HttpStatus.NOT_IMPLEMENTED, "SRI_ANULACION_NO_DISPONIBLE",
            "La anulación no está integrada con el SRI; no se modificó el comprobante ni se creó un trámite");
    }

    /** Sólo facturas y notas de crédito autorizadas y todavía vigentes. */
    private void exigirAnulable(ComprobanteElectronico comprobante) {
        if (comprobante.getTipoComprobante() != TipoComprobanteSri.FACTURA
                && comprobante.getTipoComprobante() != TipoComprobanteSri.NOTA_CREDITO) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TIPO_COMPROBANTE_INVALIDO",
                    "Sólo se pueden anular facturas y notas de crédito");
        }
        if (comprobante.getEstadoTributario() == EstadoTributario.ANULADO) {
            throw new BusinessException(HttpStatus.CONFLICT, "COMPROBANTE_YA_ANULADO",
                    "El comprobante ya está anulado: revise el trámite en la bandeja");
        }
        if (comprobante.isSimulado() || comprobante.getEstadoTributario() != EstadoTributario.AUTORIZADO) {
            throw new BusinessException(HttpStatus.CONFLICT, "COMPROBANTE_NO_AUTORIZADO",
                    "Sólo se anulan comprobantes autorizados por el SRI");
        }
    }

    /** Un comprobante sólo admite un trámite de anulación vigente. */
    private void exigirSinAnulacionVigente(Long comprobanteId) {
        boolean vigente = anulacionRepository
                .findFirstByComprobanteIdAndEstadoInOrderBySolicitadoEnDesc(comprobanteId,
                        List.of(AnulacionComprobante.ESTADO_PENDIENTE, AnulacionComprobante.ESTADO_APLICADA))
                .isPresent();
        if (vigente) {
            throw new BusinessException(HttpStatus.CONFLICT, "ANULACION_YA_SOLICITADA",
                    "El comprobante ya tiene un trámite de anulación");
        }
    }

    // ------------------------------------------------------------------
    // Validaciones previas
    // ------------------------------------------------------------------

    private void exigirSinComprobantePrevio(Long cuentaId) {
        boolean yaFacturada = comprobanteRepository.findByCuentaIdOrderByCreatedAtDesc(cuentaId)
                .stream()
                // Un comprobante ANULADO no cuenta: la cuenta se puede volver a facturar.
                .anyMatch(c -> !reintentable(c) && c.getEstadoTributario() != EstadoTributario.ANULADO);
        if (yaFacturada) {
            throw new BusinessException(HttpStatus.CONFLICT, "CUENTA_YA_FACTURADA",
                    "La cuenta ya tiene un comprobante emitido: revíselo en la bandeja de comprobantes");
        }
    }

    /** Un comprobante sólo se vuelve a emitir si el SRI lo rechazó o nunca lo recibió. */
    private static boolean reintentable(ComprobanteElectronico c) {
        if (c.getEstadoTributario() == EstadoTributario.DEVUELTO
                || c.getEstadoTributario() == EstadoTributario.NO_AUTORIZADO) {
            return true;
        }
        return c.getEstadoGeneracion() == EstadoGeneracion.ERROR
                && c.getEstadoTributario() == EstadoTributario.PENDIENTE;
    }

    private void exigirDatosFiscales(Consultorio consultorio) {
        if (vacio(consultorio.getRazonSocial())) {
            throw new BusinessException("FACTURACION_INCOMPLETA",
                    "Falta la razón social del consultorio (Configuración → Consultorio)");
        }
        if (vacio(consultorio.getDireccion())) {
            throw new BusinessException("FACTURACION_INCOMPLETA",
                    "Falta la dirección del consultorio (Configuración → Consultorio)");
        }
        String ruc = consultorio.getRuc() == null ? "" : consultorio.getRuc().trim();
        if (!ruc.matches(PATRON_RUC)) {
            throw new BusinessException("FACTURACION_INCOMPLETA",
                    "El RUC del consultorio debe tener 13 dígitos y terminar en 001");
        }
    }

    private void exigirFirmaDisponible(ConfiguracionFacturacion config, Consultorio consultorio) {
        if (config.getAmbiente() == AmbienteSri.PRODUCCION
                && !ElectronicSignatureService.firmaReal(consultorio, config.getFirmaPassword())) {
            throw new BusinessException("SRI_FIRMA_REAL_REQUERIDA",
                    "En ambiente de PRODUCCIÓN sólo se puede emitir con el certificado .p12 "
                            + "y su contraseña: suba el certificado en Configuración → Consultorio");
        }
    }

    // ------------------------------------------------------------------
    // Datos del comprobante
    // ------------------------------------------------------------------

    private DatosFactura.Emisor emisor(Consultorio consultorio, ConfiguracionFacturacion config,
                                       int secuencial) {
        return emisor(consultorio, config, secuencial, ClaveAccesoService.COD_DOC_FACTURA);
    }

    private DatosFactura.Emisor emisor(Consultorio consultorio, ConfiguracionFacturacion config,
                                       int secuencial, String codDoc) {
        String direccion = consultorio.getDireccion().trim();
        return new DatosFactura.Emisor(
                consultorio.getRazonSocial().trim(),
                texto(consultorio.getNombre()),
                consultorio.getRuc().trim(),
                direccion,
                config.getAmbiente(),
                config.getTipoEmision(),
                codDoc,
                config.getEstablecimiento(),
                config.getPuntoEmision(),
                secuencial,
                direccion,
                config.isObligadoContabilidad(),
                // El código de agente de retención todavía no es configurable:
                // se emite sólo cuando exista (igual que el contribuyente especial).
                null);
    }

    private DatosFactura.Comprador comprador(Paciente paciente) {
        String identificacion = texto(paciente.getCedula());
        String tipo;
        String valor;
        if (identificacion.isBlank()) {
            tipo = "07"; // venta al público
            valor = IDENTIFICACION_PUBLICO;
        } else if (identificacion.matches("[0-9]{10}")) {
            tipo = "05"; // cédula
            valor = identificacion;
        } else if (identificacion.matches("[0-9]{13}")) {
            tipo = "04"; // RUC
            valor = identificacion;
        } else {
            tipo = "06"; // pasaporte u otro documento
            valor = identificacion.length() > 20 ? identificacion.substring(0, 20) : identificacion;
        }
        String razon = (texto(paciente.getNombres()) + " " + texto(paciente.getApellidos())).trim();
        String direccion = texto(paciente.getDireccion());
        return new DatosFactura.Comprador(tipo, razon.isEmpty() ? "Paciente" : razon, valor,
                direccion.isBlank() ? null : direccion);
    }

    private List<DatosFactura.CampoAdicional> adicionales(Paciente paciente) {
        List<DatosFactura.CampoAdicional> campos = new ArrayList<>();
        String correo = texto(paciente.getEmail());
        if (!correo.isBlank()) {
            campos.add(new DatosFactura.CampoAdicional("Correo", correo));
        }
        return campos;
    }

    private List<DatosFactura.Pago> pagos(EmitirFacturaRequest request) {
        if (request.formasPago() == null || request.formasPago().isEmpty()) {
            return null;
        }
        return request.formasPago().stream()
                .map(p -> new DatosFactura.Pago(p.codigo(), p.total(), p.plazo(), p.unidadTiempo()))
                .toList();
    }

    private String numeroComprobante(ConfiguracionFacturacion config, int secuencial) {
        return String.format("%s-%s-%09d", config.getEstablecimiento(),
                config.getPuntoEmision(), secuencial);
    }

    // ------------------------------------------------------------------
    // Errores de envío/autorización
    // ------------------------------------------------------------------

    private void registrarError(ComprobanteElectronico comprobante, RuntimeException e) {
        comprobante.setEstadoGeneracion(EstadoGeneracion.ERROR);
        comprobante.setErrorMensaje(mensajeDe(e));
        comprobante.setUpdatedBy(currentUserService.idUsuarioActual().orElse(null));
        ComprobanteElectronico guardado = comprobanteRepository.save(comprobante);
        auditService.registrar("ERROR_COMPROBANTE_SRI", MODULO, ENTIDAD, guardado.getId(),
                null, guardado.datosAuditoria());
    }

    private String mensajeDe(RuntimeException e) {
        String mensaje = e.getMessage();
        if (mensaje == null || mensaje.isBlank()) {
            mensaje = e.getClass().getSimpleName();
        }
        return mensaje.length() > 2000 ? mensaje.substring(0, 2000) : mensaje;
    }

    private BigDecimal nz(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor.setScale(2, RoundingMode.HALF_UP);
    }

    private int cantidad(Integer cantidad) {
        return cantidad == null || cantidad < 1 ? 1 : cantidad;
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
