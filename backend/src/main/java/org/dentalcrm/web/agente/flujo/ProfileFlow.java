package org.dentalcrm.web.agente.flujo;

import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.whatsapp.Conversacion;
import org.dentalcrm.web.agente.contexto.ConversacionContexto;
import org.dentalcrm.web.agente.contexto.MenuActual;
import org.dentalcrm.web.agente.contexto.OpcionMenu;
import org.dentalcrm.web.agente.contexto.TipoOpcion;
import org.dentalcrm.web.agente.engine.EntradaFlujo;
import org.dentalcrm.web.agente.engine.PasoResultado;

import java.util.List;

/**
 * Muestra la ficha del paciente (§7: requiere identificación).
 */
public class ProfileFlow extends FlujoBase {

    public static final String CODIGO = "VER_PERFIL";
    private static final String PASO_DATOS = "MOSTRAR_PERFIL";

    public ProfileFlow(AgenteEntorno e) {
        super(e);
    }

    @Override
    public String codigo() {
        return CODIGO;
    }

    @Override
    public PasoResultado responder(Conversacion conversacion, ConversacionContexto ctx, EntradaFlujo entrada) {
        ctx.setFlujo(CODIGO);
        if (ctx.getPaso() == null) {
            ctx.setPaso(PASO_DATOS);
        }
        PasoResultado identificacion = pedirIdentificacion(conversacion, ctx, null);
        if (identificacion != null) {
            return identificacion;
        }
        return mostrar(conversacion, ctx, entrada);
    }

    private PasoResultado mostrar(Conversacion conv, ConversacionContexto ctx, EntradaFlujo entrada) {
        Paciente paciente = e.pacientes.porId(ctx.getPacienteId()).orElse(conv.getPaciente());
        if (paciente == null) {
            return pedirIdentificacion(conv, ctx, null);
        }
        OpcionMenu opcion = entrada.opcion();
        if (opcion != null && opcion.tipo() == TipoOpcion.ACCION) {
            return accion(conv, ctx, opcion);
        }
        List<OpcionMenu> opciones = List.of(
                OpcionMenu.accion(1, "CITAS", "Ver mis citas"),
                OpcionMenu.accion(2, "AGENDAR", "Agendar una cita"),
                OpcionMenu.accion(3, "MENU", "Volver al menú"));
        String texto = "Datos:\n"
                + "📋 Nombre: " + e.pacientes.nombre(paciente) + "\n"
                + "📇 Cédula: " + mascar(paciente.getCedula()) + "\n"
                + "📱 Teléfono: " + mascar(paciente.getTelefono()) + "\n"
                + (e.pacientes.tutorDe(paciente) == null ? "" : "👨‍👩‍👧 Tutor: " + e.pacientes.tutorDe(paciente) + "\n")
                + "\n1. Ver mis citas\n2. Agendar una cita\n3. Volver al menú";
        return pregunta(ctx, texto, PASO_DATOS, MenuActual.PRINCIPAL, opciones);
    }

    /** Enmascara cédula y teléfono al mostrarlos (§7). */
    private String mascar(String valor) {
        if (valor == null || valor.isBlank()) {
            return "—";
        }
        String v = valor.trim();
        if (v.length() <= 5) {
            return "*".repeat(v.length());
        }
        return v.substring(0, 3) + "*".repeat(v.length() - 5) + v.substring(v.length() - 2);
    }

    private PasoResultado accion(Conversacion conv, ConversacionContexto ctx, OpcionMenu opcion) {
        return switch (opcion.valor()) {
            case "CITAS" -> {
                ctx.setFlujo(ViewAppointmentsFlow.CODIGO);
                ctx.setPaso(null);
                yield PasoResultado.reenviar();
            }
            case "AGENDAR" -> {
                ctx.setFlujo(ScheduleAppointmentFlow.CODIGO);
                ctx.setPaso(null);
                yield PasoResultado.reenviar();
            }
            case "MENU" -> fin(ctx, soporte().menuTexto());
            default -> mostrar(conv, ctx, null);
        };
    }
}
