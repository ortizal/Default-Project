package org.dentalcrm.web.agente.flujo;

import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.whatsapp.Conversacion;
import org.dentalcrm.web.agente.AgenteSoporte;
import org.dentalcrm.web.agente.contexto.ConversacionContexto;
import org.dentalcrm.web.agente.contexto.MenuActual;
import org.dentalcrm.web.agente.contexto.OpcionMenu;
import org.dentalcrm.web.agente.engine.EntradaFlujo;
import org.dentalcrm.web.agente.engine.PasoResultado;

import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Registra un paciente desde el chat, incluyendo tutor si es menor (§7).
 *
 * <p>Al terminar retoma el flujo que pidió identificar (agendar, ver citas...)
 * o vuelve al menú si el registro fue espontáneo.
 */
public class RegisterPatientFlow extends FlujoBase {

    public static final String CODIGO = "REGISTRAR_PACIENTE";

    public RegisterPatientFlow(AgenteEntorno e) {
        super(e);
    }

    @Override
    public String codigo() {
        return CODIGO;
    }

    @Override
    public PasoResultado responder(Conversacion conversacion, ConversacionContexto ctx, EntradaFlujo entrada) {
        String paso = ctx.getPaso();
        if (paso == null || paso.isBlank()) {
            ctx.setFlujo(CODIGO);
            ctx.setMenuActual(MenuActual.REGISTRO);
            ctx.setOpciones(new ArrayList<>());
            if (entrada.esCedula() || entrada.entidades().cedula() != null) {
                ctx.setCedula(entrada.entidades().cedula() != null
                        ? entrada.entidades().cedula() : entrada.texto());
                ctx.setPaso("REG_CONFIRMAR");
                return pregunta(ctx, "No encontré ningún paciente con cédula " + ctx.getCedula()
                        + ".\n¿Quieres que cree tu ficha de paciente? Responde 'sí'.",
                        "REG_CONFIRMAR", MenuActual.REGISTRO, new ArrayList<>(), "CONFIRMACION");
            }
            ctx.setPaso("REG_NOMBRES");
            return pregunta(ctx, AgenteSoporte.MENSAJE_PREGUNTA_NOMBRE,
                    "REG_NOMBRES", MenuActual.REGISTRO, new ArrayList<>(), "TEXT");
        }
        return switch (paso) {
            case "REG_CONFIRMAR" -> confirmarRegistro(ctx, entrada);
            case "REG_NOMBRES" -> nombres(ctx, entrada);
            case "REG_APELLIDOS" -> apellidos(ctx, entrada);
            case "REG_FECHA" -> fecha(ctx, entrada);
            case "REG_TUTOR" -> parentesco(ctx, entrada);
            case "REG_TUTOR_NOMBRE" -> tutorNombre(ctx, entrada);
            case "REG_TUTOR_APELLIDO" -> tutorApellidos(ctx, entrada);
            case "REG_CONFIRMAR_DATOS" -> confirmarDatos(conversacion, ctx, entrada);
            default -> pregunta(ctx, AgenteSoporte.MENSAJE_PREGUNTA_NOMBRE,
                    "REG_NOMBRES", MenuActual.REGISTRO, new ArrayList<>(), "TEXT");
        };
    }

    private PasoResultado confirmarRegistro(ConversacionContexto ctx, EntradaFlujo entrada) {
        if (entrada.esConfirmacion()) {
            ctx.setPaso("REG_NOMBRES");
            return pregunta(ctx, AgenteSoporte.MENSAJE_PREGUNTA_NOMBRE,
                    "REG_NOMBRES", MenuActual.REGISTRO, new ArrayList<>(), "TEXT");
        }
        if (entrada.esRechazo()) {
            return abandonar(ctx);
        }
        return repetir(ctx, "¿Confirmas tu registro? Responde 'sí' para crear tu ficha o 'no' para cancelar.");
    }

    private PasoResultado nombres(ConversacionContexto ctx, EntradaFlujo entrada) {
        String texto = entrada.crudo() == null ? "" : entrada.crudo().trim();
        if (entrada.texto().length() < 2) {
            return repetir(ctx, AgenteSoporte.MENSAJE_PREGUNTA_NOMBRE);
        }
        ctx.setNombres(texto);
        return pregunta(ctx, AgenteSoporte.MENSAJE_PREGUNTA_APELLIDOS,
                "REG_APELLIDOS", MenuActual.REGISTRO, new ArrayList<>(), "TEXT");
    }

    private PasoResultado apellidos(ConversacionContexto ctx, EntradaFlujo entrada) {
        String texto = entrada.crudo() == null ? "" : entrada.crudo().trim();
        if (entrada.texto().length() < 2) {
            return repetir(ctx, AgenteSoporte.MENSAJE_PREGUNTA_APELLIDOS);
        }
        ctx.setApellidos(texto);
        return pregunta(ctx, AgenteSoporte.MENSAJE_PREGUNTA_FECHA,
                "REG_FECHA", MenuActual.REGISTRO, new ArrayList<>(), "DATE");
    }

    private PasoResultado fecha(ConversacionContexto ctx, EntradaFlujo entrada) {
        LocalDate fecha = soporte().parsearFechaLibre(entrada.crudo());
        if (fecha == null || fecha.isAfter(soporte().hoy())) {
            return repetir(ctx, "No entendí esa fecha 😅 " + AgenteSoporte.MENSAJE_PREGUNTA_FECHA);
        }
        ctx.setFechaNacimiento(fecha);
        if (soporte().esMenor(fecha)) {
            return pregunta(ctx, AgenteSoporte.MENSAJE_PREGUNTA_TUTOR,
                    "REG_TUTOR", MenuActual.REGISTRO, new ArrayList<>(), "TEXT");
        }
        return resumenRegistro(ctx);
    }

    private PasoResultado parentesco(ConversacionContexto ctx, EntradaFlujo entrada) {
        String parentesco = soporte().parsearParentesco(entrada.crudo());
        if (parentesco == null) {
            return repetir(ctx, AgenteSoporte.MENSAJE_PREGUNTA_TUTOR);
        }
        ctx.setParentesco(parentesco);
        return pregunta(ctx, "Perfecto. ¿Cuál es el nombre del " + parentesco.toLowerCase() + "?",
                "REG_TUTOR_NOMBRE", MenuActual.REGISTRO, new ArrayList<>(), "TEXT");
    }

    private PasoResultado tutorNombre(ConversacionContexto ctx, EntradaFlujo entrada) {
        String texto = entrada.crudo() == null ? "" : entrada.crudo().trim();
        if (entrada.texto().length() < 2) {
            return repetir(ctx, "¿Cuál es el nombre del tutor?");
        }
        ctx.setTutorNombre(texto);
        return pregunta(ctx, "¿Y los apellidos del tutor?",
                "REG_TUTOR_APELLIDO", MenuActual.REGISTRO, new ArrayList<>(), "TEXT");
    }

    private PasoResultado tutorApellidos(ConversacionContexto ctx, EntradaFlujo entrada) {
        String texto = entrada.crudo() == null ? "" : entrada.crudo().trim();
        if (!texto.isEmpty() && !esConfirmacionDeResumen(entrada)) {
            ctx.setTutorApellidos(texto);
        }
        return resumenRegistro(ctx);
    }

    private boolean esConfirmacionDeResumen(EntradaFlujo entrada) {
        return entrada.esConfirmacion();
    }

    private PasoResultado resumenRegistro(ConversacionContexto ctx) {
        StringBuilder sb = new StringBuilder("Revisa tus datos:\n");
        sb.append("• Nombre: ").append(ctx.getNombres()).append(" ").append(ctx.getApellidos()).append("\n");
        if (ctx.getCedula() != null && !ctx.getCedula().isBlank()) {
            sb.append("• Cédula: ").append(ctx.getCedula()).append("\n");
        }
        sb.append("• Fecha de nacimiento: ").append(soporte().formatoFechaLatina(ctx.getFechaNacimiento())).append("\n");
        if (ctx.getTutorNombre() != null) {
            sb.append("• Tutor: ").append(ctx.getParentesco()).append(" ").append(ctx.getTutorNombre());
            if (ctx.getTutorApellidos() != null && !ctx.getTutorApellidos().isBlank()) {
                sb.append(" ").append(ctx.getTutorApellidos());
            }
            sb.append("\n");
        }
        sb.append("\n¿Confirmas estos datos? Responde 'sí' para guardarlos.");
        return pregunta(ctx, sb.toString(), "REG_CONFIRMAR_DATOS",
                MenuActual.REGISTRO, new ArrayList<>(), "CONFIRMACION");
    }

    private PasoResultado confirmarDatos(Conversacion conversacion, ConversacionContexto ctx,
                                         EntradaFlujo entrada) {
        if (entrada.esRechazo()) {
            return abandonar(ctx);
        }
        if (!entrada.esConfirmacion()) {
            return repetir(ctx, "¿Confirmas estos datos? Responde 'sí' para guardarlos.");
        }
        try {
            Paciente creado = e.pacientes.crearDesdeChat(conversacion.getTelefono(),
                    ctx.getCedula(), ctx.getNombres(), ctx.getApellidos(), ctx.getFechaNacimiento(),
                    ctx.getParentesco(), ctx.getTutorNombre(), ctx.getTutorApellidos());
            conversacion.setPaciente(creado);
            ctx.setPacienteId(creado.getId());
            ctx.setNombrePaciente(e.pacientes.nombre(creado));
            ctx.setIdentidadVerificada(true);
            ctx.setIdentidadVerificadaEn(java.time.Instant.now());
            boolean menor = soporte().esMenor(ctx.getFechaNacimiento());
            String mensaje = "¡Listo, " + ctx.getNombres() + "! ✅ Te registré como paciente."
                    + (menor ? "\nComo eres menor de edad, también quedó registrado el tutor ("
                    + ctx.getParentesco() + " " + ctx.getTutorNombre() + ")." : "");
            if (retomar(ctx)) {
                return PasoResultado.reenviar(mensaje);
            }
            return fin(ctx, mensaje + "\n\n" + soporte().menuTexto());
        } catch (Exception ex) {
            return fin(ctx, "No pude registrarte: " + (ex.getMessage() == null ? "error interno" : ex.getMessage())
                    + "\nPuedes intentarlo de nuevo o escribe 'humano'.");
        }
    }

    private PasoResultado abandonar(ConversacionContexto ctx) {
        if (retomar(ctx)) {
            return PasoResultado.reenviar();
        }
        return fin(ctx, "Está bien, no registré nada. Si cambias de opinión, escríbeme 'registrarme' 😊");
    }
}
