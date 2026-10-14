package org.dentalcrm.web.facturacion.sri;

import org.dentalcrm.domain.configuracion.AmbienteSri;

/**
 * Dónde hablar con el SRI según el ambiente configurado (plan §3.5).
 *
 * <p>Sólo cambia el host: la ruta de los servicios offline es la misma en
 * ambos ambientes (ficha técnica de comprobantes electrónicos §7.2):
 * pruebas {@code celcer.sri.gob.ec}, producción {@code cel.sri.gob.ec}.
 */
public record SriEntorno(String urlBase, AmbienteSri ambiente) {

    private static final String RUTA = "/comprobantes-electronicos-ws/";

    public String urlRecepcion() {
        return ruta(RUTA + "RecepcionComprobantesOffline");
    }

    public String urlAutorizacion() {
        return ruta(RUTA + "AutorizacionComprobantesOffline");
    }

    private String ruta(String sufijo) {
        String base = urlBase == null ? "" : urlBase;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + sufijo;
    }
}
