package org.dentalcrm.web.agente.flujo;

import org.dentalcrm.domain.horario.HorarioOdontologo;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.whatsapp.Conversacion;
import org.dentalcrm.web.agente.AgenteConversacionalService;
import org.dentalcrm.web.agente.contexto.ConversacionContexto;
import org.dentalcrm.web.agente.contexto.MenuActual;
import org.dentalcrm.web.agente.contexto.OpcionMenu;
import org.dentalcrm.web.agente.contexto.TipoOpcion;
import org.dentalcrm.web.agente.engine.EntradaFlujo;
import org.dentalcrm.web.agente.engine.PasoResultado;
import org.dentalcrm.web.agente.tool.AvailabilityTool;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Flujo de disponibilidad (§8):
 * {@code CONSULTAR_DISPONIBILIDAD → SELECCIONAR_SERVICIO → SELECCIONAR_ODONTOLOGO
 * → SELECCIONAR_FECHA → SELECCIONAR_FRANJA → CONSULTAR_SLOTS → MOSTRAR_DISPONIBILIDAD}.
 *
 * <p>Nunca pide cédula para consultar (§7). Si el mensaje ya trae doctor y no
 * fecha, responde el horario general con el PDF; si trae fecha concreta, lista
 * los cupos reales de ese día (§10: sólo pregunta los datos que faltan).
 * Al elegir un cupo continúa en la reserva (§9).
 */
public class DisponibilidadFlow extends AgendaPasos {

    public static final String CODIGO = "CONSULTAR_DISPONIBILIDAD";
    public static final String PASO_HORARIO = "HORARIO_SEMANAL";

    public DisponibilidadFlow(AgenteEntorno e) {
        super(e);
    }

    @Override
    public String codigo() {
        return CODIGO;
    }

    @Override
    protected String tituloEleccionOdontologo() {
        return "¿De qué odontólogo quieres consultar disponibilidad?";
    }

    @Override
    protected String etiquetaCualquierOdontologo() {
        return "Ver disponibilidad de todos";
    }

    @Override
    public PasoResultado responder(Conversacion conversacion, ConversacionContexto ctx, EntradaFlujo entrada) {
        PasoResultado correccion = aplicarCorreccion(ctx, entrada);
        if (correccion != null) {
            return correccion;
        }
        String paso = ctx.getPaso();
        if (paso == null || PASO_HORARIO.equals(paso)) {
            return arrancar(conversacion, ctx, entrada);
        }
        if (PASO_SERVICIO.equals(paso) || "ESCRIBIR_SERVICIO".equals(paso)) {
            PasoResultado r = servicioElegido(ctx, entrada.opcion(), entrada);
            return r != null ? r : repetir(ctx, "Elige un servicio de la lista.");
        }
        if (PASO_MODALIDAD.equals(paso)) {
            return modalidadElegida(ctx, entrada.opcion(), entrada.crudo());
        }
        if (PASO_ODONTOLOGO.equals(paso)) {
            if (ctx.isConsultaHorario()) {
                return doctorParaHorario(ctx, entrada);
            }
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
        ctx.setFlujo(CODIGO);
        return siguientePendiente(ctx);
    }

    // ------------------------------------------------------------------
    // Arranque: horario general, cupos del día o pasos del §8 (§10)
    // ------------------------------------------------------------------

    private PasoResultado arrancar(Conversacion conv, ConversacionContexto ctx, EntradaFlujo entrada) {
        ctx.setFlujo(CODIGO);
        precargarEntidades(ctx, entrada);

        if (ctx.getFecha() != null && ctx.isOdontologoResuelto()) {
            ctx.setConsultaHorario(false);
            return cuposDelDia(ctx);
        }
        if (ctx.getFecha() == null && ctx.isOdontologoResuelto()
                && (ctx.getPaso() == null || pideHorario(entrada))) {
            return horarioSemanal(ctx);
        }
        ctx.setConsultaHorario(false);
        PasoResultado pendiente = siguientePendiente(ctx);
        if (pendiente != null) {
            return pendiente;
        }
        return cuposDelDia(ctx);
    }

    private boolean pideHorario(EntradaFlujo entrada) {
        String t = entrada.texto();
        if (t == null || t.isBlank()) {
            return false;
        }
        return t.contains("atiende") || t.contains("atienden") || t.contains("horario")
                || t.contains("dias de atencion") || t.contains("cuando") || t.contains("que dias")
                || t.contains("agenda") || t.contains("semana");
    }

    /** Cupos reales de un día concreto, en dos columnas (§16: datos del backend). */
    private PasoResultado cuposDelDia(ConversacionContexto ctx) {
        List<org.dentalcrm.web.agenda.dto.SlotResponse> slots =
                e.disponibilidad.slots(ctx.getOdontologoId(), ctx.getFecha(), ctx.getServicioId());
        if (slots.isEmpty()) {
            return sinCupos(ctx);
        }
        List<OpcionMenu> opciones = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            opciones.add(OpcionMenu.hora(i + 1, slots.get(i).horaInicio().toString(),
                    slots.get(i).horaInicio().toString()));
        }
        String texto = "Para el " + soporte().formatearFecha(ctx.getFecha())
                + " con " + resumenOdontologo(ctx) + " hay estos cupos:\n\n"
                + soporte().horasDeSlots(slots)
                + "\n\nElige el número del cupo que te sirva (1-" + slots.size()
                + ") o escríbeme 'quiero una cita'.";
        return pregunta(ctx, texto, PASO_SLOTS, MenuActual.HORAS, opciones, "HORA");
    }

    // ------------------------------------------------------------------
    // Horario general del odontólogo + PDF (§16: lo da el backend)
    // ------------------------------------------------------------------

    private PasoResultado horarioSemanal(ConversacionContexto ctx) {
        Long doctorId = ctx.getOdontologoId();
        Odontologo doctor = doctorId == null ? null : e.odontologos.porId(doctorId).orElse(null);
        if (doctor == null) {
            return fin(ctx, "No encuentro ese odontólogo. ¿Quieres probar con otro?");
        }
        List<HorarioOdontologo> horarios = e.disponibilidad.horariosActivos(doctorId);
        if (horarios.isEmpty()) {
            ctx.setFlujo(CODIGO);
            ctx.setPaso(PASO_HORARIO);
            ctx.setMenuActual(MenuActual.ODONTOLOGOS);
            ctx.setOpciones(new ArrayList<>());
            String texto = "El Dr. " + doctor.getNombres() + " " + doctor.getApellidos()
                    + " no tiene horarios de atención configurados. 😕\n¿Quieres hablar con una persona para agendar?";
            ctx.setUltimaPregunta(texto);
            return PasoResultado.continuar(texto);
        }
        List<String> cupos = e.disponibilidad.proximosCupos(doctorId, ctx.getServicioId());
        StringBuilder sb = new StringBuilder("El Dr. ").append(doctor.getNombres()).append(" ")
                .append(doctor.getApellidos()).append(" atiende:\n")
                .append(soporte().resumenHorario(horarios)).append("\n");
        if (!cupos.isEmpty()) {
            sb.append("\nPróximos cupos disponibles:\n");
            for (int i = 0; i < Math.min(cupos.size(), 5); i++) {
                sb.append("• ").append(cupos.get(i)).append("\n");
            }
        }
        sb.append("\nTe adjunto el horario completo y la disponibilidad de los próximos 7 días en PDF 😊");
        String mensaje = sb.toString();

        AgenteConversacionalService.Adjunto adjunto = null;
        try {
            byte[] pdf = e.disponibilidad.pdf(doctor, horarios, cupos,
                    e.disponibilidad.disponibilidadPorDia(doctorId, ctx.getServicioId()));
            if (pdf != null) {
                adjunto = new AgenteConversacionalService.Adjunto(
                        e.disponibilidad.nombreArchivo(doctor), "application/pdf", pdf, "Horario de atención");
            }
        } catch (Exception ex) {
            // El PDF es complementario: la respuesta de horario se envía igual.
        }

        ctx.setFlujo(CODIGO);
        ctx.setPaso(PASO_HORARIO);
        ctx.setConsultaHorario(true);
        ctx.setMenuActual(MenuActual.ODONTOLOGOS);
        ctx.setOpciones(new ArrayList<>());
        ctx.setExpectedInput("TEXT");
        ctx.setUltimaPregunta(mensaje);
        return PasoResultado.continuar(mensaje).adjunto(adjunto);
    }

    /** Pregunta el odontólogo cuando se pidió el horario general (§7: sin cédula). */
    private PasoResultado doctorParaHorario(ConversacionContexto ctx, EntradaFlujo entrada) {
        OpcionMenu opcion = entrada.opcion();
        if (opcion != null && opcion.tipo() == TipoOpcion.ODONTOLOGO && opcion.id() != null) {
            ctx.setOdontologoId(opcion.id());
            ctx.setOdontologoResuelto(true);
            return horarioSemanal(ctx);
        }
        var porNombre = e.odontologos.porNombre(entrada.crudo());
        if (porNombre.isPresent()) {
            ctx.setOdontologoId(porNombre.get().getId());
            ctx.setOdontologoResuelto(true);
            return horarioSemanal(ctx);
        }
        return repetir(ctx, "No entendí \"" + soporte().recortar(entrada.crudo())
                + "\" como un odontólogo.");
    }

    // ------------------------------------------------------------------
    // Elegir cupo → reserva (§9)
    // ------------------------------------------------------------------

    @Override
    protected PasoResultado trasCupo(Conversacion conv, ConversacionContexto ctx, EntradaFlujo entrada) {
        ctx.setFlujo(ScheduleAppointmentFlow.CODIGO);
        ctx.setPaso(ScheduleAppointmentFlow.PASO_RESERVAR);
        ctx.setFlujoRetorno(null);
        ctx.setPasoRetorno(null);
        ctx.setMenuActual(MenuActual.CONFIRMACION);
        ctx.setOpciones(new ArrayList<>());
        ctx.setExpectedInput("OPTION");
        return PasoResultado.reenviar();
    }
}
