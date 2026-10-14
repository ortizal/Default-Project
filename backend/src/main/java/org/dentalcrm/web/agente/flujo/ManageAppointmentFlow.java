package org.dentalcrm.web.agente.flujo;

import org.dentalcrm.domain.whatsapp.Conversacion;
import org.dentalcrm.web.agente.contexto.ConversacionContexto;
import org.dentalcrm.web.agente.contexto.MenuActual;
import org.dentalcrm.web.agente.contexto.OpcionMenu;
import org.dentalcrm.web.agente.contexto.TipoOpcion;
import org.dentalcrm.web.agente.engine.EntradaFlujo;
import org.dentalcrm.web.agente.engine.PasoResultado;

import java.util.List;

/**
 * Confirma, cancela o reprograma la próxima cita (§5: opción 5 del menú).
 *
 * <p>Si el usuario escribe "confirmar" o "cancelar" de forma explícita se actúa
 * sobre la próxima cita, como hacía el bot antiguo; si llega un número, se
 * muestra el menú de gestión.
 */
public class ManageAppointmentFlow extends FlujoBase {

    public static final String CODIGO = "GESTIONAR_CITA";
    private static final String PASO_ELEGIR = "ELEGIR_CITA";
    private static final String PASO_ACCION = "ACCION_CITA";
    private static final String PASO_CANCELAR = "CONFIRMAR_CANCELACION";
    private static final String PASO_FECHA = "REPROGRAMAR_FECHA";
    private static final String PASO_HORA = "REPROGRAMAR_HORA";

    public ManageAppointmentFlow(AgenteEntorno e) {
        super(e);
    }

    @Override
    public String codigo() {
        return CODIGO;
    }

    @Override
    public PasoResultado responder(Conversacion conversacion, ConversacionContexto ctx, EntradaFlujo entrada) {
        if (ctx.getPaso() == null) {
            ctx.setFlujo(CODIGO);
            PasoResultado identificacion = pedirIdentificacion(conversacion, ctx, null);
            if (identificacion != null) {
                return identificacion;
            }
            PasoResultado directa = accionDirecta(ctx, entrada);
            if (directa != null) {
                return directa;
            }
            return elegirCita(ctx);
        }
        return switch (ctx.getPaso()) {
            case PASO_ELEGIR -> elegir(ctx, entrada);
            case PASO_ACCION -> accion(ctx, entrada);
            case PASO_CANCELAR -> cancelar(ctx, entrada);
            case PASO_FECHA -> pedirHoraReprogramacion(ctx, entrada);
            case PASO_HORA -> reprogramar(conversacion, ctx, entrada);
            default -> elegirCita(ctx);
        };
    }

    /** Atajo: "confirmar"/"cancelar" escritos a mano (§6). */
    private PasoResultado accionDirecta(ConversacionContexto ctx, EntradaFlujo entrada) {
        String t = entrada.texto();
        if (t.contains("cancelar") || t.contains("cancela") || t.contains("no asistire")) {
            var cita = e.citas.proxima(pacienteActual(ctx)).orElse(null);
            if (cita != null) {
                ctx.setCitaId(cita.getId());
                return cancelarConfirmado(ctx, cita);
            }
        }
        if (t.contains("confirmar") || t.contains("confirmo") || t.contains("confirma")) {
            var cita = e.citas.proxima(pacienteActual(ctx)).orElse(null);
            if (cita != null) {
                return confirmado(ctx, cita);
            }
        }
        if (t.contains("reprogramar") || t.contains("cambiar cita") || t.contains("mover cita")) {
            return pedirFechaReprogramar(ctx);
        }
        return null;
    }

    private PasoResultado elegir(ConversacionContexto ctx, EntradaFlujo entrada) {
        if (entrada.tieneOpcion() && entrada.opcion().tipo() == TipoOpcion.ACCION) {
            return accionValor(ctx, entrada.opcion());
        }
        List<org.dentalcrm.domain.cita.Cita> citas = e.citas.proximas(pacienteActual(ctx));
        Integer numero = entrada.opcion() != null ? entrada.opcion().numero() : entrada.numero();
        if (numero == null || numero < 1 || numero > citas.size()) {
            PasoResultado directa = accionDirecta(ctx, entrada);
            if (directa != null) {
                return directa;
            }
            return elegirCita(ctx);
        }
        ctx.setCitaId(citas.get(numero - 1).getId());
        return accionesDeLaCita(ctx, citas.get(numero - 1));
    }

    private PasoResultado accionesDeLaCita(ConversacionContexto ctx, org.dentalcrm.domain.cita.Cita cita) {
        List<OpcionMenu> opciones = List.of(
                OpcionMenu.accion(1, "CONFIRMAR", "Confirmar cita"),
                OpcionMenu.accion(2, "CANCELAR", "Cancelar cita"),
                OpcionMenu.accion(3, "REPROGRAMAR", "Reprogramar cita"),
                OpcionMenu.accion(4, "MENU", "Volver al menú"));
        String texto = "Cita seleccionada:\n"
                + "• " + cita.getServicio().getNombre() + " — "
                + soporte().formatearFecha(cita.getFecha()) + " a las " + cita.getHoraInicio() + "\n\n"
                + "1. Confirmar cita\n2. Cancelar cita\n3. Reprogramar cita\n4. Volver al menú";
        return pregunta(ctx, texto, PASO_ACCION, MenuActual.GESTION_CITA, opciones);
    }

    private PasoResultado accion(ConversacionContexto ctx, EntradaFlujo entrada) {
        OpcionMenu opcion = entrada.opcion();
        if (opcion != null && opcion.tipo() == TipoOpcion.ACCION) {
            return accionValor(ctx, opcion);
        }
        if (entrada.esConfirmacion()) {
            return confirmado(ctx, e.citas.porId(ctx.getCitaId()));
        }
        return accionesDeLaCita(ctx, e.citas.porId(ctx.getCitaId()));
    }

    private PasoResultado accionValor(ConversacionContexto ctx, OpcionMenu opcion) {
        return switch (opcion.valor()) {
            case "CONFIRMAR" -> {
                var cita = e.citas.porId(ctx.getCitaId());
                if (cita == null) {
                    yield elegirCita(ctx);
                }
                yield confirmado(ctx, cita);
            }
            case "CANCELAR" -> {
                var cita = e.citas.porId(ctx.getCitaId());
                if (cita == null) {
                    yield elegirCita(ctx);
                }
                yield preguntarCancelacion(ctx, cita);
            }
            case "REPROGRAMAR" -> pedirFechaReprogramar(ctx);
            case "MENU" -> fin(ctx, soporte().menuTexto());
            default -> elegirCita(ctx);
        };
    }

    private PasoResultado preguntarCancelacion(ConversacionContexto ctx, org.dentalcrm.domain.cita.Cita cita) {
        ctx.setCitaId(cita.getId());
        String texto = "¿Seguro que quieres cancelar tu cita de "
                + soporte().formatearFecha(cita.getFecha()) + " a las " + cita.getHoraInicio() + "?\n"
                + "1. Sí, cancelar\n2. No, mantenerla";
        return pregunta(ctx, texto, PASO_CANCELAR, MenuActual.GESTION_CITA, List.of(
                OpcionMenu.decision(1, "SI", "Sí, cancelar"),
                OpcionMenu.decision(2, "NO", "No, mantenerla")));
    }

    private PasoResultado cancelar(ConversacionContexto ctx, EntradaFlujo entrada) {
        if (entrada.esRechazo() || (entrada.tieneOpcion() && entrada.opcion().numero() == 2)) {
            return accionesDeLaCita(ctx, e.citas.porId(ctx.getCitaId()));
        }
        var cita = e.citas.porId(ctx.getCitaId());
        if (cita == null) {
            return elegirCita(ctx);
        }
        return cancelarConfirmado(ctx, cita);
    }

    private PasoResultado cancelarConfirmado(ConversacionContexto ctx, org.dentalcrm.domain.cita.Cita cita) {
        try {
            var cancelada = e.citas.cancelar(cita, "Cancelada por el paciente (agente IA)");
            return fin(ctx, "Listo, cancelé tu cita de " + cancelada.servicioNombre()
                    + " del " + soporte().formatearFecha(cancelada.fecha())
                    + ". Si cambias de opinión, escríbeme para reagendar 😉");
        } catch (Exception ex) {
            return fin(ctx, "No pude cancelar tu cita: " + ex.getMessage()
                    + "\n¿Quieres que una persona te ayude?");
        }
    }

    private PasoResultado confirmado(ConversacionContexto ctx, org.dentalcrm.domain.cita.Cita cita) {
        if (cita == null) {
            return fin(ctx, "No encuentro una cita próxima para confirmar. ¿Quieres agendar una nueva?");
        }
        try {
            var confirmada = e.citas.confirmar(cita);
            return fin(ctx, "¡Listo! 🙌 Confirmé tu cita de " + confirmada.servicioNombre()
                    + " para el " + soporte().formatearFecha(confirmada.fecha())
                    + " a las " + confirmada.horaInicio()
                    + "\nTe esperamos 😊 Si no puedes asistir, avísanos por este chat.");
        } catch (Exception ex) {
            return fin(ctx, "No pude confirmar tu cita. Puede que ya esté confirmada o cancelada."
                    + "\n¿Hablamos con una persona?");
        }
    }

    private PasoResultado elegirCita(ConversacionContexto ctx) {
        List<org.dentalcrm.domain.cita.Cita> citas = e.citas.proximas(pacienteActual(ctx));
        if (citas.isEmpty()) {
            List<OpcionMenu> opciones = List.of(
                    OpcionMenu.accion(1, "AGENDAR", "Agendar una nueva cita"),
                    OpcionMenu.accion(2, "MENU", "Volver al menú"));
            return pregunta(ctx, "No encuentro citas próximas para ti. 😕\n"
                    + "1. Agendar una nueva cita\n2. Volver al menú",
                    PASO_ELEGIR, MenuActual.GESTION_CITA, opciones);
        }
        StringBuilder sb = new StringBuilder("Elige la cita que quieres gestionar:\n");
        List<OpcionMenu> opciones = new java.util.ArrayList<>();
        for (int i = 0; i < citas.size(); i++) {
            var c = citas.get(i);
            String etiqueta = c.getServicio().getNombre() + " — "
                    + soporte().formatearFecha(c.getFecha()) + " a las " + c.getHoraInicio();
            opciones.add(new OpcionMenu(i + 1, c.getId(), TipoOpcion.ACCION, etiqueta, "ELEGIR"));
            sb.append(i + 1).append(". ").append(etiqueta).append("\n");
        }
        opciones.add(OpcionMenu.accion(opciones.size() + 1, "MENU", "Volver al menú"));
        sb.append(opciones.size()).append(". Volver al menú");
        return pregunta(ctx, sb.toString(), PASO_ELEGIR, MenuActual.GESTION_CITA, opciones);
    }

    // ------------------------------------------------------------------
    // Reprogramación
    // ------------------------------------------------------------------

    private PasoResultado pedirFechaReprogramar(ConversacionContexto ctx) {
        var cita = e.citas.porId(ctx.getCitaId());
        if (cita == null) {
            return elegirCita(ctx);
        }
        ctx.setFecha(null);
        ctx.setSlotId(null);
        String texto = "¿Para qué día quieres mover tu cita de "
                + soporte().formatearFecha(cita.getFecha()) + "?\n"
                + "Puede ser 'mañana', 'lunes' o '20/10' (a partir de hoy).";
        return pregunta(ctx, texto, PASO_FECHA, MenuActual.FECHAS, List.of(), "DATE");
    }

    private PasoResultado pedirHoraReprogramacion(ConversacionContexto ctx, EntradaFlujo entrada) {
        var cita = e.citas.porId(ctx.getCitaId());
        if (cita == null) {
            return elegirCita(ctx);
        }
        var fecha = entrada.entidades().fecha();
        if (fecha == null || fecha.isBefore(soporte().hoy())) {
            return repetir(ctx, "No entendí esa fecha. Escribe 'mañana', 'lunes' o '20/10'.");
        }
        ctx.setFecha(fecha);
        var slots = e.disponibilidad.slots(cita.getDoctor().getId(), fecha, cita.getServicio().getId());
        if (slots.isEmpty()) {
            return fin(ctx, "No hay cupos el " + soporte().formatearFecha(fecha)
                    + " con ese odontólogo. 😕 Escribe 'reprogramar' cuando quieras elegir otro día.");
        }
        StringBuilder sb = new StringBuilder("¿A qué hora te conviene el ")
                .append(soporte().formatearFecha(fecha)).append("?\n\n")
                .append(soporte().horasDeSlots(slots))
                .append("\nResponde el número de la opción (1-").append(slots.size()).append(").");
        java.util.List<OpcionMenu> opciones = new java.util.ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            opciones.add(OpcionMenu.hora(i + 1, slots.get(i).horaInicio().toString(),
                    slots.get(i).horaInicio().toString()));
        }
        return pregunta(ctx, sb.toString(), PASO_HORA, MenuActual.HORAS, opciones, "HORA");
    }

    private PasoResultado reprogramar(Conversacion conversacion, ConversacionContexto ctx,
                                      EntradaFlujo entrada) {
        var cita = e.citas.porId(ctx.getCitaId());
        if (cita == null) {
            return elegirCita(ctx);
        }
        String hora = entrada.opcion() != null && entrada.opcion().tipo() == TipoOpcion.HORA
                ? entrada.opcion().valor()
                : (entrada.entidades().hora() != null ? entrada.entidades().hora().toString() : null);
        if (hora == null) {
            return repetir(ctx, "Elige el número de un cupo de la lista.");
        }
        try {
            var actualizada = e.citas.actualizar(cita, ctx.getFecha(),
                    java.time.LocalTime.parse(hora));
            return fin(ctx, "Listo ✔ Moví tu cita de " + actualizada.servicioNombre()
                    + " al " + soporte().formatearFecha(actualizada.fecha())
                    + " a las " + actualizada.horaInicio() + ".");
        } catch (Exception ex) {
            return fin(ctx, "No pude reprogramar tu cita: " + ex.getMessage()
                    + "\n¿Quieres que una persona te ayude?");
        }
    }
}
