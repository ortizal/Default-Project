package org.dentalcrm.web.agente.flujo;

import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.whatsapp.Conversacion;
import org.dentalcrm.web.agente.contexto.ConversacionContexto;
import org.dentalcrm.web.agente.contexto.MenuActual;
import org.dentalcrm.web.agente.contexto.OpcionMenu;
import org.dentalcrm.web.agente.contexto.TipoOpcion;
import org.dentalcrm.web.agente.engine.EntradaFlujo;
import org.dentalcrm.web.agente.engine.PasoResultado;

import java.util.ArrayList;
import java.util.List;

/**
 * Lista las citas próximas del paciente (§7: requiere identificación).
 */
public class ViewAppointmentsFlow extends FlujoBase {

    public static final String CODIGO = "VER_CITAS";
    private static final String PASO_LISTA = "LISTAR_CITAS";

    public ViewAppointmentsFlow(AgenteEntorno e) {
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
            ctx.setPaso(PASO_LISTA);
            PasoResultado identificacion = pedirIdentificacion(conversacion, ctx, null);
            if (identificacion != null) {
                return identificacion;
            }
            return listar(ctx);
        }
        OpcionMenu opcion = entrada.opcion();
        if (opcion != null && opcion.tipo() == TipoOpcion.ACCION) {
            return accion(ctx, opcion);
        }
        return listar(ctx);
    }

    private PasoResultado listar(ConversacionContexto ctx) {
        List<Cita> citas = e.citas.proximas(pacienteActual(ctx));
        List<OpcionMenu> opciones = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        if (citas.isEmpty()) {
            sb.append("No encuentro citas próximas para ti. 😕\n");
        } else {
            sb.append("Tienes ").append(citas.size()).append(" cita(s) próxima(s):\n");
            for (int i = 0; i < citas.size(); i++) {
                Cita c = citas.get(i);
                String etiqueta = c.getServicio().getNombre() + " — "
                        + soporte().formatearFecha(c.getFecha()) + " a las " + c.getHoraInicio()
                        + " con " + c.getDoctor().getNombres() + " " + c.getDoctor().getApellidos();
                opciones.add(new OpcionMenu(i + 1, c.getId(), TipoOpcion.ACCION, etiqueta, "ELEGIR"));
                sb.append(i + 1).append(". ").append(etiqueta).append("\n");
            }
            sb.append("\n");
        }
        opciones.add(OpcionMenu.accion(opciones.size() + 1, "AGENDAR", "Agendar una nueva cita"));
        sb.append(opciones.size()).append(". Agendar una nueva cita\n");
        opciones.add(OpcionMenu.accion(opciones.size() + 1, "MENU", "Volver al menú"));
        sb.append(opciones.size()).append(". Volver al menú");
        return pregunta(ctx, sb.toString(), PASO_LISTA, MenuActual.GESTION_CITA, opciones);
    }

    private PasoResultado accion(ConversacionContexto ctx, OpcionMenu opcion) {
        if ("ELEGIR".equals(opcion.valor()) && opcion.id() != null) {
            ctx.setCitaId(opcion.id());
            ctx.setFlujo(ManageAppointmentFlow.CODIGO);
            ctx.setPaso("ACCION_CITA");
            ctx.setMenuActual(MenuActual.GESTION_CITA);
            ctx.setOpciones(new ArrayList<>());
            return PasoResultado.reenviar();
        }
        return switch (opcion.valor()) {
            case "GESTIONAR" -> {
                ctx.setFlujo(ManageAppointmentFlow.CODIGO);
                ctx.setPaso(null);
                yield PasoResultado.reenviar();
            }
            case "AGENDAR" -> {
                ctx.setFlujo(ScheduleAppointmentFlow.CODIGO);
                ctx.setPaso(null);
                yield PasoResultado.reenviar();
            }
            case "MENU" -> fin(ctx, soporte().menuTexto());
            default -> listar(ctx);
        };
    }
}
