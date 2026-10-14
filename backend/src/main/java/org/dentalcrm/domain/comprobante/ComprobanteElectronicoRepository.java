package org.dentalcrm.domain.comprobante;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ComprobanteElectronicoRepository extends JpaRepository<ComprobanteElectronico, Long> {

    /** La clave de acceso es única en todo el SRI (V41). */
    Optional<ComprobanteElectronico> findByClaveAcceso(String claveAcceso);

    List<ComprobanteElectronico> findAllByOrderByCreatedAtDesc();

    List<ComprobanteElectronico> findByEstadoTributarioOrderByCreatedAtDesc(EstadoTributario estado);

    /** Comprobantes de una cuenta, del más reciente al más antiguo (§3.7). */
    List<ComprobanteElectronico> findByCuentaIdOrderByCreatedAtDesc(Long cuentaId);

    /** Documentos que modifican a una factura: notas de crédito (§3.9). */
    List<ComprobanteElectronico> findByComprobanteReferenciaIdOrderByCreatedAtDesc(Long referenciaId);

    /**
     * Carga con bloqueo de escritura para emitir una nota de crédito
     * sobre la factura (§3.9) o para anularla (§3.10): dos trámites
     * simultáneos nunca pasan ambas por las comprobaciones de vigencia.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ComprobanteElectronico c where c.id = :id")
    Optional<ComprobanteElectronico> buscarParaModificar(@Param("id") Long id);

    /**
     * Bandeja de comprobantes (§3.7). {@code pacientes} son los
     * identificadores que casaron con el texto libre (nombres, apellidos
     * o cédula); nunca debe llegar vacío. {@code desde} y {@code hasta}
     * se reciben siempre tipados (sentinela fuera de rango cuando el
     * filtro no se usa), porque un parámetro {@code LocalDate} anulado en
     * {@code (:desde IS NULL OR ...)} no puede tiparse en PostgreSQL.
     */
    @Query("""
            SELECT c FROM ComprobanteElectronico c
            WHERE c.fechaEmision >= :desde
              AND c.fechaEmision <= :hasta
              AND (:tipo IS NULL OR c.tipoComprobante = :tipo)
              AND (:estado IS NULL OR c.estadoTributario = :estado)
              AND (:generacion IS NULL OR c.estadoGeneracion = :generacion)
              AND (:pacienteId IS NULL OR c.pacienteId = :pacienteId)
              AND (:establecimiento = '' OR c.establecimiento = :establecimiento)
              AND (:usuarioId IS NULL OR c.createdBy = :usuarioId)
              AND (:q = '' OR lower(c.numeroComprobante) LIKE lower(concat('%', :q, '%'))
                          OR c.claveAcceso LIKE concat('%', :q, '%')
                          OR c.pacienteId IN :pacientes)
            ORDER BY c.createdAt DESC
            """)
    Page<ComprobanteElectronico> buscar(@Param("q") String q,
                                        @Param("pacientes") List<Long> pacientes,
                                        @Param("desde") LocalDate desde,
                                        @Param("hasta") LocalDate hasta,
                                        @Param("tipo") TipoComprobanteSri tipo,
                                        @Param("estado") EstadoTributario estado,
                                        @Param("generacion") EstadoGeneracion generacion,
                                        @Param("pacienteId") Long pacienteId,
                                        @Param("establecimiento") String establecimiento,
                                        @Param("usuarioId") Long usuarioId,
                                        Pageable pageable);
}
