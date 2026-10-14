package org.dentalcrm.web.agente.flujo;

import org.dentalcrm.web.agente.AgenteSoporte;
import org.dentalcrm.web.agente.contexto.ContextManager;
import org.dentalcrm.web.agente.engine.ConfirmationResolver;
import org.dentalcrm.web.agente.engine.EntityExtractor;
import org.dentalcrm.web.agente.engine.EntradaFlujo;
import org.dentalcrm.web.agente.engine.MenuResolver;
import org.dentalcrm.web.agente.engine.PasoResultado;
import org.dentalcrm.web.agenda.publica.AgendaPublicaService;
import org.dentalcrm.web.agente.tool.AppointmentTool;
import org.dentalcrm.web.agente.tool.AvailabilityTool;
import org.dentalcrm.web.agente.tool.DentistTool;
import org.dentalcrm.web.agente.tool.PatientTool;
import org.dentalcrm.web.agente.tool.ServiceTool;
import org.dentalcrm.domain.whatsapp.Conversacion;

/**
 * Dependencias compartidas por todos los flujos y por el motor.
 */
public class AgenteEntorno {

    public final AgenteSoporte soporte;
    public final ContextManager contexto;
    public final PatientTool pacientes;
    public final ServiceTool servicios;
    public final DentistTool odontologos;
    public final AvailabilityTool disponibilidad;
    public final AppointmentTool citas;
    public final AgendaPublicaService agendaPublica;
    public final EntityExtractor extractor;
    public final ConfirmationResolver confirmaciones;
    public final MenuResolver menus;

    private final ThreadLocal<Conversacion> conversacionEnCurso = new ThreadLocal<>();

    public AgenteEntorno(AgenteSoporte soporte, ContextManager contexto, PatientTool pacientes,
                         ServiceTool servicios, DentistTool odontologos, AvailabilityTool disponibilidad,
                         AppointmentTool citas, EntityExtractor extractor,
                         ConfirmationResolver confirmaciones, MenuResolver menus) {
        this(soporte, contexto, pacientes, servicios, odontologos, disponibilidad, citas,
                extractor, confirmaciones, menus, null);
    }

    public AgenteEntorno(AgenteSoporte soporte, ContextManager contexto, PatientTool pacientes,
                         ServiceTool servicios, DentistTool odontologos, AvailabilityTool disponibilidad,
                         AppointmentTool citas, EntityExtractor extractor,
                         ConfirmationResolver confirmaciones, MenuResolver menus,
                         AgendaPublicaService agendaPublica) {
        this.soporte = soporte;
        this.contexto = contexto;
        this.pacientes = pacientes;
        this.servicios = servicios;
        this.odontologos = odontologos;
        this.disponibilidad = disponibilidad;
        this.citas = citas;
        this.agendaPublica = agendaPublica;
        this.extractor = extractor;
        this.confirmaciones = confirmaciones;
        this.menus = menus;
    }

    /** Conversación que se está procesando en este hilo (los flujos la usan como respaldo). */
    public void entrarEnConversacion(Conversacion conversacion) {
        if (conversacion == null) {
            conversacionEnCurso.remove();
        } else {
            conversacionEnCurso.set(conversacion);
        }
    }

    public void salirDeConversacion() {
        conversacionEnCurso.remove();
    }

    public Conversacion conversacionActual() {
        return conversacionEnCurso.get();
    }
}
