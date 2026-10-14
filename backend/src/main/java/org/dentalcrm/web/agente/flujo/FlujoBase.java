package org.dentalcrm.web.agente.flujo;

import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.whatsapp.Conversacion;
import org.dentalcrm.web.agente.AgenteSoporte;
import org.dentalcrm.web.agente.contexto.ConversacionContexto;
import org.dentalcrm.web.agente.contexto.MenuActual;
import org.dentalcrm.web.agente.contexto.OpcionMenu;
import org.dentalcrm.web.agente.contexto.TipoOpcion;
import org.dentalcrm.web.agente.engine.EntradaFlujo;
import org.dentalcrm.web.agente.engine.PasoResultado;

import java.util.ArrayList;
import java.util.List;

/**
 * Rutinas compartidas por los flujos: transición de pasos, menú principal,
 * identificación del paciente (§7) y confirmaciones (§11).
 */
public abstract class FlujoBase implements Flujo {

    protected final AgenteEntorno e;

    protected FlujoBase(AgenteEntorno e) {
        this.e = e;
    }

    protected AgenteSoporte soporte() {
        return e.soporte;
    }

    // ------------------------------------------------------------------
    // Transiciones
    // ------------------------------------------------------------------

    /** Deja el flujo en el paso indicado y devuelve la pregunta al usuario. */
    protected PasoResultado pregunta(ConversacionContexto ctx, String mensaje, String paso,
                                     MenuActual menu, List<OpcionMenu> opciones, String esperado) {
        ctx.setFlujo(codigo());
        ctx.setPaso(paso);
        ctx.setMenuActual(menu);
        ctx.setOpciones(opciones == null ? new ArrayList<>() : new ArrayList<>(opciones));
        ctx.setExpectedInput(esperado);
        ctx.setUltimaPregunta(mensaje);
        ctx.setIntentosFallidos(0);
        return PasoResultado.continuar(mensaje);
    }

    protected PasoResultado pregunta(ConversacionContexto ctx, String mensaje, String paso,
                                     MenuActual menu, List<OpcionMenu> opciones) {
        return pregunta(ctx, mensaje, paso, menu, opciones, "OPTION");
    }

    /** Termina el flujo y devuelve el control al menú principal (§5). */
    protected PasoResultado fin(ConversacionContexto ctx, String mensaje) {
        ctx.setFlujo(null);
        ctx.setPaso(null);
        ctx.setExpectedInput(null);
        ctx.setSlotId(null);
        ctx.setReanudando(false);
        ctx.setFlujoRetorno(null);
        ctx.setPasoRetorno(null);
        menuPrincipal(ctx);
        ctx.setUltimaPregunta(mensaje);
        return PasoResultado.fin(mensaje);
    }

    protected void menuPrincipal(ConversacionContexto ctx) {
        ctx.setMenuActual(MenuActual.PRINCIPAL);
        ctx.setOpciones(soporte().opcionesPrincipales());
        ctx.setExpectedInput("OPTION");
    }

    /** Repite la última pregunta tras un error (§14). */
    protected PasoResultado repetir(ConversacionContexto ctx, String error) {
        ctx.setIntentosFallidos(ctx.getIntentosFallidos() + 1);
        StringBuilder sb = new StringBuilder();
        if (error != null && !error.isBlank()) {
            sb.append(error).append("\n\n");
        }
        sb.append(ctx.getUltimaPregunta() == null ? "¿Puedes intentarlo de nuevo?" : ctx.getUltimaPregunta());
        if (ctx.getIntentosFallidos() >= 3) {
            sb.append("\n\nSi prefieres, escribe 'humano' y te atiende una persona.");
        }
        return PasoResultado.continuar(sb.toString()).fallo();
    }

    // ------------------------------------------------------------------
    // Identificación (§7)
    // ------------------------------------------------------------------

    /**
     * Ofrece identificar al paciente por cédula (§2) si el flujo lo requiere y
     * aún no hay identidad vigente.
     *
     * @return la pregunta de identificación, o {@code null} si ya está identificado
     */
    protected PasoResultado pedirIdentificacion(Conversacion conv, ConversacionContexto ctx, String mensaje) {
        if (ctx.getPacienteId() != null) {
            return null;
        }
        if (conv.getPaciente() != null) {
            ctx.setPacienteId(conv.getPaciente().getId());
            return null;
        }
        if (e.contexto.identidadValida(ctx)) {
            return null;
        }
        if (ctx.getPasoRetorno() == null) {
            ctx.setFlujoRetorno(ctx.getFlujo());
            ctx.setPasoRetorno(ctx.getPaso());
        }
        return PatientIdentificationFlow.pedirCedulaEn(ctx, ctx.getFlujoRetorno(), ctx.getPasoRetorno());
    }

    /**
     * Paciente del contexto, con la ficha guardada en la conversación como
     * respaldo (evita otra consulta cuando la conversación ya lo conoce).
     */
    protected Paciente pacienteActual(ConversacionContexto ctx) {
        Long id = ctx.getPacienteId();
        if (id != null) {
            Paciente porId = e.pacientes.porId(id).orElse(null);
            if (porId != null) {
                return porId;
            }
        }
        Conversacion conv = e.conversacionActual();
        Paciente guardado = conv == null ? null : conv.getPaciente();
        if (guardado != null && (id == null || id.equals(guardado.getId()))) {
            return guardado;
        }
        return null;
    }

    protected boolean esPasoIdentificacion(ConversacionContexto ctx) {
        return PatientIdentificationFlow.PASO_CEDULA.equals(ctx.getPaso())
                || PatientIdentificationFlow.PASO_NO_ENCONTRADO.equals(ctx.getPaso());
    }

    protected PasoResultado iniciarRegistro(ConversacionContexto ctx) {
        ctx.setFlujo("REGISTRAR_PACIENTE");
        ctx.setPaso("REG_NOMBRES");
        ctx.setMenuActual(MenuActual.REGISTRO);
        ctx.setOpciones(new ArrayList<>());
        ctx.setExpectedInput("TEXT");
        ctx.setUltimaPregunta(AgenteSoporte.MENSAJE_PREGUNTA_NOMBRE);
        return PasoResultado.continuar(AgenteSoporte.MENSAJE_PREGUNTA_NOMBRE);
    }

    protected boolean contieneRegistro(EntradaFlujo entrada) {
        String t = entrada.texto();
        return t.contains("soy nuevo") || t.contains("registrarme") || t.contains("registrar");
    }

    /** Tras registrar, vuelve al flujo que pidió identificar o termina (§7). */
    protected boolean retomar(ConversacionContexto ctx) {
        String flujo = ctx.getFlujoRetorno();
        if (flujo == null || flujo.isBlank()) {
            return false;
        }
        ctx.setFlujo(flujo);
        ctx.setFlujoRetorno(null);
        ctx.setPaso(ctx.getPasoRetorno());
        ctx.setPasoRetorno(null);
        return true;
    }
}
