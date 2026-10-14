package org.dentalcrm.web.agente.flujo;

import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.whatsapp.Conversacion;
import org.dentalcrm.web.agente.contexto.ConversacionContexto;
import org.dentalcrm.web.agente.contexto.MenuActual;
import org.dentalcrm.web.agente.contexto.OpcionMenu;
import org.dentalcrm.web.agente.contexto.TipoOpcion;
import org.dentalcrm.web.agente.engine.EntradaFlujo;
import org.dentalcrm.web.agente.engine.PasoResultado;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Flujo de agendamiento (§9 y §10): servicio → odontólogo → fecha → franja →
 * cupo → identificación → confirmación → creación con revalidación (§19).
 */
public class ScheduleAppointmentFlow extends AgendaPasos {

    public static final String CODIGO = "AGENDAR_CITA";
    public static final String PASO_RESERVAR = "RESERVAR";
    public static final String PASO_CONFIRMAR = "CONFIRMAR_RESERVA";

    public ScheduleAppointmentFlow(AgenteEntorno e) {
        super(e);
    }

    @Override
    public String codigo() {
        return CODIGO;
    }

    @Override
    public PasoResultado responder(Conversacion conversacion, ConversacionContexto ctx, EntradaFlujo entrada) {
        PasoResultado correccion = aplicarCorreccion(ctx, entrada);
        if (correccion != null) {
            return correccion;
        }
        if (ctx.getPaso() == null) {
            ctx.setFlujo(CODIGO);
            precargarEntidades(ctx, entrada);
            // §7: reservar exige identificar antes de preguntar el servicio.
            PasoResultado identificacion = pedirIdentificacion(conversacion, ctx, null);
            if (identificacion != null) {
                return identificacion;
            }
            PasoResultado pendiente = siguientePendiente(ctx);
            if (pendiente != null) {
                return pendiente;
            }
            return flujoReserva(conversacion, ctx, entrada);
        }
        String paso = ctx.getPaso();
        if (PASO_SERVICIO.equals(paso) || "ESCRIBIR_SERVICIO".equals(paso)) {
            PasoResultado r = servicioElegido(ctx, entrada.opcion(), entrada);
            return r != null ? r : repetir(ctx, "Elige un servicio de la lista.");
        }
        if (PASO_MODALIDAD.equals(paso)) {
            return modalidadElegida(ctx, entrada.opcion(), entrada.crudo());
        }
        if (PASO_ODONTOLOGO.equals(paso)) {
            return odontologoElegido(ctx, entrada.opcion(), entrada.crudo());
        }
        if (PASO_FECHA.equals(paso)) {
            return fechaElegida(ctx, entrada);
        }
        if (PASO_FRANJA.equals(paso)) {
            return franjaElegida(ctx, entrada);
        }
        if (PASO_SLOTS.equals(paso)) {
            return horaElegida(conversacion, ctx, entrada);
        }
        if (PASO_SIN_CUPOS.equals(paso)) {
            PasoResultado accion = accionSinCupos(ctx, entrada.opcion());
            return accion != null ? accion : repetir(ctx, "Elige una de las opciones disponibles.");
        }
        if (PASO_FALLA_CALENDARIO.equals(paso)) {
            PasoResultado accion = accionFallaCalendario(ctx, entrada.opcion());
            return accion != null ? accion : repetir(ctx, "Elige una de las opciones disponibles.");
        }
        if (PASO_RESERVAR.equals(paso)) {
            return flujoReserva(conversacion, ctx, entrada);
        }
        if (PASO_CONFIRMAR.equals(paso)) {
            return confirmarReserva(conversacion, ctx, entrada);
        }
        ctx.setFlujo(CODIGO);
        return siguientePendiente(ctx);
    }

    // ------------------------------------------------------------------
    // Reserva: cupo elegido → identificación → confirmación (§9)
    // ------------------------------------------------------------------

    @Override
    protected PasoResultado trasCupo(Conversacion conv, ConversacionContexto ctx, EntradaFlujo entrada) {
        return flujoReserva(conv, ctx, entrada);
    }

    private PasoResultado flujoReserva(Conversacion conv, ConversacionContexto ctx, EntradaFlujo entrada) {
        if (ctx.getSlotId() == null) {
            return mostrarSlots(ctx, "Elige primero un cupo de la lista.");
        }
        String resumen = "¡Excelente! Las " + ctx.getSlotId() + " están disponibles.";
        PasoResultado identificacion = pedirIdentificacion(conv, ctx, resumen);
        if (identificacion != null) {
            return identificacion;
        }
        return confirmarReserva(conv, ctx, entrada);
    }

    private PasoResultado confirmarReserva(Conversacion conv, ConversacionContexto ctx, EntradaFlujo entrada) {
        OpcionMenu opcion = entrada.opcion();
        if (opcion != null && opcion.tipo() == TipoOpcion.ACCION) {
            switch (opcion.valor()) {
                case "CAMBIAR_HORA" -> {
                    ctx.setSlotId(null);
                    return mostrarSlots(ctx, null);
                }
                case "CAMBIAR_FECHA" -> {
                    ctx.setSlotId(null);
                    return pedirFecha(ctx, null);
                }
                case "CAMBIAR_ODONTOLOGO" -> {
                    ctx.setSlotId(null);
                    ctx.setOdontologoResuelto(false);
                    return pedirOdontologo(ctx, null);
                }
                case "CANCELAR_PROCESO" -> {
                    return fin(ctx, "No reservé ninguna cita. ¿Puedo ayudarte con algo más?\n"
                            + soporte().menuTexto());
                }
                default -> {
                }
            }
        }
        if (entrada.esRechazo()) {
            return fin(ctx, "No reservé ninguna cita. ¿Puedo ayudarte con algo más?\n"
                    + soporte().menuTexto());
        }
        boolean confirmar = entrada.esConfirmacion()
                || (opcion != null && "CONFIRMAR".equals(opcion.valor()));
        if (!confirmar) {
            if (ctx.getUltimaPregunta() != null && ctx.getUltimaPregunta().contains("Confirma")) {
                return repetir(ctx, "Responde 1 para confirmar o 4 para cancelar el proceso.");
            }
            return mostrarConfirmacion(ctx);
        }
        return crearCita(conv, ctx);
    }

    private PasoResultado mostrarConfirmacion(ConversacionContexto ctx) {
        List<OpcionMenu> opciones = List.of(
                OpcionMenu.accion(1, "CONFIRMAR", "Confirmar"),
                OpcionMenu.accion(2, "CAMBIAR_HORA", "Cambiar horario"),
                OpcionMenu.accion(3, "CAMBIAR_FECHA", "Cambiar día"),
                OpcionMenu.accion(4, "CANCELAR_PROCESO", "Cancelar proceso"));
        String texto = "Confirma:\n"
                + "🦷 " + nombreServicio(ctx) + "\n"
                + "👨‍⚕️ " + (ctx.getOdontologoId() == null
                ? (ctx.getOdontologoLibre() == null ? "Cualquier odontólogo" : ctx.getOdontologoLibre())
                : e.odontologos.nombre(ctx.getOdontologoId())) + "\n"
                + "📅 " + soporte().formatearFecha(ctx.getFecha()) + "\n"
                + "🕣 " + ctx.getSlotId() + "\n\n"
                + "1. Confirmar\n2. Cambiar horario\n3. Cambiar día\n4. Cancelar proceso";
        return pregunta(ctx, texto, PASO_CONFIRMAR, MenuActual.CONFIRMACION, opciones, "CONFIRMACION");
    }

    private PasoResultado crearCita(Conversacion conv, ConversacionContexto ctx) {
        Paciente paciente = pacienteActual(ctx);
        if (paciente == null) {
            ctx.setPacienteId(null);
            return pedirIdentificacion(conv, ctx, "Necesito identificar al paciente para reservar.");
        }
        LocalDate fecha = ctx.getFecha();
        LocalTime hora = LocalTime.parse(ctx.getSlotId());
        var resultado = e.citas.crear(paciente, ctx.getOdontologoId(), ctx.getServicioId(), fecha, hora);

        if (resultado.duplicada()) {
            return fin(ctx, "Ya tenías esa cita agendada ✔\n🦷 " + nombreServicio(ctx)
                    + "\n📅 " + soporte().formatearFecha(fecha) + " a las " + hora
                    + "\n\n¿Puedo ayudarte con algo más?\n" + soporte().menuTexto());
        }
        if (resultado.alternativas() != null && !resultado.alternativas().isEmpty()) {
            // §19: el slot mostrado no estaba reservado; se ofrecen alternativas reales.
            ctx.setSlotId(null);
            return mostrarSlots(ctx, "Ese horario acaba de ocuparse 😕.");
        }
        if (resultado.error() != null) {
            return fin(ctx, "No pude agendar tu cita: " + resultado.error()
                    + "\nPuedes intentarlo de nuevo o escribir 'humano'.");
        }
        var cita = resultado.cita();
        return fin(ctx, "¡Listo! 🎉 Tu cita quedó agendada:\n"
                + "🦷 " + cita.servicioNombre() + "\n"
                + "👨‍⚕️ " + cita.doctorNombre() + "\n"
                + "📅 " + soporte().formatearFecha(cita.fecha()) + " a las " + cita.horaInicio() + "\n"
                + "Te esperamos 😊 Si no puedes asistir, avísanos por este chat.");
    }
}
