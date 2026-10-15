package org.dentalcrm.web.automatizacion;

import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.cuenta.CobroRealizadoEvent;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.web.configuracion.ConsultorioService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class VariableResolver {

    private final ConsultorioService consultorioService;

    public VariableResolver(ConsultorioService consultorioService) {
        this.consultorioService = consultorioService;
    }

    public Map<String, String> variables(Cita cita) {
        Map<String, String> v = new LinkedHashMap<>();
        String nombres = cita.getPaciente().getNombres();
        String apellidos = cita.getPaciente().getApellidos() == null ? "" : cita.getPaciente().getApellidos();
        v.put("nombre", primerNombre(nombres));
        v.put("apellido", primerApellido(apellidos));
        v.put("nombre_completo", (nombres + " " + apellidos).trim());
        v.put("fecha", cita.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        v.put("hora", cita.getHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm")));
        v.put("doctor", cita.getDoctor().getNombres() + " " + cita.getDoctor().getApellidos());
        v.put("servicio", cita.getServicio().getNombre());
        var consultorio = consultorioService.obtener();
        v.put("clinica", consultorio.nombre());
        v.put("direccion", consultorio.direccion());
        v.put("telefono", consultorio.telefono());
        v.put("direccion_consultorio", linea("Dirección: ", consultorio.direccion()));
        v.put("ubicacion", linea("Ubicación en Google Maps: ", consultorio.enlaceUbicacion()));
        v.put("horario", linea("Horario de atención: ", consultorio.horarioAtencion()));
        v.put("codigo_cita", String.valueOf(cita.getId()));
        return v;
    }

    /**
     * Variables de un cobro (evento {@code COBRO_REALIZADO}): las de la
     * clínica y el paciente más el dinero. No depende de una cita.
     */
    public Map<String, String> variablesCobro(Paciente paciente, CobroRealizadoEvent e) {
        Map<String, String> v = new LinkedHashMap<>();
        String nombres = paciente.getNombres() == null ? "" : paciente.getNombres();
        String apellidos = paciente.getApellidos() == null ? "" : paciente.getApellidos();
        v.put("nombre", primerNombre(nombres));
        v.put("apellido", primerApellido(apellidos));
        v.put("nombre_completo", (nombres + " " + apellidos).trim());
        var consultorio = consultorioService.obtener();
        v.put("clinica", consultorio.nombre());
        v.put("direccion", consultorio.direccion());
        v.put("telefono", consultorio.telefono());
        v.put("direccion_consultorio", linea("Dirección: ", consultorio.direccion()));
        v.put("ubicacion", linea("Ubicación en Google Maps: ", consultorio.enlaceUbicacion()));
        v.put("horario", linea("Horario de atención: ", consultorio.horarioAtencion()));
        v.put("monto", dinero(e.monto()));
        v.put("forma_pago", e.formaPago() == null ? "—" : etiquetaFormaPago(e.formaPago().name()));
        v.put("concepto", e.concepto() == null ? "—" : e.concepto());
        v.put("saldo", dinero(e.saldo()));
        v.put("tipo_movimiento", e.tipo() == null ? "Pago" : etiquetaTipoPago(e.tipo().name()));
        v.put("fecha", e.fecha() == null ? "" : e.fecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        v.put("codigo_movimiento", e.movimientoId() == null ? "" : String.valueOf(e.movimientoId()));
        return v;
    }

    private String etiquetaFormaPago(String forma) {
        return switch (forma) {
            case "EFECTIVO" -> "Efectivo";
            case "TRANSFERENCIA" -> "Transferencia";
            case "TARJETA" -> "Tarjeta";
            default -> "Otro";
        };
    }

    private String etiquetaTipoPago(String tipo) {
        return switch (tipo) {
            case "ANTICIPO" -> "Anticipo";
            default -> "Abono";
        };
    }

    private String dinero(BigDecimal valor) {
        BigDecimal v = (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
        return "$ " + v.toPlainString();
    }

    public String renderizar(String contenido, Map<String, String> variables) {
        if (contenido == null) {
            return null;
        }
        String resultado = contenido;
        for (Map.Entry<String, String> e : variables.entrySet()) {
            if (e.getValue() != null) {
                resultado = resultado.replace("{{" + e.getKey() + "}}", e.getValue());
            }
        }
        return resultado;
    }

    private String primerNombre(String nombres) {
        if (nombres == null || nombres.isBlank()) {
            return "";
        }
        return nombres.trim().split("\\s+")[0];
    }

    private String primerApellido(String apellidos) {
        if (apellidos == null || apellidos.isBlank()) {
            return "";
        }
        return apellidos.trim().split("\\s+")[0];
    }

    private String linea(String etiqueta, String valor) {
        return valor == null || valor.isBlank() ? "" : etiqueta + valor;
    }
}