package org.dentalcrm.web.servicio;

import org.dentalcrm.domain.servicio.Servicio;
import org.dentalcrm.domain.servicio.ServicioRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.web.servicio.dto.ServicioRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServicioServiceTest {

    private ServicioService servicio;
    private ServicioRepository repo;

    @BeforeEach
    void setUp() {
        repo = mock(ServicioRepository.class);
        servicio = new ServicioService(repo, mock(AuditService.class));
    }

    @Test
    void rechazaNombreDuplicado() {
        when(repo.findByNombreIgnoreCase("Limpieza")).thenReturn(Optional.of(new Servicio()));
        assertThrows(BusinessException.class, () -> servicio.crear(request("Limpieza")));
    }

    @Test
    void guardaServicioValido() {
        when(repo.save(any(Servicio.class))).thenAnswer(inv -> inv.getArgument(0));
        servicio.crear(request("Blanqueamiento"));
    }

    @Test
    void rechazaServicioInexistente() {
        when(repo.findById(7L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> servicio.obtener(7L));
    }

    private static ServicioRequest request(String nombre) {
        return new ServicioRequest(nombre, "Descripción", 45, new BigDecimal("80.00"), null);
    }
}