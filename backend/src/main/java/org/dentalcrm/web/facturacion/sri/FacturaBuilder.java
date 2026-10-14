package org.dentalcrm.web.facturacion.sri;

import org.dentalcrm.domain.cuenta.FormaPago;
import org.dentalcrm.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Construye los datos de una factura comercial a partir de la cuenta del
 * paciente (plan §3.6 {@code FacturaBuilder}).
 *
 * <p><b>Los precios llevan IVA incluido</b> (decisión de la clínica): los
 * cargos de la cuenta ya contienen el impuesto, así que aquí se reparten en
 * base e impuesto para que el comprobante cuadre <b>al céntimo</b> con la
 * cuenta:
 * <pre>
 *   total de la línea (con IVA) = base (sin IVA) + impuesto
 *   importeTotal                = Σ base + Σ impuesto
 * </pre>
 * El impuesto de cada línea se fija a mano en {@link DatosFactura.Linea}
 * ({@code base + impuesto} es exactamente lo que debe cobrarse), en lugar de
 * redondear el 15% de la base, que en algunos importes se desvía un centavo.
 *
 * <p>Los pagos salen de la cuenta cuando no se piden a mano: primero el
 * dinero ya cobrado (abonos y anticipos menos devoluciones, agrupados por
 * código SRI) y el resto como {@code 20} (otros con sistema financiero),
 * es decir, la parte que el paciente aún no ha pagado.
 */
public final class FacturaBuilder {

    public static final int CODIGO_IMPUESTO_IVA = 2;
    public static final int CODIGO_PORCENTAJE_IVA = 4;
    public static final BigDecimal TARIFA_IVA = new BigDecimal("15");

    /** 1 + 15%: los precios de la cuenta ya llevan el IVA. */
    private static final BigDecimal DIVISOR_IVA = new BigDecimal("1.15");
    private static final List<String> ORDEN_CODIGOS =
            List.of(FormaPagoCatalogo.EFECTIVO, FormaPagoCatalogo.TARJETA_DEBITO,
                    FormaPagoCatalogo.TARJETA_CREDITO, FormaPagoCatalogo.OTROS_SISTEMA_FINANCIERO);

    private FacturaBuilder() {
    }

    /** Línea de la cuenta: {@code total = cantidad × precio − descuento propio} (Fase F). */
    public record LineaCuenta(String descripcion, int cantidad, BigDecimal precioUnitario, BigDecimal total) {
    }

    /** Cobro de la cuenta: positivo para abonos/anticipos, negativo para devoluciones. */
    public record Cobro(FormaPago forma, BigDecimal total) {
    }

    public record Entrada(
            String claveAcceso,
            LocalDate fechaEmision,
            DatosFactura.Emisor emisor,
            DatosFactura.Comprador comprador,
            List<LineaCuenta> cargos,
            BigDecimal descuentoCuenta,
            List<Cobro> cobros,
            List<DatosFactura.Pago> pagosExplicitos,
            List<DatosFactura.CampoAdicional> adicionales
    ) {
    }

    /** Totales reales del comprobante: se persisten y se muestran en la bandeja. */
    public record Totales(BigDecimal base, BigDecimal impuesto, BigDecimal descuento, BigDecimal total) {
    }

    public record FacturaConstruida(DatosFactura datos, Totales totales) {
    }

    public static FacturaConstruida construir(Entrada entrada) {
        List<LineaCuenta> cargos = entrada.cargos() == null ? List.of() : entrada.cargos();
        if (cargos.isEmpty()) {
            throw new BusinessException("SIN_DETALLE", "La cuenta no tiene cargos para facturar");
        }

        BigDecimal descuentoCuenta = dinero(entrada.descuentoCuenta());
        if (descuentoCuenta.signum() < 0) {
            throw new BusinessException("DESCUENTO_INVALIDO", "El descuento de la cuenta no puede ser negativo");
        }

        List<BigDecimal> totalesLinea = new ArrayList<>();
        for (LineaCuenta cargo : cargos) {
            if (cargo == null || cargo.total() == null || cargo.cantidad() < 1 || cargo.precioUnitario() == null) {
                throw new BusinessException("MONTO_INVALIDO", "La línea «"
                        + descripcion(cargo) + "» no tiene un importe válido");
            }
            if (cargo.precioUnitario().signum() <= 0) {
                throw new BusinessException("MONTO_INVALIDO",
                        "El precio unitario de «" + descripcion(cargo) + "» debe ser mayor que cero");
            }
            totalesLinea.add(dinero(cargo.total()));
        }

        BigDecimal sumaCargos = suma(totalesLinea);
        if (sumaCargos.signum() <= 0) {
            throw new BusinessException("MONTO_INVALIDO", "La cuenta no tiene importes por facturar");
        }
        if (descuentoCuenta.compareTo(sumaCargos) > 0) {
            throw new BusinessException("DESCUENTO_EXCEDE",
                    "Los descuentos de la cuenta (" + descuentoCuenta.toPlainString()
                            + ") superan los cargos (" + sumaCargos.toPlainString() + ")");
        }

        List<BigDecimal> partes = repartir(descuentoCuenta, totalesLinea);
        List<DatosFactura.Linea> lineas = new ArrayList<>();
        for (int i = 0; i < cargos.size(); i++) {
            LineaCuenta cargo = cargos.get(i);
            BigDecimal totalLinea = dinero(totalesLinea.get(i).subtract(partes.get(i)));
            if (totalLinea.signum() <= 0) {
                throw new BusinessException("MONTO_INVALIDO",
                        "Tras los descuentos, la línea «" + descripcion(cargo) + "» queda en cero");
            }
            BigDecimal brutoCompleto = dinero(BigDecimal.valueOf(cargo.cantidad())
                    .multiply(cargo.precioUnitario()));
            if (brutoCompleto.compareTo(totalesLinea.get(i)) < 0) {
                throw new BusinessException("MONTO_INVALIDO",
                        "El total de «" + descripcion(cargo) + "» es mayor que su precio por cantidad");
            }
            lineas.add(lineaNeta(cargo, totalLinea,
                    brutoCompleto.subtract(totalesLinea.get(i)).add(partes.get(i))));
        }

        BigDecimal total = sumaCargos.subtract(descuentoCuenta);
        DatosFactura datos = new DatosFactura(
                entrada.claveAcceso(),
                entrada.fechaEmision(),
                entrada.emisor(),
                entrada.comprador(),
                List.copyOf(lineas),
                pagos(entrada, total),
                entrada.adicionales());
        return new FacturaConstruida(datos, totales(lineas, total));
    }

    // ------------------------------------------------------------------
    // Línea con IVA incluido
    // ------------------------------------------------------------------

    /**
     * Convierte una línea cuyo total ya lleva IVA en la línea que se emite:
     * precio unitario e impuesto desglosados ({@code 6} decimales de precio,
     * {@code 2} de todo lo demás, como exige el XSD).
     */
    private static DatosFactura.Linea lineaNeta(LineaCuenta cargo, BigDecimal totalLinea,
                                                BigDecimal descuentoBruto) {
        BigDecimal base = dinero(totalLinea.divide(DIVISOR_IVA, 6, RoundingMode.HALF_UP));
        BigDecimal descuentoNeto = dinero(descuentoBruto.divide(DIVISOR_IVA, 6, RoundingMode.HALF_UP));
        BigDecimal precioNeto = base.add(descuentoNeto)
                .divide(BigDecimal.valueOf(cargo.cantidad()), 6, RoundingMode.HALF_UP);
        // La base que verá el SRI es la que resulta de los valores emitidos:
        // se recalcula para que base + impuesto sea exactamente lo cobrado.
        BigDecimal baseEmitida = dinero(BigDecimal.valueOf(cargo.cantidad()).multiply(precioNeto)
                .subtract(descuentoNeto));
        BigDecimal impuesto = dinero(totalLinea.subtract(baseEmitida));
        return new DatosFactura.Linea(null, descripcion(cargo), BigDecimal.valueOf(cargo.cantidad()),
                precioNeto, descuentoNeto, CODIGO_IMPUESTO_IVA, CODIGO_PORCENTAJE_IVA,
                TARIFA_IVA, impuesto);
    }

    /** Reparte el descuento de la cuenta entre las líneas en proporción a su total. */
    private static List<BigDecimal> repartir(BigDecimal descuento, List<BigDecimal> totales) {
        List<BigDecimal> partes = new ArrayList<>(Collections.nCopies(totales.size(), BigDecimal.ZERO));
        if (descuento.signum() <= 0) {
            return partes;
        }
        BigDecimal suma = suma(totales);
        int mayor = 0;
        for (int i = 1; i < totales.size(); i++) {
            if (totales.get(i).compareTo(totales.get(mayor)) > 0) mayor = i;
        }
        BigDecimal repartido = BigDecimal.ZERO;
        for (int i = 0; i < totales.size(); i++) {
            BigDecimal parte = descuento.multiply(totales.get(i)).divide(suma, 2, RoundingMode.HALF_UP);
            partes.set(i, parte);
            repartido = repartido.add(parte);
        }
        // El redondeo (± centavos) se resuelve en la línea más grande para
        // que la suma de las partes sea exactamente el descuento de la cuenta.
        partes.set(mayor, dinero(partes.get(mayor).add(descuento.subtract(repartido))));
        return partes;
    }

    // ------------------------------------------------------------------
    // Pagos
    // ------------------------------------------------------------------

    private static List<DatosFactura.Pago> pagos(Entrada entrada, BigDecimal total) {
        if (entrada.pagosExplicitos() != null && !entrada.pagosExplicitos().isEmpty()) {
            return pagosExplicitos(entrada.pagosExplicitos(), total);
        }

        Map<String, BigDecimal> porCodigo = new LinkedHashMap<>();
        List<Cobro> cobros = entrada.cobros() == null ? List.of() : entrada.cobros();
        for (Cobro cobro : cobros) {
            if (cobro == null || cobro.forma() == null || cobro.total() == null) continue;
            porCodigo.merge(FormaPagoCatalogo.codigo(cobro.forma()), dinero(cobro.total()), BigDecimal::add);
        }

        // Se cobra primero lo ya recibido, agrupado por código SRI, sin
        // pasar del total de la factura.
        Map<String, BigDecimal> seleccionados = new LinkedHashMap<>();
        BigDecimal restante = total;
        for (String codigo : ORDEN_CODIGOS) {
            restante = cobrar(seleccionados, porCodigo.remove(codigo), codigo, restante);
        }
        for (Map.Entry<String, BigDecimal> resto : porCodigo.entrySet()) {
            restante = cobrar(seleccionados, resto.getValue(), resto.getKey(), restante);
        }
        // Lo que el paciente todavía no ha pagado: crédito con sistema financiero.
        if (restante.signum() > 0) {
            seleccionados.merge(FormaPagoCatalogo.OTROS_SISTEMA_FINANCIERO, dinero(restante), BigDecimal::add);
        }

        List<DatosFactura.Pago> pagos = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : seleccionados.entrySet()) {
            if (entry.getValue().signum() > 0) {
                pagos.add(new DatosFactura.Pago(entry.getKey(), dinero(entry.getValue()), null, null));
            }
        }
        return List.copyOf(pagos);
    }

    /** Suma hasta {@code restante} lo recibido con el código indicado. */
    private static BigDecimal cobrar(Map<String, BigDecimal> seleccionados, BigDecimal monto,
                                     String codigo, BigDecimal restante) {
        if (monto == null || monto.signum() <= 0 || restante.signum() <= 0) {
            return restante;
        }
        BigDecimal aCobrar = monto.min(restante);
        seleccionados.merge(codigo, dinero(aCobrar), BigDecimal::add);
        return dinero(restante.subtract(aCobrar));
    }

    private static List<DatosFactura.Pago> pagosExplicitos(List<DatosFactura.Pago> pedidos, BigDecimal total) {
        BigDecimal suma = BigDecimal.ZERO;
        for (DatosFactura.Pago pago : pedidos) {
            FormaPagoCatalogo.validar(pago.formaPago());
            if (pago.total() == null || pago.total().signum() <= 0) {
                throw new BusinessException("MONTO_INVALIDO",
                        "El total de la forma de pago " + pago.formaPago() + " debe ser mayor que cero");
            }
            if (pago.plazo() != null && pago.plazo().signum() < 0) {
                throw new BusinessException("MONTO_INVALIDO", "El plazo de la forma de pago no puede ser negativo");
            }
            suma = suma.add(dinero(pago.total()));
        }
        if (suma.compareTo(total) != 0) {
            throw new BusinessException("FORMAS_PAGO_NO_COINCIDEN",
                    "Las formas de pago suman " + suma.toPlainString()
                            + " pero la factura totaliza " + total.toPlainString());
        }
        return pedidos.stream()
                .map(p -> new DatosFactura.Pago(p.formaPago(), dinero(p.total()), p.plazo(), p.unidadTiempo()))
                .toList();
    }

    // ------------------------------------------------------------------
    // Totales
    // ------------------------------------------------------------------

    /** Totales tal como los calcula el generador, para persistirlos sin parsear el XML. */
    private static Totales totales(List<DatosFactura.Linea> lineas, BigDecimal esperado) {
        BigDecimal base = BigDecimal.ZERO;
        BigDecimal impuesto = BigDecimal.ZERO;
        BigDecimal descuento = BigDecimal.ZERO;
        for (DatosFactura.Linea linea : lineas) {
            BigDecimal lineaBase = dinero(linea.cantidad().multiply(linea.precioUnitario())
                    .subtract(linea.descuento()));
            base = base.add(lineaBase);
            impuesto = impuesto.add(dinero(linea.impuesto()));
            descuento = descuento.add(dinero(linea.descuento()));
        }
        BigDecimal total = dinero(base.add(impuesto));
        if (total.compareTo(esperado) != 0) {
            throw new BusinessException("TOTALES_INCONSISTENTES",
                    "Los totales del comprobante (" + total.toPlainString()
                            + ") no coinciden con la cuenta (" + esperado.toPlainString() + ")");
        }
        return new Totales(dinero(base), dinero(impuesto), dinero(descuento), total);
    }

    private static String descripcion(LineaCuenta cargo) {
        if (cargo == null || cargo.descripcion() == null || cargo.descripcion().isBlank()) {
            return "Cargo";
        }
        String texto = cargo.descripcion().trim();
        return texto.length() > 300 ? texto.substring(0, 300) : texto;
    }

    private static BigDecimal suma(List<BigDecimal> valores) {
        return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal dinero(BigDecimal valor) {
        BigDecimal v = valor == null ? BigDecimal.ZERO : valor;
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
