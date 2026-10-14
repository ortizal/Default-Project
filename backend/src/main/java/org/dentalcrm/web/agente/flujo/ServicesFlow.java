package org.dentalcrm.web.agente.flujo;

import org.dentalcrm.domain.servicio.Servicio;
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
 * Lista los servicios publicados con su duración y precio (§16: los precios
 * los decide el backend, nunca el modelo).
 */
public class ServicesFlow extends FlujoBase {

    public static final String CODIGO = "VER_SERVICIOS";
    private static final String PASO_LISTA = "LISTAR_SERVICIOS";
    private static final String PASO_DETALLE = "DETALLE_SERVICIO";

    public ServicesFlow(AgenteEntorno e) {
        super(e);
    }

    @Override
    public String codigo() {
        return CODIGO;
    }

    @Override
    public PasoResultado responder(Conversacion conversacion, ConversacionContexto ctx, EntradaFlujo entrada) {
        if (ctx.getPaso() == null) {
            return listar(ctx);
        }
        OpcionMenu opcion = entrada.opcion();
        if (opcion != null && opcion.tipo() == TipoOpcion.ACCION) {
            return accion(ctx, opcion);
        }
        if (opcion != null && opcion.tipo() == TipoOpcion.SERVICIO) {
            return detalle(ctx, opcion);
        }
        if (PASO_DETALLE.equals(ctx.getPaso())) {
            return detalle(ctx, null);
        }
        return listar(ctx);
    }

    private PasoResultado listar(ConversacionContexto ctx) {
        List<Servicio> servicios = e.servicios.activos();
        if (servicios.isEmpty()) {
            return fin(ctx, "Aún no tenemos servicios publicados. Escribe 'humano' y te ayudamos.");
        }
        ctx.setFlujo(CODIGO);
        List<OpcionMenu> opciones = new ArrayList<>();
        StringBuilder sb = new StringBuilder("Estos son nuestros servicios:\n");
        for (int i = 0; i < servicios.size(); i++) {
            Servicio s = servicios.get(i);
            opciones.add(OpcionMenu.servicio(i + 1, s.getId(), s.getNombre()));
            sb.append(i + 1).append(". ").append(s.getNombre())
                    .append(" — ").append(s.getDuracionMinutos()).append(" min")
                    .append(" — $").append(s.getPrecio()).append("\n");
        }
        opciones.add(OpcionMenu.accion(opciones.size() + 1, "AGENDAR", "Agendar una cita"));
        sb.append(opciones.size()).append(". Agendar una cita\n");
        opciones.add(OpcionMenu.accion(opciones.size() + 1, "MENU", "Volver al menú"));
        sb.append(opciones.size()).append(". Volver al menú");
        return pregunta(ctx, sb.toString(), PASO_LISTA, MenuActual.SERVICIOS, opciones);
    }

    private PasoResultado detalle(ConversacionContexto ctx, OpcionMenu opcion) {
        Long servicioId = opcion != null ? opcion.id() : ctx.getServicioId();
        Servicio servicio = servicioId == null ? null : e.servicios.porId(servicioId).orElse(null);
        if (servicio == null) {
            return listar(ctx);
        }
        ctx.setServicioId(servicio.getId());
        ctx.setServicioResuelto(true);
        List<OpcionMenu> opciones = List.of(
                OpcionMenu.accion(1, "AGENDAR", "Agendar este servicio"),
                OpcionMenu.accion(2, "VER", "Ver otros servicios"),
                OpcionMenu.accion(3, "MENU", "Volver al menú"));
        String texto = "🦷 " + servicio.getNombre() + "\n"
                + "⏱ " + servicio.getDuracionMinutos() + " minutos\n"
                + "💵 $" + servicio.getPrecio() + "\n\n"
                + "1. Agendar este servicio\n2. Ver otros servicios\n3. Volver al menú";
        return pregunta(ctx, texto, PASO_DETALLE, MenuActual.SERVICIOS, opciones);
    }

    private PasoResultado accion(ConversacionContexto ctx, OpcionMenu opcion) {
        return switch (opcion.valor()) {
            case "AGENDAR" -> {
                ctx.setFlujo(ScheduleAppointmentFlow.CODIGO);
                ctx.setPaso(null);
                ctx.setFlujoRetorno(null);
                ctx.setPasoRetorno(null);
                yield PasoResultado.reenviar();
            }
            case "VER" -> listar(ctx);
            case "MENU" -> fin(ctx, soporte().menuTexto());
            default -> listar(ctx);
        };
    }
}
