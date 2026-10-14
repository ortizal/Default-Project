package org.dentalcrm.web.facturacion;

import org.dentalcrm.domain.cuenta.CuentaDetalle;
import org.dentalcrm.domain.cuenta.CuentaDetalleRepository;
import org.dentalcrm.domain.cuenta.CuentaPaciente;
import org.dentalcrm.domain.cuenta.CuentaPacienteRepository;
import org.dentalcrm.domain.cuenta.TipoMovimientoCuenta;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.web.facturacion.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Dashboard y reportes financieros — Fase F (plan §3.12 y §3.13),
 * sin datos del SRI (esa parte es la Fase G): facturado, cobrado,
 * cuentas por cobrar, anticipos, ticket promedio, ventas por rango,
 * producción por odontólogo, saldos y corte de caja.
 *
 * <p>Antes de existir comprobantes electrónicos, "facturado" son los
 * cargos de las cuentas (§3.3) y "cobrado" el dinero recibido (abonos +
 * anticipos − devoluciones).
 */
@Service
public class ReporteFinancieroService {

    private static final BigDecimal CERO = BigDecimal.ZERO;

    private final CuentaDetalleRepository detalleRepository;
    private final CuentaPacienteRepository cuentaRepository;
    private final CajaService cajaService;
    private final ZoneId zona;

    public ReporteFinancieroService(CuentaDetalleRepository detalleRepository,
                                    CuentaPacienteRepository cuentaRepository,
                                    CajaService cajaService,
                                    @Value("${app.timezone}") String zona) {
        this.detalleRepository = detalleRepository;
        this.cuentaRepository = cuentaRepository;
        this.cajaService = cajaService;
        this.zona = ZoneId.of(zona);
    }

    private record Rango(LocalDate desde, LocalDate hasta) {
    }

    // ------------------------------------------------------------------
    // Dashboard (§3.12)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public DashboardFinancieroResponse dashboard(LocalDate desde, LocalDate hasta) {
        Rango rango = rango(desde, hasta);
        List<CuentaDetalle> movimientos = detalleRepository
                .findByAnuladoFalseAndFechaBetweenOrderByFechaAscIdAsc(rango.desde(), rango.hasta());
        LocalDate hoy = LocalDate.now(zona);
        List<CuentaDetalle> delDia = detalleRepository.findByAnuladoFalseAndFecha(hoy);
        List<CuentaDetalle> delMes = detalleRepository
                .findByAnuladoFalseAndFechaBetweenOrderByFechaAscIdAsc(hoy.withDayOfMonth(1), hoy);

        Totales t = totales(movimientos);
        Totales hoyT = totales(delDia);
        Totales mesT = totales(delMes);

        CuentaPacienteRepository.ResumenSaldosPendientes saldos = cuentaRepository.resumirSaldosPendientes();
        BigDecimal porCobrar = saldos.getTotal();
        int pendientes = Math.toIntExact(saldos.getCantidad());
        BigDecimal anticipos = detalleRepository.anticiposNetos();

        BigDecimal ticket = t.cobros == 0 ? CERO
                : t.cobrado.divide(BigDecimal.valueOf(t.cobros), 2, RoundingMode.HALF_UP);

        return new DashboardFinancieroResponse(rango.desde(), rango.hasta(),
                f(t.facturado), f(t.cobrado), f(t.descuentos), f(t.devoluciones),
                f(hoyT.facturado), f(hoyT.cobrado), f(mesT.facturado), f(mesT.cobrado),
                f(porCobrar), pendientes, f(anticipos), t.cobros, ticket,
                porFormaPago(movimientos), porOdontologo(movimientos), porServicio(movimientos));
    }

    // ------------------------------------------------------------------
    // Reportes (§3.13)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ReporteVentasResponse ventas(LocalDate desde, LocalDate hasta) {
        Rango rango = rango(desde, hasta);
        List<CuentaDetalle> movimientos = detalleRepository
                .findByAnuladoFalseAndFechaBetweenOrderByFechaAscIdAsc(rango.desde(), rango.hasta());
        Totales t = totales(movimientos);

        Map<LocalDate, Totales> porDia = new TreeMap<>();
        for (CuentaDetalle movimiento : movimientos) {
            porDia.computeIfAbsent(movimiento.getFecha(), k -> new Totales())
                    .sumar(movimiento);
        }
        List<ReporteVentasResponse.FilaDiaria> filas = porDia.entrySet().stream()
                .map(e -> new ReporteVentasResponse.FilaDiaria(e.getKey(),
                        f(e.getValue().facturado), f(e.getValue().cobrado),
                        e.getValue().movimientos))
                .toList();

        return new ReporteVentasResponse(rango.desde(), rango.hasta(),
                f(t.facturado), f(t.cobrado), f(t.descuentos), f(t.devoluciones),
                t.cargos, t.cobros, filas, porFormaPago(movimientos), porServicio(movimientos));
    }

    @Transactional(readOnly = true)
    public ReporteProduccionResponse produccion(LocalDate desde, LocalDate hasta) {
        Rango rango = rango(desde, hasta);
        List<CuentaDetalle> movimientos = detalleRepository
                .findByAnuladoFalseAndFechaBetweenOrderByFechaAscIdAsc(rango.desde(), rango.hasta());
        Totales t = totales(movimientos);
        return new ReporteProduccionResponse(rango.desde(), rango.hasta(),
                f(t.facturado), t.cargos, porOdontologoDetalle(movimientos));
    }

    @Transactional(readOnly = true)
    public ReporteSaldosResponse saldos() {
        List<CuentaPaciente> cuentas = cuentaRepository.findAllByOrderByUpdatedAtDesc();
        BigDecimal total = CERO;
        List<CuentaResumenResponse> filas = new ArrayList<>();
        for (CuentaPaciente cuenta : cuentas) {
            if (cuenta.getSaldo().signum() <= 0) {
                continue;
            }
            total = total.add(cuenta.getSaldo());
            filas.add(CuentaResumenResponse.from(cuenta,
                    nombrePaciente(cuenta), cuenta.getPaciente().getCedula(),
                    (int) detalleRepository.countByCuentaIdAndAnuladoFalse(cuenta.getId())));
        }
        return new ReporteSaldosResponse(f(total), filas.size(), filas);
    }

    @Transactional(readOnly = true)
    public ReporteCajaResponse reporteCaja(LocalDate desde, LocalDate hasta) {
        Rango rango = rango(desde, hasta);
        List<CajaResponse> sesiones = cajaService.listar(rango.desde(), rango.hasta());

        BigDecimal aperturas = CERO;
        BigDecimal ingresos = CERO;
        BigDecimal egresos = CERO;
        BigDecimal diferencias = CERO;
        List<ReporteCajaResponse.SesionResumen> filas = new ArrayList<>();
        for (CajaResponse sesion : sesiones) {
            aperturas = aperturas.add(nz(sesion.montoApertura()));
            ingresos = ingresos.add(nz(sesion.totales().ingresos()));
            egresos = egresos.add(nz(sesion.totales().egresos()));
            diferencias = diferencias.add(nz(sesion.diferencia()));
            filas.add(new ReporteCajaResponse.SesionResumen(sesion.id(), sesion.nombre(),
                    sesion.fecha(), sesion.estado(), sesion.montoApertura(),
                    sesion.montoDeclarado(), sesion.diferencia(),
                    sesion.totales().movimientos()));
        }
        return new ReporteCajaResponse(rango.desde(), rango.hasta(), sesiones.size(),
                f(aperturas), f(ingresos), f(egresos), f(diferencias), filas);
    }

    // ------------------------------------------------------------------
    // Internos
    // ------------------------------------------------------------------

    /** Acumuladores de un conjunto de movimientos. */
    private static final class Totales {
        BigDecimal facturado = CERO;
        BigDecimal cobrado = CERO;
        BigDecimal descuentos = CERO;
        BigDecimal devoluciones = CERO;
        Map<String, BigDecimal> porForma = new LinkedHashMap<>();
        Map<String, BigDecimal[]> porOdontologo = new LinkedHashMap<>();
        Map<String, BigDecimal[]> porServicio = new LinkedHashMap<>();
        int cargos;
        int cobros;
        int movimientos;

        void sumar(CuentaDetalle m) {
            movimientos++;
            BigDecimal total = m.getTotal() == null ? CERO : m.getTotal();
            switch (m.getTipo()) {
                case CARGO -> {
                    facturado = facturado.add(total);
                    cargos++;
                    if (m.getOdontologo() != null) {
                        acumular(porOdontologo, nombre(m.getOdontologo().getNombres(),
                                m.getOdontologo().getApellidos()), total);
                    }
                    if (m.getServicio() != null) {
                        acumular(porServicio, m.getServicio().getNombre(), total);
                    }
                }
                case DESCUENTO -> descuentos = descuentos.add(total);
                case ABONO, ANTICIPO -> {
                    cobrado = cobrado.add(total);
                    cobros++;
                    porForma.merge(nombre(m.getFormaPago()), total, BigDecimal::add);
                }
                case DEVOLUCION -> {
                    devoluciones = devoluciones.add(total);
                    cobrado = cobrado.subtract(total);
                    porForma.merge(nombre(m.getFormaPago()), total.negate(), BigDecimal::add);
                }
            }
        }

        private static void acumular(Map<String, BigDecimal[]> mapa, String clave, BigDecimal monto) {
            BigDecimal[] dato = mapa.computeIfAbsent(clave, k -> new BigDecimal[]{CERO, CERO});
            dato[0] = dato[0].add(monto);
            dato[1] = dato[1].add(BigDecimal.ONE);
        }

        private static String nombre(Enum<?> valor) {
            return valor == null ? "OTRO" : valor.name();
        }

        private static String nombre(String... partes) {
            return String.join(" ", partes).trim();
        }
    }

    private Totales totales(List<CuentaDetalle> movimientos) {
        Totales t = new Totales();
        movimientos.forEach(t::sumar);
        return t;
    }

    private List<FormaPagoTotal> porFormaPago(List<CuentaDetalle> movimientos) {
        Map<String, BigDecimal> mapa = new LinkedHashMap<>();
        for (CuentaDetalle m : movimientos) {
            if (!m.getTipo().esCredito()) {
                continue;
            }
            BigDecimal total = m.getTotal();
            mapa.merge(m.getFormaPago() == null ? "OTRO" : m.getFormaPago().name(),
                    m.getTipo() == TipoMovimientoCuenta.DEVOLUCION ? total.negate() : total,
                    BigDecimal::add);
        }
        return mapa.entrySet().stream()
                .map(e -> new FormaPagoTotal(e.getKey(), f(e.getValue())))
                .toList();
    }

    private List<TotalPorOdontologo> porOdontologo(List<CuentaDetalle> movimientos) {
        Map<String, BigDecimal[]> mapa = new LinkedHashMap<>();
        for (CuentaDetalle m : movimientos) {
            if (m.getTipo() != TipoMovimientoCuenta.CARGO || m.getOdontologo() == null) {
                continue;
            }
            String clave = nombreCompleto(m);
            BigDecimal[] dato = mapa.computeIfAbsent(clave, k -> new BigDecimal[]{CERO, CERO});
            dato[0] = dato[0].add(m.getTotal());
            dato[1] = dato[1].add(BigDecimal.ONE);
        }
        return mapa.entrySet().stream()
                .sorted((a, b) -> b.getValue()[0].compareTo(a.getValue()[0]))
                .map(e -> new TotalPorOdontologo(e.getKey(), f(e.getValue()[0]),
                        e.getValue()[1].intValue()))
                .toList();
    }

    private List<TotalPorServicio> porServicio(List<CuentaDetalle> movimientos) {
        Map<String, BigDecimal[]> mapa = new LinkedHashMap<>();
        for (CuentaDetalle m : movimientos) {
            if (m.getTipo() != TipoMovimientoCuenta.CARGO || m.getServicio() == null) {
                continue;
            }
            String clave = m.getServicio().getNombre();
            BigDecimal[] dato = mapa.computeIfAbsent(clave, k -> new BigDecimal[]{CERO, CERO});
            dato[0] = dato[0].add(m.getTotal());
            dato[1] = dato[1].add(BigDecimal.ONE);
        }
        return mapa.entrySet().stream()
                .sorted((a, b) -> b.getValue()[0].compareTo(a.getValue()[0]))
                .map(e -> new TotalPorServicio(e.getKey(), f(e.getValue()[0]),
                        e.getValue()[1].intValue()))
                .toList();
    }

    private List<ReporteProduccionResponse.FilaProduccion> porOdontologoDetalle(
            List<CuentaDetalle> movimientos) {
        Map<String, BigDecimal[]> mapa = new LinkedHashMap<>();
        for (CuentaDetalle m : movimientos) {
            if (m.getTipo() != TipoMovimientoCuenta.CARGO || m.getOdontologo() == null) {
                continue;
            }
            String clave = nombreCompleto(m);
            BigDecimal[] dato = mapa.computeIfAbsent(clave, k -> new BigDecimal[]{CERO, CERO, CERO});
            dato[0] = dato[0].add(m.getTotal());
            dato[1] = dato[1].add(BigDecimal.ONE);
        }
        for (CuentaDetalle m : movimientos) {
            if (m.getTipo() != TipoMovimientoCuenta.DESCUENTO || m.getOdontologo() == null) {
                continue;
            }
            BigDecimal[] dato = mapa.get(nombreCompleto(m));
            if (dato != null) {
                dato[2] = dato[2].add(m.getTotal());
            }
        }
        return mapa.entrySet().stream()
                .sorted((a, b) -> b.getValue()[0].compareTo(a.getValue()[0]))
                .map(e -> new ReporteProduccionResponse.FilaProduccion(e.getKey(),
                        f(e.getValue()[0]), e.getValue()[1].intValue(), f(e.getValue()[2])))
                .toList();
    }

    private String nombreCompleto(CuentaDetalle m) {
        String texto = (val(m.getOdontologo().getNombres()) + " "
                + val(m.getOdontologo().getApellidos())).trim();
        return texto.isBlank() ? "Sin asignar" : texto;
    }

    private String nombrePaciente(CuentaPaciente cuenta) {
        return (val(cuenta.getPaciente().getNombres()) + " "
                + val(cuenta.getPaciente().getApellidos())).trim();
    }

    private Rango rango(LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta == null ? LocalDate.now(zona) : hasta;
        LocalDate ini = desde == null ? fin.minusYears(1) : desde;
        if (ini.isAfter(fin)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "RANGO_INVALIDO",
                    "El inicio no puede ser posterior al fin");
        }
        return new Rango(ini, fin);
    }

    private String val(String texto) {
        return texto == null ? "" : texto;
    }

    private BigDecimal nz(BigDecimal valor) {
        return valor == null ? CERO : valor;
    }

    private BigDecimal f(BigDecimal valor) {
        return nz(valor).setScale(2, RoundingMode.HALF_UP);
    }
}
