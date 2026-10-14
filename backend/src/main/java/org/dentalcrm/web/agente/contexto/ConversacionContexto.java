package org.dentalcrm.web.agente.contexto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Estado de la conversación persistido en {@code conversaciones.contexto_agente}
 * (JSONB, §3 de las instrucciones).
 *
 * <p>Es la primera pieza del pipeline: el motor siempre pregunta "¿dónde está
 * el usuario?" leyendo este objeto antes de interpretar el texto.
 */
@Getter
@Setter
@NoArgsConstructor
public class ConversacionContexto {

    /** Código del flujo activo ({@code CONSULTAR_DISPONIBILIDAD}, ...); vacío si no hay. */
    private String flujo;

    /** Paso dentro del flujo ({@code SELECCIONAR_ODONTOLOGO}, ...). */
    private String paso;

    /** Tipo de entrada esperada ({@code DATE}, {@code TEXT}, {@code OPTION}...), §18. */
    private String expectedInput;

    private Long pacienteId;
    private Long odontologoId;
    private Long servicioId;
    private String modalidadReserva;
    private boolean modalidadResuelta;
    private LocalDate fecha;
    /** Franja elegida: MANANA, MEDIODIA, TARDE o TODAS. */
    private String franja;
    /** Hora elegida en formato {@code HH:mm}; es un deseo, no una reserva. */
    private String slotId;

    private MenuActual menuActual;
    private List<OpcionMenu> opciones = new ArrayList<>();

    /** Indica que el flujo necesita identificar al paciente (§7). */
    private boolean requiereIdentificacion;
    /** Última pregunta enviada, para repetirla si el usuario escribe ruido. */
    private String ultimaPregunta;
    private int intentosFallidos;

    private Instant updatedAt;
    private Instant expiresAt;

    // ---- identificación y registro (§7) ----
    private String cedula;
    private String nombres;
    private String apellidos;
    private LocalDate fechaNacimiento;
    private String tutorNombre;
    private String tutorApellidos;
    private String tutorCedula;
    private String parentesco;
    private String telefono;
    /** Paciente elegido de una lista de dependientes del tutor. */
    private Long pacienteSeleccionId;

    // ---- entidades libres (§10 y §12) ----
    /** Nombre mencionado por el usuario ("para Dulce"). */
    private String nombrePaciente;
    private String servicioLibre;
    private String odontologoLibre;
    /** El servicio/odontólogo ya quedó resuelto (aunque sea "no estoy seguro"/"cualquiera"). */
    private boolean servicioResuelto;
    private boolean odontologoResuelto;
    /** Resumen de la reserva mostrado antes de confirmar (§9). */
    private String resumenReserva;
    /** Cita en curso de gestión (confirmar/cancelar/reprogramar). */
    private Long citaId;
    /** El usuario dejó una pausa y hay que ofrecer continuar (§15). */
    private boolean reanudando;
    /** Identidad verificada por cédula y vigente, para no repetir la pregunta (§9). */
    private boolean identidadVerificada;
    private Instant identidadVerificadaEn;
    /** La consulta de disponibilidad pide horario general (no un día concreto). */
    private boolean consultaHorario;
    /** Paso y flujo a los que volver tras identificar o registrar (§7). */
    private String pasoRetorno;
    private String flujoRetorno;

    // ---- transferencia a recepción (§13 del plan de calendario) ----
    /** Flujo que estaba en curso antes de transferir, para que recepción sepa el punto. */
    private String flujoAnteriorTransferencia;
    /** Motivo escrito por el usuario al pedir una persona (si lo hubo). */
    private String motivoTransferencia;

    public boolean tieneFlujoActivo() {
        return flujo != null && !flujo.isBlank();
    }

    public boolean tieneOpciones() {
        return opciones != null && !opciones.isEmpty();
    }

    public void reiniciar() {
        flujo = null;
        paso = null;
        expectedInput = null;
        pacienteId = null;
        odontologoId = null;
        servicioId = null;
        modalidadReserva = null;
        modalidadResuelta = false;
        fecha = null;
        franja = null;
        slotId = null;
        menuActual = MenuActual.PRINCIPAL;
        opciones = new ArrayList<>();
        requiereIdentificacion = false;
        ultimaPregunta = null;
        intentosFallidos = 0;
        cedula = null;
        nombres = null;
        apellidos = null;
        fechaNacimiento = null;
        tutorNombre = null;
        tutorApellidos = null;
        tutorCedula = null;
        parentesco = null;
        nombrePaciente = null;
        servicioLibre = null;
        odontologoLibre = null;
        servicioResuelto = false;
        odontologoResuelto = false;
        resumenReserva = null;
        citaId = null;
        reanudando = false;
        identidadVerificada = false;
        identidadVerificadaEn = null;
        consultaHorario = false;
        pasoRetorno = null;
        flujoRetorno = null;
        flujoAnteriorTransferencia = null;
        motivoTransferencia = null;
    }

    /** Copia las entidades de otra reserva para que un cambio de fecha no pierda el resto (§12). */
    public void heredarReserva(ConversacionContexto otra) {
        if (otra == null) {
            return;
        }
        pacienteId = otra.pacienteId;
        odontologoId = otra.odontologoId;
        servicioId = otra.servicioId;
        fecha = otra.fecha;
        franja = otra.franja;
        slotId = otra.slotId;
        servicioLibre = otra.servicioLibre;
        odontologoLibre = otra.odontologoLibre;
        servicioResuelto = otra.servicioResuelto;
        odontologoResuelto = otra.odontologoResuelto;
        franja = otra.franja;
        nombrePaciente = otra.nombrePaciente;
        cedula = otra.cedula;
    }

}
