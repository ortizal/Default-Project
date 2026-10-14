package org.dentalcrm.domain.configuracion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;

/**
 * Configuración tributaria de un tenant (Fase G, plan §3.5/§3.14).
 *
 * <p>Una sola fila por tenant con el ambiente SRI, la secuencialización
 * del establecimiento/punto de emisión y las credenciales de los
 * servicios web. Los datos fiscales (razón social, RUC, dirección) y el
 * certificado .p12 se leen de {@link Consultorio} para no duplicarlos.
 */
@Entity
@Table(name = "configuracion_facturacion")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionFacturacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, unique = true)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AmbienteSri ambiente = AmbienteSri.PRUEBAS;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_emision", nullable = false, length = 20)
    private TipoEmisionSri tipoEmision = TipoEmisionSri.NORMAL;

    @Column(name = "contribuyente_especial", nullable = false)
    private boolean contribuyenteEspecial;

    @Column(name = "agente_retencion", nullable = false)
    private boolean agenteRetencion;

    @Column(name = "obligado_contabilidad", nullable = false)
    private boolean obligadoContabilidad;

    @Column(nullable = false, length = 3)
    private String establecimiento = "001";

    @Column(name = "punto_emision", nullable = false, length = 3)
    private String puntoEmision = "001";

    @Column(name = "secuencial_actual", nullable = false)
    private int secuencialActual;

    /**
     * Serie propia de las notas de crédito (04): cada tipo de comprobante
     * secuenciaa por separado dentro del mismo establecimiento y punto de
     * emisión (ficha técnica del SRI, plan §3.9).
     */
    @Column(name = "secuencial_nota_credito", nullable = false)
    private int secuencialNotaCredito;

    @Column(name = "sri_url_pruebas", nullable = false, length = 500)
    private String sriUrlPruebas = "https://celpruebas.sri.gob.ec";

    @Column(name = "sri_url_produccion", nullable = false, length = 500)
    private String sriUrlProduccion = "https://cel.sri.gob.ec";

    @Column(name = "sri_usuario", length = 100)
    private String sriUsuario;

    @Column(name = "sri_clave", columnDefinition = "text")
    private String sriClave;

    /** Contraseña del certificado .p12, cifrada con {@code IntegracionSecretCipher}. */
    @Column(name = "firma_password", columnDefinition = "text")
    private String firmaPassword;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    @PreUpdate
    void actualizarFecha() {
        updatedAt = Instant.now();
    }

    /** URL de los servicios web según el ambiente configurado (plan §3.5). */
    public String urlAmbiente() {
        return ambiente == AmbienteSri.PRODUCCION ? sriUrlProduccion : sriUrlPruebas;
    }
}
