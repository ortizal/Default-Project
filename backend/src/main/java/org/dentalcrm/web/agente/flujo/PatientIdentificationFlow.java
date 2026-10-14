package org.dentalcrm.web.agente.flujo;

import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.whatsapp.Conversacion;
import org.dentalcrm.web.agente.contexto.ConversacionContexto;
import org.dentalcrm.web.agente.contexto.MenuActual;
import org.dentalcrm.web.agente.contexto.OpcionMenu;
import org.dentalcrm.web.agente.engine.EntradaFlujo;
import org.dentalcrm.web.agente.engine.PasoResultado;

import java.util.ArrayList;
import java.util.List;

/**
 * Identificación por cédula reutilizable (§2): suspende el flujo que la pidió
 * (guardando su código y paso), verifica al paciente y lo retoma exactamente
 * donde quedó.
 *
 * <p>La identidad verificada queda guardada en el contexto y se reutiliza
 * durante una sesión corta (§9), para no pedir la cédula en cada operación
 * privada.
 */
public class PatientIdentificationFlow extends FlujoBase {

    public static final String CODIGO = "IDENTIFICAR_PACIENTE";
    public static final String PASO_CEDULA = "SOLICITAR_CEDULA";
    public static final String PASO_NO_ENCONTRADO = "CEDULA_NO_ENCONTRADA";

    public PatientIdentificationFlow(AgenteEntorno e) {
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
            return pedirCedula(ctx, null);
        }
        if (PASO_NO_ENCONTRADO.equals(paso)) {
            return accionDeNoEncontrado(conversacion, ctx, entrada);
        }
        return verificarCedula(conversacion, ctx, entrada);
    }

    // ------------------------------------------------------------------
    // Solicitud de cédula (la usan el motor y los demás flujos)
    // ------------------------------------------------------------------

    /** Mensaje de solicitud según el flujo que la pidió (§2, §6). */
    public static String mensajeDeSolicitud(String codigoOrigen) {
        if ("AGENDAR_CITA".equals(codigoOrigen)) {
            return "Para agendar una cita primero necesito identificar al paciente.\n"
                    + "Por favor, envíame su número de cédula.";
        }
        if (codigoOrigen != null) {
            return "Para proteger tu información necesito verificar primero al paciente.\n"
                    + "Envíame su número de cédula.";
        }
        return "Para continuar necesito identificar al paciente.\nEnvíame su número de cédula.";
    }

    /** Suspende el flujo de origen y deja al paciente en la espera de la cédula (§2). */
    public static PasoResultado pedirCedulaEn(ConversacionContexto ctx, String flujoOrigen,
                                              String pasoOrigen) {
        return pedirCedulaEn(ctx, flujoOrigen, pasoOrigen, mensajeDeSolicitud(flujoOrigen));
    }

    public static PasoResultado pedirCedulaEn(ConversacionContexto ctx, String flujoOrigen,
                                              String pasoOrigen, String mensaje) {
        ctx.setFlujo(CODIGO);
        ctx.setPaso(PASO_CEDULA);
        ctx.setFlujoRetorno(flujoOrigen);
        ctx.setPasoRetorno(pasoOrigen);
        ctx.setMenuActual(MenuActual.REGISTRO);
        ctx.setOpciones(new ArrayList<>());
        ctx.setExpectedInput("CEDULA");
        ctx.setRequiereIdentificacion(true);
        String texto = mensaje == null || mensaje.isBlank() ? mensajeDeSolicitud(flujoOrigen) : mensaje;
        ctx.setUltimaPregunta(texto);
        return PasoResultado.continuar(texto);
    }

    // ------------------------------------------------------------------
    // Pasos
    // ------------------------------------------------------------------

    private PasoResultado pedirCedula(ConversacionContexto ctx, String error) {
        String mensaje = error == null || error.isBlank() ? mensajeDeSolicitud(ctx.getFlujoRetorno()) : error;
        ctx.setPaso(PASO_CEDULA);
        ctx.setMenuActual(MenuActual.REGISTRO);
        ctx.setOpciones(new ArrayList<>());
        ctx.setExpectedInput("CEDULA");
        ctx.setRequiereIdentificacion(true);
        String texto = error == null || error.isBlank()
                ? mensaje
                : error + "\n\n" + mensaje;
        ctx.setUltimaPregunta(texto);
        return PasoResultado.continuar(texto).fallo();
    }

    private PasoResultado verificarCedula(Conversacion conv, ConversacionContexto ctx,
                                          EntradaFlujo entrada) {
        if (contieneRegistro(entrada) || entrada.esRechazo()) {
            return iniciarRegistro(ctx);
        }
        String cedula = entrada.entidades().cedula() != null
                ? entrada.entidades().cedula()
                : (entrada.esCedula() ? entrada.texto() : null);
        if (cedula == null) {
            return pedirCedula(ctx, "Envía un número de cédula de 7 a 10 dígitos o escribe 'soy nuevo'.");
        }
        ctx.setCedula(cedula);
        var encontrado = e.pacientes.porCedula(cedula);
        if (encontrado.isEmpty()) {
            return noEncontrado(ctx, cedula);
        }
        return verificar(conv, ctx, encontrado.get());
    }

    private PasoResultado noEncontrado(ConversacionContexto ctx, String cedula) {
        List<OpcionMenu> opciones = List.of(
                OpcionMenu.decision(1, "REGISTRAR", "Sí, registrarme"),
                OpcionMenu.decision(2, "OTRA_CEDULA", "Intentar con otra cédula"),
                OpcionMenu.decision(3, "RECEPCION", "Hablar con recepción"),
                OpcionMenu.decision(4, "MENU", "Volver al menú"));
        String texto = "No encontré un paciente registrado con esa cédula.\n"
                + "1. Sí, registrarme\n"
                + "2. Intentar con otra cédula\n"
                + "3. Hablar con recepción\n"
                + "4. Volver al menú";
        ctx.setCedula(cedula);
        ctx.setPaso(PASO_NO_ENCONTRADO);
        ctx.setMenuActual(MenuActual.REGISTRO);
        ctx.setOpciones(opciones);
        ctx.setExpectedInput("OPTION");
        ctx.setRequiereIdentificacion(true);
        ctx.setUltimaPregunta(texto);
        return PasoResultado.continuar(texto);
    }

    private PasoResultado accionDeNoEncontrado(Conversacion conv, ConversacionContexto ctx,
                                               EntradaFlujo entrada) {
        String valor = entrada.tieneOpcion() ? entrada.opcion().valor() : null;
        if ("REGISTRAR".equals(valor) || entrada.esConfirmacion()) {
            return iniciarRegistro(ctx);
        }
        if ("OTRA_CEDULA".equals(valor) || entrada.esCedula()) {
            return verificarCedula(conv, ctx, entrada);
        }
        if ("RECEPCION".equals(valor)) {
            conv.setEstado(org.dentalcrm.domain.whatsapp.EstadoConversacion.ATENCION_HUMANA);
            // §13: conserva el punto del proceso y el motivo para recepción.
            if (ctx.tieneFlujoActivo()) {
                ctx.setFlujoAnteriorTransferencia(ctx.getFlujo());
            }
            ctx.setMotivoTransferencia("Paciente no encontrado: pidió hablar con recepción");
            String texto = "Claro 👍 Te voy a comunicar con una persona de nuestro equipo. ¡Un momento por favor!";
            ctx.setUltimaPregunta(texto);
            return PasoResultado.continuar(texto).transferir();
        }
        if ("MENU".equals(valor)) {
            return fin(ctx, soporte().menuTexto());
        }
        return repetir(ctx, "Elige 1 para registrarte, 2 para intentar con otra cédula, "
                + "3 para hablar con recepción o 4 para volver al menú.");
    }

    /** Verifica la identidad (§9) y retoma el flujo suspendido (§2). */
    private PasoResultado verificar(Conversacion conv, ConversacionContexto ctx, Paciente paciente) {
        ctx.setIdentidadVerificada(true);
        ctx.setIdentidadVerificadaEn(java.time.Instant.now());
        ctx.setPacienteId(paciente.getId());
        ctx.setCedula(paciente.getCedula());
        ctx.setNombrePaciente(e.pacientes.nombre(paciente));
        ctx.setRequiereIdentificacion(false);
        conv.setPaciente(paciente);
        String saludo = "¡Hola, " + e.pacientes.nombreCorto(paciente) + "! 👋 Ya encontré tu información.";
        retomar(ctx);
        ctx.setPaso(null);
        return PasoResultado.reenviar(saludo);
    }
}
