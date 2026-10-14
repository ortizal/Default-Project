package org.dentalcrm.web.facturacion.sri;

import org.dentalcrm.domain.comprobante.ComprobanteElectronico;
import org.dentalcrm.domain.comprobante.ComprobanteElectronicoRepository;
import org.dentalcrm.domain.comprobante.EstadoGeneracion;
import org.dentalcrm.domain.comprobante.EstadoTributario;
import org.dentalcrm.domain.configuracion.AmbienteSri;
import org.dentalcrm.domain.configuracion.ConfiguracionFacturacion;
import org.dentalcrm.domain.configuracion.ConfiguracionFacturacionRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.multitenant.TenantContext;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/**
 * Envío y autorización de comprobantes ante el SRI — plan §3.6
 * {@code AuthorizationService} y estados §3.8.
 *
 * <p>Concentra aquí las transiciones de estado, de modo que el resto del
 * módulo sólo tenga que generar el XML y llamar a estos dos pasos:
 * <ol>
 *   <li>{@link #enviar} → recepción (RECIBIDA / DEVUELTA) →
 *       {@code ENVIADO} o {@code ERROR + DEVUELTO};</li>
 *   <li>{@link #autorizar} → consulta de autorización (AUT / NAT / PPR) →
 *       {@code AUTORIZADO}, {@code NO_AUTORIZADO} o {@code PENDIENTE}.</li>
 * </ol>
 *
 * <p><b>Modo simulado</b> (firma sin certificado .p12): el webservice del SRI
 * <b>nunca</b> se llama. Sólo se permite en ambiente de pruebas; el documento
 * queda local, con firma SIMULADA y estado tributario PENDIENTE. Nunca se
 * fabrican datos de recepción o autorización. En producción exige certificado.
 */
@Service
public class AuthorizationService {

    private static final String MODULO = "FACTURACION";
    private static final String ENTIDAD = "COMPROBANTE_ELECTRONICO";
    private static final String ESTADO_SIMULADO = "SIMULADA";
    private static final String RESPUESTA_ENVIO_SIMULADO =
            "Prueba interna: el comprobante no salió de este equipo ni fue recibido por el SRI.";
    private static final String RESPUESTA_AUTORIZACION_SIMULADA =
            "Prueba interna: no se consultó al SRI; la autorización tributaria sigue pendiente.";

    private final SriGateway gateway;
    private final ComprobanteElectronicoRepository repository;
    private final ConfiguracionFacturacionRepository configuracionRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    public AuthorizationService(SriGateway gateway,
                                ComprobanteElectronicoRepository repository,
                                ConfiguracionFacturacionRepository configuracionRepository,
                                AuditService auditService,
                                CurrentUserService currentUserService) {
        this.gateway = gateway;
        this.repository = repository;
        this.configuracionRepository = configuracionRepository;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
    }

    /** true si el comprobante se emitió sin certificado (no debe salir a la red). */
    public static boolean modoSimulado(ComprobanteElectronico comprobante) {
        return ElectronicSignatureService.MODO_SIMULADA.equals(comprobante.getModoFirma());
    }

    /**
     * Paso 1: envía el XML firmado al servicio de recepción del SRI
     * (o lo simula, ver la clase).
     */
    @Transactional
    public ComprobanteElectronico enviar(ComprobanteElectronico comprobante) {
        exigirXml(comprobante);

        if (modoSimulado(comprobante)) {
            exigirAmbientePruebas(comprobante);
            // §3.8: el XML ya fue firmado (BORRADOR → GENERADO → FIRMADO).
            // En modo simulado el documento nunca sale a la red, así que el
            // paso de envío no altera el estado de generación: se queda
            // FIRMADO, a la espera de una autorización real. Sólo se marca
            // que fue una prueba local.
            comprobante.setSimulado(true);
            comprobante.setSriEstado(ESTADO_SIMULADO);
            comprobante.setSriRespuesta(RESPUESTA_ENVIO_SIMULADO);
            comprobante.setErrorMensaje(null);
        } else {
            RespuestaRecepcion respuesta = gateway.recepcionar(base64(comprobante.getXmlFirmado()), entorno());
            comprobante.setSriEstado(respuesta.estado());
            comprobante.setSriRespuesta(respuesta.detalle().isBlank()
                    ? respuesta.estado() : respuesta.detalle());
            if (respuesta.recibida()) {
                comprobante.setEstadoGeneracion(EstadoGeneracion.ENVIADO);
                comprobante.setErrorMensaje(null);
            } else {
                // DEVUELTA u otro estado: el SRI no aceptó el comprobante
                comprobante.setEstadoGeneracion(EstadoGeneracion.ERROR);
                comprobante.setEstadoTributario(EstadoTributario.DEVUELTO);
                comprobante.setErrorMensaje(mensajeDe(respuesta.detalle(), respuesta.estado()));
            }
        }
        return guardar(comprobante, "ENVIAR_COMPROBANTE_SRI");
    }

    /**
     * Paso 2: consulta la autorización por clave de acceso y refleja el
     * resultado en los estados. También es lo que usa la bandeja (§3.7)
     * como acción "consultar SRI".
     */
    @Transactional
    public ComprobanteElectronico autorizar(ComprobanteElectronico comprobante) {
        if (modoSimulado(comprobante)) {
            exigirAmbientePruebas(comprobante);
            comprobante.setSimulado(true);
            comprobante.setSriEstado(ESTADO_SIMULADO);
            comprobante.setSriRespuesta(RESPUESTA_AUTORIZACION_SIMULADA);
            comprobante.setErrorMensaje(null);
            return guardar(comprobante, "CONSULTAR_AUTORIZACION_SIMULADA");
        }

        if (comprobante.getEstadoGeneracion() != EstadoGeneracion.ENVIADO) {
            throw new BusinessException("COMPROBANTE_NO_ENVIADO",
                    "El comprobante debe estar recibido por el SRI antes de consultar su autorización");
        }

        RespuestaAutorizacion respuesta = gateway.autorizar(comprobante.getClaveAcceso(), entorno());
        comprobante.setSriEstado(respuesta.estado());
        if (respuesta.autorizado()) {
            String inconsistencia = validarAutorizacion(comprobante, respuesta);
            if (inconsistencia != null) {
                comprobante.setEstadoGeneracion(EstadoGeneracion.ERROR);
                comprobante.setSriRespuesta(inconsistencia);
                comprobante.setErrorMensaje(inconsistencia);
                return guardar(comprobante, "ERROR_VERIFICAR_AUTORIZACION_SRI");
            }
            comprobante.setEstadoTributario(EstadoTributario.AUTORIZADO);
            comprobante.setAutorizacionNumero(respuesta.numeroAutorizacion());
            comprobante.setAutorizacionFecha(respuesta.fechaAutorizacion() != null
                    ? respuesta.fechaAutorizacion() : Instant.now());
            comprobante.setXmlAutorizado(respuesta.xmlAutorizado());
            comprobante.setSriRespuesta(respuesta.detalle().isBlank()
                    ? RespuestaAutorizacion.AUTORIZADO : respuesta.detalle());
            comprobante.setErrorMensaje(null);
            return guardar(comprobante, "AUTORIZAR_COMPROBANTE_SRI");
        }
        if (respuesta.noAutorizado()) {
            comprobante.setEstadoTributario(EstadoTributario.NO_AUTORIZADO);
            comprobante.setSriRespuesta(respuesta.detalle());
            comprobante.setErrorMensaje(mensajeDe(respuesta.detalle(),
                    "El SRI no autorizó el comprobante"));
            return guardar(comprobante, "NO_AUTORIZAR_COMPROBANTE_SRI");
        }
        // PPR: el SRI aún no procesó; se conserva PENDIENTE para reintentar
        comprobante.setSriRespuesta(respuesta.detalle().isBlank()
                ? "Sin respuesta del SRI todavía; vuelva a consultar."
                : respuesta.detalle());
        return guardar(comprobante, "CONSULTAR_COMPROBANTE_SRI");
    }

    private String validarAutorizacion(ComprobanteElectronico comprobante, RespuestaAutorizacion respuesta) {
        String ambienteEsperado = comprobante.getAmbiente().name();
        if (!Objects.equals(comprobante.getClaveAcceso(), respuesta.claveAccesoConsultada())) {
            return "La autorización recibida no corresponde a la clave de acceso consultada.";
        }
        if (!Objects.equals(comprobante.getClaveAcceso(), respuesta.numeroAutorizacion())) {
            return "El número de autorización no coincide con la clave de acceso del comprobante.";
        }
        if (respuesta.ambiente() == null || !ambienteEsperado.equalsIgnoreCase(respuesta.ambiente())) {
            return "El ambiente de la autorización no coincide con el ambiente del comprobante.";
        }
        if (respuesta.xmlAutorizado() == null || respuesta.xmlAutorizado().isBlank()) {
            return "El SRI informó autorización sin devolver el XML autorizado.";
        }
        try {
            if (!Objects.equals(comprobante.getClaveAcceso(),
                    RideXmlParser.parsear(respuesta.xmlAutorizado()).claveAcceso())) {
                return "El XML autorizado no corresponde a la clave de acceso del comprobante.";
            }
        } catch (RuntimeException e) {
            return "El SRI devolvió un XML autorizado que no se pudo validar.";
        }
        return null;
    }

    /** Una factura local no se puede consultar ni reenviar al SRI. */
    public static void exigirNoSimulado(ComprobanteElectronico comprobante) {
        if (modoSimulado(comprobante) || comprobante.isSimulado()) {
            throw new BusinessException(org.springframework.http.HttpStatus.CONFLICT,
                    "COMPROBANTE_SIMULADO_NO_REINTENTABLE",
                    "El comprobante es una prueba local y nunca fue enviado al SRI");
        }
    }

    private SriEntorno entorno() {
        ConfiguracionFacturacion config = configuracionRepository
                .findByTenantId(TenantContext.actualOrDefault())
                .orElseThrow(() -> new BusinessException("FACTURACION_NO_CONFIGURADA",
                        "Configure la facturación electrónica antes de emitir"));
        return new SriEntorno(config.urlAmbiente(), config.getAmbiente());
    }

    private void exigirXml(ComprobanteElectronico comprobante) {
        if (comprobante.getXmlFirmado() == null || comprobante.getXmlFirmado().isBlank()) {
            throw new BusinessException("COMPROBANTE_SIN_XML",
                    "El comprobante no tiene XML firmado para enviar");
        }
    }

    private void exigirAmbientePruebas(ComprobanteElectronico comprobante) {
        if (comprobante.getAmbiente() == AmbienteSri.PRODUCCION) {
            throw new BusinessException("SRI_FIRMA_REAL_REQUERIDA",
                    "En ambiente de PRODUCCIÓN sólo se puede emitir con el certificado .p12 "
                            + "y su contraseña: suba el certificado en Configuración → Consultorio");
        }
    }

    private ComprobanteElectronico guardar(ComprobanteElectronico comprobante, String accion) {
        comprobante.setUpdatedBy(currentUserService.idUsuarioActual().orElse(null));
        ComprobanteElectronico guardado = repository.save(comprobante);
        auditService.registrar(accion, MODULO, ENTIDAD, guardado.getId(),
                null, guardado.datosAuditoria());
        return guardado;
    }

    private String base64(String xml) {
        return Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8));
    }

    private String mensajeDe(String detalle, String porDefecto) {
        return detalle == null || detalle.isBlank() ? porDefecto : detalle;
    }
}
