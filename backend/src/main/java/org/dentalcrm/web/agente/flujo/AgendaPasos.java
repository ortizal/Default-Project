package org.dentalcrm.web.agente.flujo;

import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.whatsapp.Conversacion;
import org.dentalcrm.domain.servicio.Servicio;
import org.dentalcrm.web.agente.AgenteSoporte;
import org.dentalcrm.web.agente.ParserAgendamiento;
import org.dentalcrm.web.agente.contexto.ConversacionContexto;
import org.dentalcrm.web.agente.contexto.MenuActual;
import org.dentalcrm.web.agente.contexto.OpcionMenu;
import org.dentalcrm.web.agente.contexto.TipoOpcion;
import org.dentalcrm.web.agente.engine.EntradaFlujo;
import org.dentalcrm.web.agente.engine.EntityExtractor;
import org.dentalcrm.web.agente.engine.PasoResultado;
import org.dentalcrm.web.agente.tool.AvailabilityTool;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Pasos compartidos del flujo de agenda (§8): servicio → odontólogo → fecha →
 * franja → slots reales, más las correcciones de un dato ya elegido (§12).
 *
 * <p>Los usan {@link DisponibilidadFlow} y {@link ScheduleAppointmentFlow}.
 */
abstract class AgendaPasos extends FlujoBase {

    protected static final String PASO_SERVICIO = "SELECCIONAR_SERVICIO";
    protected static final String PASO_ODONTOLOGO = "SELECCIONAR_ODONTOLOGO";
    protected static final String PASO_MODALIDAD = "ELEGIR_MODALIDAD";
    protected static final String PASO_FECHA = "SELECCIONAR_FECHA";
    protected static final String PASO_FRANJA = "SELECCIONAR_FRANJA";
    protected static final String PASO_SLOTS = "SELECCIONAR_HORA";
    protected static final String PASO_SIN_CUPOS = "SIN_DISPONIBILIDAD";
    protected static final String PASO_FALLA_CALENDARIO = "FALLA_CALENDARIO";

    protected AgendaPasos(AgenteEntorno e) {
        super(e);
    }

    // ------------------------------------------------------------------
    // Servicio (§8)
    // ------------------------------------------------------------------

    protected PasoResultado pedirServicio(ConversacionContexto ctx, String error) {
        List<Servicio> servicios = e.servicios.activos();
        if (servicios.isEmpty()) {
            return fin(ctx, "Aún no tenemos servicios publicados. Escribe 'humano' y te ayudamos.");
        }
        if (servicios.size() == 1) {
            ctx.setServicioId(servicios.get(0).getId());
            ctx.setServicioLibre(null);
            ctx.setServicioResuelto(true);
            return despuesDeServicio(ctx);
        }
        List<OpcionMenu> opciones = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        if (error != null) {
            sb.append(error).append("\n\n");
        }
        sb.append("¿Qué atención necesitas?\n");
        for (int i = 0; i < servicios.size(); i++) {
            Servicio s = servicios.get(i);
            opciones.add(OpcionMenu.servicio(i + 1, s.getId(), s.getNombre()));
            sb.append(i + 1).append(". ").append(s.getNombre()).append("\n");
        }
        opciones.add(OpcionMenu.servicio(opciones.size() + 1, null, "Otro", "OTRO"));
        sb.append(opciones.size()).append(". Otro\n");
        opciones.add(OpcionMenu.servicio(opciones.size() + 1, null, "No estoy seguro", "INCIERTO"));
        sb.append(opciones.size()).append(". No estoy seguro");
        return pregunta(ctx, sb.toString(), PASO_SERVICIO, MenuActual.SERVICIOS, opciones);
    }

    protected PasoResultado servicioElegido(ConversacionContexto ctx, OpcionMenu opcion, EntradaFlujo entrada) {
        if (opcion != null && opcion.tipo() == TipoOpcion.SERVICIO) {
            if (opcion.id() != null) {
                ctx.setServicioId(opcion.id());
                ctx.setServicioLibre(null);
                ctx.setServicioResuelto(true);
                ctx.setSlotId(null);
                return despuesDeServicio(ctx);
            }
            if ("INCIERTO".equals(opcion.valor())) {
                ctx.setServicioId(null);
                ctx.setServicioLibre(null);
                ctx.setServicioResuelto(true);
                ctx.setSlotId(null);
                return despuesDeServicio(ctx);
            }
            ctx.setServicioId(null);
            ctx.setServicioResuelto(true);
            ctx.setSlotId(null);
            return escribirServicio(ctx);
        }
        if ("ESCRIBIR_SERVICIO".equals(ctx.getPaso())) {
            ctx.setServicioLibre(entrada.crudo());
            ctx.setServicioId(null);
            ctx.setServicioResuelto(true);
            ctx.setSlotId(null);
            return despuesDeServicio(ctx);
        }
        return null;
    }

    private PasoResultado escribirServicio(ConversacionContexto ctx) {
        String texto = "Perfecto, ¿qué servicio necesitas? Escríbelo con tus palabras.";
        ctx.setPaso("ESCRIBIR_SERVICIO");
        ctx.setExpectedInput("TEXT");
        ctx.setMenuActual(MenuActual.SERVICIOS);
        ctx.setOpciones(new ArrayList<>());
        ctx.setUltimaPregunta(texto);
        return PasoResultado.continuar(texto);
    }

    // ------------------------------------------------------------------
    // Odontólogo (§8): siempre se ofrece "cualquier odontólogo"
    // ------------------------------------------------------------------

    protected PasoResultado pedirOdontologo(ConversacionContexto ctx, String error) {
        List<Odontologo> odontologos = e.odontologos.activos();
        if (odontologos.isEmpty()) {
            return fin(ctx, "Aún no tenemos odontólogos activos. Escribe 'humano' para más ayuda.");
        }
        if (odontologos.size() == 1) {
            ctx.setOdontologoId(odontologos.get(0).getId());
            ctx.setOdontologoResuelto(true);
            return despuesDeOdontologo(ctx);
        }
        List<OpcionMenu> opciones = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        if (error != null) {
            sb.append(error).append("\n\n");
        }
        // §3/§4 del plan: lista los doctores primero y el "todos" al final.
        sb.append(tituloEleccionOdontologo()).append("\n");
        for (int i = 0; i < odontologos.size(); i++) {
            Odontologo o = odontologos.get(i);
            String etiqueta = e.soporte.nombreOdontologo(o)
                    + (o.getEspecialidad() == null || o.getEspecialidad().isBlank()
                    ? "" : " — " + o.getEspecialidad());
            opciones.add(OpcionMenu.odontologo(i + 1, o.getId(), etiqueta));
            sb.append(i + 1).append(". ").append(etiqueta).append("\n");
        }
        opciones.add(OpcionMenu.odontologo(opciones.size() + 1, null, etiquetaCualquierOdontologo()));
        sb.append(opciones.size()).append(". ").append(etiquetaCualquierOdontologo());
        return pregunta(ctx, sb.toString().stripTrailing(), PASO_ODONTOLOGO,
                MenuActual.ODONTOLOGOS, opciones);
    }

    protected PasoResultado odontologoElegido(ConversacionContexto ctx, OpcionMenu opcion, String texto) {
        if (opcion != null && opcion.tipo() == TipoOpcion.ODONTOLOGO) {
            ctx.setOdontologoId(opcion.id());
            ctx.setOdontologoResuelto(true);
            ctx.setOdontologoLibre(opcion.id() == null ? null : opcion.label());
            ctx.setSlotId(null);
            return despuesDeOdontologo(ctx);
        }
        var porNombre = e.odontologos.porNombre(texto);
        if (porNombre.isPresent()) {
            ctx.setOdontologoId(porNombre.get().getId());
            ctx.setOdontologoResuelto(true);
            ctx.setOdontologoLibre(null);
            ctx.setSlotId(null);
            return despuesDeOdontologo(ctx);
        }
        return repetir(ctx, "No entendí \"" + soporte().recortar(texto) + "\" como un odontólogo.");
    }

    /** Título de la lista de odontólogos (§3 agendar / §4 disponibilidad). */
    protected String tituloEleccionOdontologo() {
        return "¿Con qué odontólogo deseas agendar?";
    }

    /** Última opción de la lista: aplica a ambos flujos (§3/§4). */
    protected String etiquetaCualquierOdontologo() {
        return "Cualquier odontólogo disponible";
    }

    // ------------------------------------------------------------------
    // Modalidad chat o calendario web (§2, §10 del plan)
    // ------------------------------------------------------------------

    protected PasoResultado despuesDeOdontologo(ConversacionContexto ctx) {
        if (ctx.getFecha() == null && ctx.getSlotId() == null && !ctx.isModalidadResuelta()) {
            return pedirModalidad(ctx, null);
        }
        return despuesDeServicio(ctx);
    }

    protected PasoResultado pedirModalidad(ConversacionContexto ctx, String error) {
        boolean agendar = ScheduleAppointmentFlow.CODIGO.equals(codigo());
        StringBuilder sb = new StringBuilder();
        if (error != null) {
            sb.append(error).append("\n\n");
        }
        sb.append(agendar ? "¿Cómo quieres buscar tu cita?" : "¿Cómo quieres revisar la disponibilidad?")
                .append("\n");
        sb.append(agendar ? "1. 📅 Elegir día por este chat" : "1. 💬 Consultar una fecha por este chat")
                .append("\n");
        sb.append(agendar ? "2. 🗓️ Ver calendario de disponibilidad" : "2. 🗓️ Abrir calendario de disponibilidad");
        List<OpcionMenu> opciones = List.of(
                OpcionMenu.accion(1, "CHAT", "Por este chat"),
                OpcionMenu.accion(2, "CALENDARIO", "Calendario web"));
        return pregunta(ctx, sb.toString(), PASO_MODALIDAD, MenuActual.ODONTOLOGOS, opciones, "OPCION");
    }

    protected PasoResultado modalidadElegida(ConversacionContexto ctx, OpcionMenu opcion, String texto) {
        String valor = opcion != null && opcion.tipo() == TipoOpcion.ACCION ? opcion.valor() : null;
        if (valor == null) {
            String t = soporte().compactar(texto);
            if (t.contains("chat")) {
                valor = "CHAT";
            } else if (t.contains("calendario") || t.contains("enlace") || t.contains("web")) {
                valor = "CALENDARIO";
            }
        }
        if (valor == null) {
            return repetir(ctx, "Elige 1 para continuar por este chat o 2 para abrir el calendario.");
        }
        ctx.setModalidadReserva(valor);
        ctx.setModalidadResuelta(true);
        return despuesDeModalidad(ctx);
    }

    /** Tras la modalidad: el servicio se resuelve antes de la fecha (§9). */
    protected PasoResultado despuesDeModalidad(ConversacionContexto ctx) {
        if (!ctx.isServicioResuelto()) {
            return pedirServicio(ctx, null);
        }
        return despuesDeServicio(ctx);
    }

    /** Servicio listo → fecha (chat) o enlace temporal firmado (calendario web, §10). */
    protected PasoResultado despuesDeServicio(ConversacionContexto ctx) {
        if ("CALENDARIO".equals(ctx.getModalidadReserva())) {
            return generarEnlaceCalendario(ctx);
        }
        if (ctx.getFecha() == null) {
            return pedirFecha(ctx, null);
        }
        if (ctx.getFranja() == null) {
            return pedirFranja(ctx);
        }
        if (ctx.getSlotId() == null) {
            return mostrarSlots(ctx, null);
        }
        return null;
    }

    protected PasoResultado generarEnlaceCalendario(ConversacionContexto ctx) {
        if (e.agendaPublica == null || ctx.getOdontologoId() == null) {
            return fallaCalendario(ctx, null);
        }
        if (ScheduleAppointmentFlow.CODIGO.equals(codigo()) && ctx.getPacienteId() == null) {
            return fallaCalendario(ctx, "Para reservar desde el calendario necesito identificar al paciente.");
        }
        try {
            String token = e.agendaPublica.generarEnlace(ctx.getPacienteId(), ctx.getOdontologoId(),
                    ctx.getServicioId());
            String texto = "Claro 😊 Abre este enlace para elegir tu cita en el calendario:\n"
                    + soporte().urlAgendaPublica(token)
                    + "\n\nEl enlace es personal y caduca en " + e.agendaPublica.ttlMinutos() + " minutos."
                    + "\n\n¿Puedo ayudarte con algo más?\n" + soporte().menuTexto();
            return fin(ctx, texto);
        } catch (Exception ex) {
            return fallaCalendario(ctx, null);
        }
    }

    /**
     * Fallback del calendario (§13): nunca obliga a escribir "humano"; ofrece
     * seguir por chat, reintentar, recepción o volver al menú sin perder el
     * proceso en curso.
     */
    protected PasoResultado fallaCalendario(ConversacionContexto ctx, String detalle) {
        StringBuilder sb = new StringBuilder(
                "No pude abrir el calendario en este momento. 😕\nPodemos continuar sin perder el proceso.");
        if (detalle != null && !detalle.isBlank()) {
            sb.append("\n\n").append(detalle);
        }
        sb.append("\n\n1. 💬 Ver horarios disponibles por este chat")
                .append("\n2. 🔄 Intentar abrir el calendario nuevamente")
                .append("\n3. 💬 Hablar con recepción")
                .append("\n4. Volver al menú");
        List<OpcionMenu> opciones = List.of(
                OpcionMenu.accion(1, "CALENDARIO_CHAT", "Ver horarios por este chat"),
                OpcionMenu.accion(2, "CALENDARIO_REINTENTAR", "Reintentar calendario"),
                OpcionMenu.accion(3, "CALENDARIO_RECEPCION", "Hablar con recepción"),
                OpcionMenu.accion(4, "CALENDARIO_MENU", "Volver al menú"));
        return pregunta(ctx, sb.toString(), PASO_FALLA_CALENDARIO, MenuActual.ODONTOLOGOS, opciones, "OPCION");
    }

    /** Opción elegida dentro del fallback del calendario (§13). */
    protected PasoResultado accionFallaCalendario(ConversacionContexto ctx, OpcionMenu opcion) {
        if (opcion == null || opcion.tipo() != TipoOpcion.ACCION) {
            return null;
        }
        return switch (opcion.valor()) {
            case "CALENDARIO_CHAT" -> {
                ctx.setModalidadReserva("CHAT");
                ctx.setModalidadResuelta(true);
                yield despuesDeServicio(ctx);
            }
            case "CALENDARIO_REINTENTAR" -> generarEnlaceCalendario(ctx);
            case "CALENDARIO_RECEPCION" -> transferirDesdeCalendario(ctx);
            case "CALENDARIO_MENU" -> fin(ctx, soporte().menuTexto());
            default -> null;
        };
    }

    /** Conserva el punto del proceso y el motivo antes de transferir (§13). */
    private PasoResultado transferirDesdeCalendario(ConversacionContexto ctx) {
        if (ctx.tieneFlujoActivo() && !HumanTransferFlow.CODIGO.equals(ctx.getFlujo())) {
            ctx.setFlujoAnteriorTransferencia(ctx.getFlujo());
        }
        if (ctx.getMotivoTransferencia() == null || ctx.getMotivoTransferencia().isBlank()) {
            ctx.setMotivoTransferencia("No pudo abrirse el calendario de disponibilidad");
        }
        ctx.setFlujo(HumanTransferFlow.CODIGO);
        ctx.setPaso(null);
        return PasoResultado.reenviar();
    }

    // ------------------------------------------------------------------
    // Fecha (§8)
    // ------------------------------------------------------------------

    protected PasoResultado pedirFecha(ConversacionContexto ctx, String error) {
        StringBuilder sb = new StringBuilder();
        if (error != null) {
            sb.append(error).append("\n\n");
        }
        sb.append("¿Para qué día quieres consultar?\n")
                .append("Puede ser 'hoy', 'mañana', 'lunes' o '20/10'.");
        return pregunta(ctx, sb.toString(), PASO_FECHA, MenuActual.FECHAS,
                new ArrayList<>(), "DATE");
    }

    protected PasoResultado fechaElegida(ConversacionContexto ctx, EntradaFlujo entrada) {
        LocalDate fecha = entrada.entidades().fecha();
        if (fecha == null || fecha.isBefore(soporte().hoy())) {
            return repetir(ctx, "No entendí esa fecha. Escribe 'mañana', 'lunes' o '20/10' (a partir de hoy).");
        }
        ctx.setFecha(fecha);
        ctx.setSlotId(null);
        return pedirFranja(ctx);
    }

    // ------------------------------------------------------------------
    // Franja (§8): evita listas interminables de horas
    // ------------------------------------------------------------------

    protected PasoResultado pedirFranja(ConversacionContexto ctx) {
        List<OpcionMenu> opciones = List.of(
                OpcionMenu.franja(1, "MANANA", "Mañana"),
                OpcionMenu.franja(2, "MEDIODIA", "Mediodía"),
                OpcionMenu.franja(3, "TARDE", "Tarde"),
                OpcionMenu.franja(4, "TODAS", "Ver todos"));
        String texto = "¿Qué horario prefieres?\n"
                + "1. 🌅 Mañana\n2. ☀️ Mediodía\n3. 🌤️ Tarde\n4. Ver todos";
        return pregunta(ctx, texto, PASO_FRANJA, MenuActual.FRANJAS, opciones);
    }

    protected PasoResultado franjaElegida(ConversacionContexto ctx, EntradaFlujo entrada) {
        OpcionMenu opcion = entrada.opcion();
        String franja = opcion != null && opcion.tipo() == TipoOpcion.FRANJA
                ? opcion.valor()
                : entrada.entidades().franja();
        if (franja == null) {
            if (entrada.tieneOpcion()) {
                return repetir(ctx, "Elige 1 Mañana, 2 Mediodía, 3 Tarde o 4 Ver todos.");
            }
            return repetir(ctx, "No entendí esa franja. Escribe 'mañana', 'mediodía' o 'tarde'.");
        }
        ctx.setFranja(franja);
        return mostrarSlots(ctx, null);
    }

    // ------------------------------------------------------------------
    // Slots reales (§8 y §19)
    // ------------------------------------------------------------------

    protected PasoResultado mostrarSlots(ConversacionContexto ctx, String error) {
        List<AvailabilityTool.SlotConDoctor> todos = slotsDelDia(ctx);
        if (todos.isEmpty()) {
            return sinCupos(ctx);
        }
        List<AvailabilityTool.SlotConDoctor> filtrados = filtrar(todos, ctx.getFranja());
        boolean franjaVacia = filtrados.isEmpty();
        if (franjaVacia) {
            filtrados = todos;
            ctx.setFranja("TODAS");
        }
        List<OpcionMenu> opciones = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        if (error != null) {
            sb.append(error).append("\n\n");
        }
        if (franjaVacia) {
            sb.append("No hay cupos en esa franja, te muestro todos los disponibles:\n");
        } else {
            sb.append("Tengo disponibles:\n");
        }
        for (int i = 0; i < filtrados.size(); i++) {
            AvailabilityTool.SlotConDoctor slot = filtrados.get(i);
            String etiqueta = slot.hora().toString()
                    + (ctx.getOdontologoId() == null ? " — " + slot.doctorNombre() : "");
            opciones.add(OpcionMenu.hora(i + 1, slot.hora().toString(), etiqueta));
            sb.append(i + 1).append(". ").append(etiqueta).append("\n");
        }
        sb.append("¿Quieres reservar alguno?");
        return pregunta(ctx, sb.toString(), PASO_SLOTS, MenuActual.HORAS, opciones, "HORA");
    }

    protected PasoResultado sinCupos(ConversacionContexto ctx) {
        String texto = "No encuentro cupos el " + soporte().formatearFecha(ctx.getFecha())
                + " con " + resumenOdontologo(ctx) + ". 😕\n¿Qué prefieres hacer?";
        ctx.setPaso(PASO_SIN_CUPOS);
        ctx.setMenuActual(MenuActual.HORAS);
        ctx.setOpciones(opcionesSinCupos());
        ctx.setExpectedInput("OPTION");
        ctx.setUltimaPregunta(texto);
        return PasoResultado.continuar(texto);
    }

    /** Elige un cupo: opción de la lista, número fuera de rango (§14) o hora escrita (§6). */
    protected PasoResultado horaElegida(Conversacion conversacion, ConversacionContexto ctx, EntradaFlujo entrada) {
        OpcionMenu opcion = entrada.opcion();
        String hora = null;
        if (opcion != null && opcion.tipo() == TipoOpcion.HORA) {
            hora = opcion.valor();
        } else if (opcion != null && opcion.tipo() == TipoOpcion.ACCION) {
            return accionSinCupos(ctx, opcion);
        } else if (entrada.numero() != null) {
            // Fuera del rango de la lista: se interpreta como hora ("10" = 10:00)
            // sólo si ese cupo está listado; si no, error contextual (§14).
            hora = horaListada(ctx, entrada.numero());
            if (hora == null) {
                return repetir(ctx, "La opción " + entrada.numero() + " no existe.");
            }
        } else if (entrada.entidades().hora() != null) {
            hora = entrada.entidades().hora().toString();
        }
        if (hora == null) {
            return repetir(ctx, "Elige un cupo de la lista o escríbeme la hora con dos puntos (ej: 10:00).");
        }
        final String horaElegida = hora;
        AvailabilityTool.SlotConDoctor elegido = slotsDelDia(ctx).stream()
                .filter(s -> s.hora().toString().equals(horaElegida))
                .findFirst()
                .orElse(null);
        if (elegido == null) {
            return mostrarSlots(ctx, "Ese cupo ya no está disponible. Actualicé la lista:");
        }
        ctx.setSlotId(elegido.hora().toString());
        if (ctx.getOdontologoId() == null) {
            ctx.setOdontologoId(elegido.doctorId());
            ctx.setOdontologoLibre(elegido.doctorNombre());
        }
        return trasCupo(conversacion, ctx, entrada);
    }

    /** Continúa después de elegir cupo: cada flujo decide si reservar (§9). */
    protected PasoResultado trasCupo(Conversacion conversacion, ConversacionContexto ctx,
                                     EntradaFlujo entrada) {
        return repetir(ctx, "Elige un cupo de la lista.");
    }

    private String horaListada(ConversacionContexto ctx, int numero) {
        if (numero < 0 || numero > 23 || ctx.getOpciones() == null) {
            return null;
        }
        String[] candidatas = {String.format("%02d:00", numero), String.format("%02d:30", numero)};
        for (String candidata : candidatas) {
            for (OpcionMenu o : ctx.getOpciones()) {
                if (o.tipo() == TipoOpcion.HORA && candidata.equals(o.valor())) {
                    return candidata;
                }
            }
        }
        return null;
    }

    protected List<AvailabilityTool.SlotConDoctor> slotsDelDia(ConversacionContexto ctx) {
        if (ctx.getFecha() == null) {
            return List.of();
        }
        if (ctx.getOdontologoId() == null) {
            return e.disponibilidad.slotsCualquierOdontologo(ctx.getFecha(), ctx.getServicioId());
        }
        return e.disponibilidad.slots(ctx.getOdontologoId(), ctx.getFecha(), ctx.getServicioId())
                .stream()
                .map(s -> new AvailabilityTool.SlotConDoctor(s.horaInicio(), ctx.getOdontologoId(),
                        e.odontologos.nombre(ctx.getOdontologoId())))
                .toList();
    }

    protected List<AvailabilityTool.SlotConDoctor> filtrar(List<AvailabilityTool.SlotConDoctor> slots,
                                                           String franja) {
        if (franja == null || "TODAS".equals(franja)) {
            return slots;
        }
        return slots.stream().filter(s -> e.disponibilidad.enFranja(s.hora(), franja)).toList();
    }

    protected String resumenOdontologo(ConversacionContexto ctx) {
        return ctx.getOdontologoId() == null
                ? "cualquier odontólogo disponible"
                : e.odontologos.nombre(ctx.getOdontologoId());
    }

    protected String nombreServicio(ConversacionContexto ctx) {
        if (ctx.getServicioId() != null) {
            return e.servicios.nombre(ctx.getServicioId());
        }
        return ctx.getServicioLibre() == null ? "una valoración general" : ctx.getServicioLibre();
    }

    /** Opciones de "no hay cupos": salida contextual, nunca un error genérico (§14). */
    protected List<OpcionMenu> opcionesSinCupos() {
        return List.of(
                OpcionMenu.accion(1, "MANANA", "Probar mañana"),
                OpcionMenu.accion(2, "OTRO_DIA", "Elegir otro día"),
                OpcionMenu.accion(3, "OTRO_DOCTOR", "Cambiar de odontólogo"),
                OpcionMenu.accion(4, "MENU", "Volver al menú"));
    }

    protected PasoResultado accionSinCupos(ConversacionContexto ctx, OpcionMenu opcion) {
        if (opcion == null || opcion.tipo() != TipoOpcion.ACCION) {
            return null;
        }
        return switch (opcion.valor()) {
            case "MANANA" -> {
                ctx.setFecha(soporte().hoy().plusDays(1));
                ctx.setSlotId(null);
                yield pedirFranja(ctx);
            }
            case "OTRO_DIA" -> pedirFecha(ctx, null);
            case "OTRO_DOCTOR" -> {
                ctx.setOdontologoId(null);
                ctx.setOdontologoResuelto(false);
                yield pedirOdontologo(ctx, null);
            }
            case "MENU" -> fin(ctx, soporte().menuTexto());
            default -> null;
        };
    }

    // ------------------------------------------------------------------
    // Correcciones (§12): cambia sólo el dato afectado y limpia dependencias
    // ------------------------------------------------------------------

    protected PasoResultado aplicarCorreccion(ConversacionContexto ctx, EntradaFlujo entrada) {
        EntityExtractor.Correccion correccion = entrada.correccion();
        if (correccion == null) {
            return null;
        }
        switch (correccion.tipo()) {
            case FECHA -> {
                LocalDate fecha = ParserAgendamiento.fechaDelTexto(
                        AgenteSoporte.compactar(correccion.texto()), soporte().hoy()).orElse(null);
                if (fecha == null) {
                    fecha = entrada.entidades().fecha();
                }
                if (fecha == null || fecha.isBefore(soporte().hoy())) {
                    return null;
                }
                ctx.setFecha(fecha);
                ctx.setSlotId(null);
            }
            case FRANJA -> {
                String texto = AgenteSoporte.compactar(correccion.texto());
                String franja = texto.contains("manana") ? "MANANA"
                        : texto.contains("tarde") ? "TARDE" : "MEDIODIA";
                ctx.setFranja(franja);
                ctx.setSlotId(null);
            }
            case ODONTOLOGO -> {
                ctx.setOdontologoId(null);
                ctx.setOdontologoResuelto(false);
                ctx.setOdontologoLibre(null);
                ctx.setSlotId(null);
            }
            case SERVICIO -> {
                Long servicioId = ParserAgendamiento.servicioDelTexto(
                        AgenteSoporte.compactar(correccion.texto()), e.servicios.activos()).orElse(null);
                if (servicioId == null) {
                    ctx.setServicioLibre(correccion.texto());
                    ctx.setServicioId(null);
                } else {
                    ctx.setServicioId(servicioId);
                    ctx.setServicioLibre(null);
                }
                ctx.setServicioResuelto(true);
                ctx.setSlotId(null);
            }
            case HORA -> ctx.setSlotId(null);
            case PACIENTE, DATO -> {
                return null;
            }
            default -> {
                return null;
            }
        }
        return continuarTrasCorreccion(ctx);
    }

    private PasoResultado continuarTrasCorreccion(ConversacionContexto ctx) {
        if (!ctx.isOdontologoResuelto()) {
            return pedirOdontologo(ctx, null);
        }
        if (ctx.getFecha() == null && ctx.getSlotId() == null && !ctx.isModalidadResuelta()) {
            return pedirModalidad(ctx, null);
        }
        if (!ctx.isServicioResuelto()) {
            return pedirServicio(ctx, null);
        }
        if (ctx.getFecha() == null) {
            return pedirFecha(ctx, null);
        }
        if (ctx.getFranja() == null) {
            return pedirFranja(ctx);
        }
        return mostrarSlots(ctx, null);
    }

    /** Precarga lo que el usuario ya escribió en un solo mensaje (§10). */
    protected void precargarEntidades(ConversacionContexto ctx, EntradaFlujo entrada) {
        EntityExtractor.Entidades entidades = entrada.entidades();
        if (!ctx.isServicioResuelto() && entidades.servicioId() != null) {
            ctx.setServicioId(entidades.servicioId());
            ctx.setServicioResuelto(true);
        }
        if (!ctx.isOdontologoResuelto() && entidades.odontologoId() != null) {
            ctx.setOdontologoId(entidades.odontologoId());
            ctx.setOdontologoResuelto(true);
        }
        if (ctx.getFecha() == null && entidades.fecha() != null) {
            ctx.setFecha(entidades.fecha());
        }
        if (ctx.getFranja() == null && entidades.franja() != null) {
            ctx.setFranja(entidades.franja());
        }
        if (ctx.getSlotId() == null && entidades.hora() != null) {
            ctx.setSlotId(entidades.hora().toString());
        }
    }

    /** Siguiente paso pendiente (§9 del plan): odontólogo → modalidad → servicio → fecha. */
    protected PasoResultado siguientePendiente(ConversacionContexto ctx) {
        if (!ctx.isOdontologoResuelto()) {
            return pedirOdontologo(ctx, null);
        }
        if (ctx.getFecha() == null && ctx.getSlotId() == null && !ctx.isModalidadResuelta()) {
            return pedirModalidad(ctx, null);
        }
        if (!ctx.isServicioResuelto()) {
            return pedirServicio(ctx, null);
        }
        if (ctx.getFecha() == null) {
            return pedirFecha(ctx, null);
        }
        if (ctx.getFranja() == null) {
            return pedirFranja(ctx);
        }
        if (ctx.getSlotId() == null) {
            return mostrarSlots(ctx, null);
        }
        return null;
    }
}
