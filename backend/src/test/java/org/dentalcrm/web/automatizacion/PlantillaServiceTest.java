package org.dentalcrm.web.automatizacion;

import org.dentalcrm.domain.automatizacion.PlantillaMensaje;
import org.dentalcrm.domain.automatizacion.PlantillaMensajeRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.web.automatizacion.dto.PlantillaRequest;
import org.dentalcrm.web.automatizacion.dto.PlantillaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlantillaServiceTest {

    @Mock
    private PlantillaMensajeRepository plantillaRepository;
    @Mock
    private AuditService auditService;

    private PlantillaService servicio;

    @BeforeEach
    void setUp() {
        servicio = new PlantillaService(plantillaRepository, auditService);
    }

    private PlantillaMensaje plantilla(Long id, String nombre) {
        PlantillaMensaje p = new PlantillaMensaje();
        p.setId(id);
        p.setNombre(nombre);
        p.setContenido("Hola {{nombre}}");
        p.setActiva(true);
        return p;
    }

    @Test
    void listarDevuelveOrdenadasPorNombre() {
        when(plantillaRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of(plantilla(1L, "b"), plantilla(2L, "a")));

        List<PlantillaResponse> r = servicio.listar();

        assertEquals(2, r.size());
        assertEquals("a", r.get(1).nombre());
        assertEquals("b", r.get(0).nombre());
    }

    @Test
    void crearGuardaPlantillaActivaPorDefecto() {
        when(plantillaRepository.existsByNombreIgnoreCase("cita_creada")).thenReturn(false);
        when(plantillaRepository.save(any(PlantillaMensaje.class))).thenAnswer(i -> {
            PlantillaMensaje p = i.getArgument(0);
            p.setId(10L);
            return p;
        });

        PlantillaResponse r = servicio.crear(new PlantillaRequest("cita_creada", "Hola {{nombre}}", null));

        assertEquals(10L, r.id());
        assertEquals("cita_creada", r.nombre());
        assertEquals(Boolean.TRUE, r.activa());
        verify(plantillaRepository).save(any(PlantillaMensaje.class));
        verify(auditService).registrar(eq("CREAR_PLANTILLA"), eq("AUTOMATIZACIONES"), eq("PLANTILLA"), eq(10L));
    }

    @Test
    void crearRechazaNombreDuplicado() {
        when(plantillaRepository.existsByNombreIgnoreCase("cita_creada")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> servicio.crear(new PlantillaRequest("cita_creada", "Hola", true)));

        assertEquals("PLANTILLA_YA_EXISTE", ex.getCode());
        verify(plantillaRepository, never()).save(any());
    }

    @Test
    void actualizarEditaYAudita() {
        PlantillaMensaje p = plantilla(5L, "viejo");
        when(plantillaRepository.findById(5L)).thenReturn(Optional.of(p));

        PlantillaResponse r = servicio.actualizar(5L, new PlantillaRequest("nuevo", "Otro contenido {{fecha}}", false));

        assertEquals("nuevo", r.nombre());
        assertEquals("Otro contenido {{fecha}}", r.contenido());
        assertEquals(Boolean.FALSE, r.activa());
        verify(auditService).registrar(eq("ACTUALIZAR_PLANTILLA"), eq("AUTOMATIZACIONES"), eq("PLANTILLA"), eq(5L));
    }

    @Test
    void actualizarLanzaSiNoExiste() {
        when(plantillaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> servicio.actualizar(99L, new PlantillaRequest("x", "y", true)));
    }

    @Test
    void eliminarBorraYAudita() {
        when(plantillaRepository.existsById(3L)).thenReturn(true);

        servicio.eliminar(3L);

        verify(plantillaRepository).deleteById(3L);
        verify(auditService).registrar(eq("ELIMINAR_PLANTILLA"), eq("AUTOMATIZACIONES"), eq("PLANTILLA"), eq(3L));
    }
}