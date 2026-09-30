package org.dentalcrm.domain.configuracion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;

@Entity
@Table(name = "configuracion_consultorio")
@Getter
@Setter
@NoArgsConstructor
public class Consultorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, unique = true)
    private Long tenantId;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "razon_social", length = 200)
    private String razonSocial;

    @Column(length = 13)
    private String ruc;

    @Column(name = "correo_electronico", length = 190)
    private String correoElectronico;

    @Column(length = 500)
    private String direccion;

    @Column(length = 30)
    private String telefono;

    @Column(name = "enlace_ubicacion", length = 1000)
    private String enlaceUbicacion;

    @Column(name = "horario_atencion", length = 500)
    private String horarioAtencion;

    @Column(name = "firma_digital", columnDefinition = "bytea")
    private byte[] firmaDigital;

    @Column(name = "firma_digital_nombre", length = 255)
    private String firmaDigitalNombre;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void actualizarFecha() {
        updatedAt = Instant.now();
    }
}