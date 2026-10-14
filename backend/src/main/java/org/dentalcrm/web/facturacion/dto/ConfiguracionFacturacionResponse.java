package org.dentalcrm.web.facturacion.dto;

/**
 * Configuración tributaria de la clínica (plan §3.5). Los campos de
 * credenciales sólo se reportan como "configuradas": ni la clave del SRI
 * ni la contraseña del .p12 se devuelven nunca.
 *
 * @param razonSocial       dato fiscal leído de la configuración del consultorio (V21/V24)
 * @param secuencialSiguiente número que usaría el próximo comprobante, p. ej. `001-001-000000001`
 * @param firmaModo         `SIMULADA` mientras falte el .p12 o su contraseña; `CERTIFICADO` cuando ambos existen (plan §3.6)
 * @param firmaMotivo       por qué la firma está simulada, o `null` si es real
 */
public record ConfiguracionFacturacionResponse(
        String ambiente,
        String tipoEmision,
        boolean contribuyenteEspecial,
        boolean agenteRetencion,
        boolean obligadoContabilidad,
        String establecimiento,
        String puntoEmision,
        int secuencialActual,
        String secuencialSiguiente,
        String sriUrlPruebas,
        String sriUrlProduccion,
        String sriUrlActiva,
        String sriUsuario,
        boolean sriCredencialesConfiguradas,
        String firmaModo,
        String firmaMotivo,
        String firmaArchivo,
        boolean firmaPasswordConfigurada,
        String razonSocial,
        String ruc,
        String direccion,
        String telefono,
        String correo,
        String firmaTitular,
        java.time.Instant firmaValidoDesde,
        java.time.Instant firmaValidoHasta
) {
}
