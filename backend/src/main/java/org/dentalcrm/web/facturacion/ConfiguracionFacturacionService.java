package org.dentalcrm.web.facturacion;

import org.dentalcrm.domain.configuracion.ConfiguracionFacturacion;
import org.dentalcrm.domain.configuracion.ConfiguracionFacturacionRepository;
import org.dentalcrm.domain.configuracion.Consultorio;
import org.dentalcrm.domain.configuracion.ConsultorioRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.multitenant.TenantContext;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.CurrentUserService;
import org.dentalcrm.service.IntegracionSecretCipher;
import org.dentalcrm.web.facturacion.dto.ConfiguracionFacturacionRequest;
import org.dentalcrm.web.facturacion.dto.ConfiguracionFacturacionResponse;
import org.dentalcrm.web.facturacion.dto.FirmaPasswordRequest;
import org.dentalcrm.web.facturacion.sri.ElectronicSignatureService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

/**
 * Configuración tributaria y credenciales del SRI — Fase G (plan §3.5/§3.16).
 *
 * <p>Se sirve del certificado .p12 y de los datos fiscales que ya viven en
 * {@link Consultorio} (V21/V24): aquí sólo se guardan el ambiente, la
 * secuencialización y los secretos. Mientras falte el .p12 o su contraseña
 * la firma queda en modo {@code SIMULADA}, de modo que el flujo completo
 * puede probarse antes de tener el certificado de la clínica.
 */
@Service
public class ConfiguracionFacturacionService {

    private static final String MODULO = "FACTURACION";
    private static final String ENTIDAD = "CONFIGURACION_FACTURACION";
    private static final String PREFIJO_CIFRADO = "enc:v1:";

    private final ConfiguracionFacturacionRepository repository;
    private final ConsultorioRepository consultorioRepository;
    private final AuditService auditService;
    private final IntegracionSecretCipher cipher;
    private final CurrentUserService currentUserService;
    private final ElectronicSignatureService firmaService;

    public ConfiguracionFacturacionService(ConfiguracionFacturacionRepository repository,
                                           ConsultorioRepository consultorioRepository,
                                           AuditService auditService,
                                           IntegracionSecretCipher cipher,
                                           CurrentUserService currentUserService,
                                           ElectronicSignatureService firmaService) {
        this.repository = repository;
        this.consultorioRepository = consultorioRepository;
        this.auditService = auditService;
        this.cipher = cipher;
        this.currentUserService = currentUserService;
        this.firmaService = firmaService;
    }

    @Transactional
    public ConfiguracionFacturacionResponse obtener() {
        return respuesta(obtenerOInicializar());
    }

    @Transactional
    public ConfiguracionFacturacionResponse actualizar(ConfiguracionFacturacionRequest request) {
        validarUrl(request.sriUrlPruebas(), "La URL de pruebas del SRI");
        validarUrl(request.sriUrlProduccion(), "La URL de producción del SRI");

        ConfiguracionFacturacion config = obtenerOInicializar();
        config.setAmbiente(request.ambiente());
        config.setTipoEmision(request.tipoEmision());
        config.setContribuyenteEspecial(request.contribuyenteEspecial());
        config.setAgenteRetencion(request.agenteRetencion());
        config.setObligadoContabilidad(request.obligadoContabilidad());
        config.setEstablecimiento(request.establecimiento().trim());
        config.setPuntoEmision(request.puntoEmision().trim());
        config.setSecuencialActual(request.secuencialActual());
        config.setSriUrlPruebas(request.sriUrlPruebas().trim());
        config.setSriUrlProduccion(request.sriUrlProduccion().trim());
        config.setSriUsuario(limpiar(request.sriUsuario()));
        if (request.limpiarSriClave()) {
            config.setSriClave(null);
        } else if (noVacio(request.sriClave())) {
            config.setSriClave(cifrar(request.sriClave()));
        }
        guardar(config, "ACTUALIZAR_CONFIGURACION_SRI");
        return respuesta(config);
    }

    @Transactional
    public ConfiguracionFacturacionResponse definirPasswordFirma(FirmaPasswordRequest request) {
        ConfiguracionFacturacion config = obtenerOInicializar();
        Consultorio consultorio = consultorioRepository.findFirstByOrderByIdAsc()
            .orElseThrow(() -> new BusinessException("FIRMA_CERTIFICADO_NO_CARGADO",
                "Carga primero el archivo .p12 del emisor"));
        firmaService.validarCertificado(consultorio, request.password());
        config.setFirmaPassword(cifrar(request.password()));
        guardar(config, "DEFINIR_PASSWORD_FIRMA");
        return respuesta(config);
    }

    @Transactional
    public ConfiguracionFacturacionResponse limpiarPasswordFirma() {
        ConfiguracionFacturacion config = obtenerOInicializar();
        config.setFirmaPassword(null);
        guardar(config, "LIMPIAR_PASSWORD_FIRMA");
        return respuesta(config);
    }

    private ConfiguracionFacturacion obtenerOInicializar() {
        Long tenantId = TenantContext.actualOrDefault();
        return repository.findByTenantId(tenantId).orElseGet(() -> {
            ConfiguracionFacturacion config = new ConfiguracionFacturacion();
            config.setTenantId(tenantId);
            return repository.save(config);
        });
    }

    private void guardar(ConfiguracionFacturacion config, String accion) {
        config.setUpdatedBy(currentUserService.idUsuarioActual().orElse(null));
        repository.save(config);
        auditService.registrar(accion, MODULO, ENTIDAD, config.getId());
    }

    private ConfiguracionFacturacionResponse respuesta(ConfiguracionFacturacion config) {
        Consultorio consultorio = consultorioRepository.findFirstByOrderByIdAsc().orElse(null);
        boolean tieneCertificado = consultorio != null && consultorio.getFirmaDigital() != null;
        boolean tienePassword = config.getFirmaPassword() != null && !config.getFirmaPassword().isBlank();
        boolean firmaReal = ElectronicSignatureService.firmaReal(consultorio, config.getFirmaPassword());
        String motivo = firmaReal ? null : (!tieneCertificado
                ? "Falta el certificado .p12: súbelo en Configuración → Consultorio"
                : "Falta la contraseña del certificado .p12");
        String firmaTitular = null;
        java.time.Instant firmaValidoDesde = null;
        java.time.Instant firmaValidoHasta = null;
        if (firmaReal) {
            try {
                String password = descifrar(config.getFirmaPassword());
                ElectronicSignatureService.CertificadoInfo info = firmaService.validarCertificado(consultorio, password);
                firmaTitular = info.titular();
                firmaValidoDesde = info.validoDesde();
                firmaValidoHasta = info.validoHasta();
            } catch (BusinessException e) {
                firmaReal = false;
                motivo = e.getMessage();
            }
        }

        String siguiente = String.format("%s-%s-%09d",
                config.getEstablecimiento(), config.getPuntoEmision(), config.getSecuencialActual() + 1);

        return new ConfiguracionFacturacionResponse(
                config.getAmbiente().name(),
                config.getTipoEmision().name(),
                config.isContribuyenteEspecial(),
                config.isAgenteRetencion(),
                config.isObligadoContabilidad(),
                config.getEstablecimiento(),
                config.getPuntoEmision(),
                config.getSecuencialActual(),
                siguiente,
                config.getSriUrlPruebas(),
                config.getSriUrlProduccion(),
                config.urlAmbiente(),
                config.getSriUsuario(),
                config.getSriClave() != null && !config.getSriClave().isBlank(),
                firmaReal ? ElectronicSignatureService.MODO_CERTIFICADO : ElectronicSignatureService.MODO_SIMULADA,
                motivo,
                consultorio != null ? consultorio.getFirmaDigitalNombre() : null,
                tienePassword,
                consultorio != null ? consultorio.getRazonSocial() : null,
                consultorio != null ? consultorio.getRuc() : null,
                consultorio != null ? consultorio.getDireccion() : null,
                consultorio != null ? consultorio.getTelefono() : null,
                consultorio != null ? consultorio.getCorreoElectronico() : null,
                firmaTitular, firmaValidoDesde, firmaValidoHasta);
    }

    private void validarUrl(String valor, String etiqueta) {
        String url = limpiar(valor);
        if (url == null) {
            throw new BusinessException("URL_INVALIDA", etiqueta + " es obligatoria");
        }
        try {
            URI uri = new URI(url);
            String esquema = uri.getScheme();
            if (esquema == null || uri.getHost() == null
                    || !(esquema.equalsIgnoreCase("http") || esquema.equalsIgnoreCase("https"))) {
                throw new BusinessException("URL_INVALIDA", etiqueta + " debe ser una URL http(s)");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("URL_INVALIDA", etiqueta + " no es una URL válida");
        }
    }

    private String cifrar(String valor) {
        return PREFIJO_CIFRADO + cipher.encrypt(valor);
    }

    private String descifrar(String valor) {
        if (valor != null && valor.startsWith(PREFIJO_CIFRADO)) {
            return cipher.decrypt(valor.substring(PREFIJO_CIFRADO.length()));
        }
        return valor;
    }

    private boolean noVacio(String valor) {
        return valor != null && !valor.isBlank();
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
