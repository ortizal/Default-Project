package org.dentalcrm.web.configuracion;

import org.dentalcrm.domain.configuracion.Consultorio;
import org.dentalcrm.domain.configuracion.ConsultorioRepository;
import org.dentalcrm.multitenant.TenantContext;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.web.configuracion.dto.ConsultorioRequest;
import org.dentalcrm.web.configuracion.dto.ConsultorioResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultorioService {

    private static final String MODULO = "CONFIGURACION";

    private final ConsultorioRepository repository;
    private final AuditService auditService;
    private final String nombreInicial;
    private final String direccionInicial;
    private final String telefonoInicial;

    public ConsultorioService(ConsultorioRepository repository,
                              AuditService auditService,
                              @Value("${app.clinica.nombre:Clínica Dental}") String nombreInicial,
                              @Value("${app.clinica.direccion:}") String direccionInicial,
                              @Value("${app.clinica.telefono:}") String telefonoInicial) {
        this.repository = repository;
        this.auditService = auditService;
        this.nombreInicial = nombreInicial;
        this.direccionInicial = direccionInicial;
        this.telefonoInicial = telefonoInicial;
    }

    @Transactional
    public ConsultorioResponse obtener() {
        return ConsultorioResponse.from(obtenerOInicializar());
    }

    @Transactional
    public ConsultorioResponse actualizar(ConsultorioRequest request) {
        Consultorio consultorio = obtenerOInicializar();
        consultorio.setNombre(request.nombre().trim());
        consultorio.setDireccion(limpiar(request.direccion()));
        consultorio.setTelefono(limpiar(request.telefono()));
        consultorio.setEnlaceUbicacion(limpiar(request.enlaceUbicacion()));
        consultorio.setHorarioAtencion(limpiar(request.horarioAtencion()));
        repository.save(consultorio);
        auditService.registrar("ACTUALIZAR_CONSULTORIO", MODULO, "CONSULTORIO", consultorio.getId());
        return ConsultorioResponse.from(consultorio);
    }

    private Consultorio obtenerOInicializar() {
        return repository.findFirstByOrderByIdAsc().orElseGet(() -> {
            Consultorio consultorio = new Consultorio();
            consultorio.setTenantId(TenantContext.actualOrDefault());
            consultorio.setNombre(nombreInicial);
            consultorio.setDireccion(direccionInicial);
            consultorio.setTelefono(telefonoInicial);
            return repository.save(consultorio);
        });
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}