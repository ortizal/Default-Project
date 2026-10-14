package org.dentalcrm.web.facturacion.sri;

import org.dentalcrm.domain.configuracion.AmbienteSri;
import org.dentalcrm.domain.configuracion.TipoEmisionSri;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Clave de acceso del comprobante electrónico — ficha técnica del SRI,
 * tabla 1: 49 dígitos = fecha(8) + tipo de comprobante(2) + RUC(13) +
 * ambiente(1) + serie establecimiento-punto(6) + secuencial(9) +
 * código numérico(8) + tipo de emisión(1) + dígito verificador(1).
 *
 * <p>El dígito verificador (posición 49) se calcula con el módulo 11 sobre
 * los 48 primeros dígitos, con factor de chequeo ponderado que arranca en 2
 * desde la derecha y cicla 2..7: resto = suma % 11; DV = 11 − resto, donde
 * 11 se escribe 0 y 10 se escribe 1.
 *
 * <p>La fecha de emisión no puede caer en sábado ni domingo: si cae, se
 * traslada al siguiente día hábil (festivos nacionales no se consideran;
 * se revisarán cuando la clínica indique su calendario).
 */
@Service
public class ClaveAccesoService {

    public static final String COD_DOC_FACTURA = "01";
    /** Nota de crédito electrónica (ficha técnica del SRI, tabla 5). */
    public static final String COD_DOC_NOTA_CREDITO = "04";

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final SecureRandom ALEATORIO = new SecureRandom();

    /** Clave de acceso generada junto con la fecha de emisión realmente usada. */
    public record ClaveDeAcceso(String valor, LocalDate fechaEmision) {
    }

    public ClaveDeAcceso generar(LocalDate fechaSolicitada, String ruc, String codDoc,
                                 AmbienteSri ambiente, String establecimiento, String puntoEmision,
                                 int secuencial, TipoEmisionSri tipoEmision) {
        LocalDate fecha = fechaEmisionValida(fechaSolicitada);
        String secuencial9 = String.format("%09d", secuencial);
        String sinVerificador = fecha.format(FECHA)
                + codDoc
                + ruc
                + codigoAmbiente(ambiente)
                + establecimiento + puntoEmision
                + secuencial9
                + codigoNumerico()
                + codigoEmision(tipoEmision);
        return new ClaveDeAcceso(sinVerificador + digitoVerificador(sinVerificador), fecha);
    }

    /** Sábado y domingo no son días hábiles: se emite el siguiente lunes. */
    public LocalDate fechaEmisionValida(LocalDate fecha) {
        LocalDate ajustada = fecha;
        while (ajustada.getDayOfWeek() == DayOfWeek.SATURDAY
                || ajustada.getDayOfWeek() == DayOfWeek.SUNDAY) {
            ajustada = ajustada.plusDays(1);
        }
        return ajustada;
    }

    /** Módulo 11 sobre los 48 dígitos previos (ficha técnica del SRI, §5.2). */
    public int digitoVerificador(String claveSinVerificador) {
        int suma = 0;
        int factor = 2;
        for (int i = claveSinVerificador.length() - 1; i >= 0; i--) {
            int digito = claveSinVerificador.charAt(i) - '0';
            suma += digito * factor;
            factor = factor == 7 ? 2 : factor + 1;
        }
        int resto = suma % 11;
        int dv = 11 - resto;
        if (dv == 11) return 0;
        if (dv == 10) return 1;
        return dv;
    }

    public static String codigoAmbiente(AmbienteSri ambiente) {
        return ambiente == AmbienteSri.PRODUCCION ? "2" : "1";
    }

    public static String codigoEmision(TipoEmisionSri tipoEmision) {
        return tipoEmision == TipoEmisionSri.CONTINGENCIA ? "2" : "1";
    }

    private String codigoNumerico() {
        return String.format("%08d", ALEATORIO.nextInt(100_000_000));
    }
}
