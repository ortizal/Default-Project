package org.dentalcrm.web.facturacion.sri;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.dentalcrm.domain.comprobante.ComprobanteElectronico;
import org.dentalcrm.domain.comprobante.TipoComprobanteSri;
import org.dentalcrm.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * RIDE del comprobante electrónico — plan §3.11: PDF con los datos
 * fiscales del emisor, el adquiriente, el detalle, los impuestos, las
 * formas de pago, la clave de acceso y la autorización del SRI.
 *
 * <p>Todo se lee del XML del propio comprobante (el autorizado si existe),
 * de modo que el PDF no puede divergir de lo que se envió. Los mismos
 * pasos se usan para las notas de crédito y las retenciones cuando la
 * Fase G las incorpore.
 */
@Service
public class RideGenerator {

    private static final Color PRIMARIO = new Color(20, 90, 160);
    private static final Color GRIS = new Color(90, 90, 90);
    private static final Color ALERTA = new Color(170, 30, 30);
    private static final DateTimeFormatter FECHA_AUTORIZACION =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public String nombreArchivo(ComprobanteElectronico comprobante) {
        String numero = comprobante.getNumeroComprobante() == null
                ? comprobante.getClaveAcceso()
                : comprobante.getNumeroComprobante();
        return "ride-" + numero + ".pdf";
    }

    public byte[] generar(ComprobanteElectronico comprobante) {
        RideDatos datos = RideXmlParser.parsear(xml(comprobante));
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            cabecera(doc, comprobante, datos);
            bloque(doc, "INFORMACIÓN DEL EMISOR", emisor(datos));
            bloque(doc, "INFORMACIÓN DEL ADQUIRIENTE", adquiriente(datos));
            detalle(doc, datos);
            impuestos(doc, datos);
            totales(doc, datos);
            pagos(doc, datos);
            adicionales(doc, datos);
            autorizacion(doc, comprobante, datos);

            doc.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("No se pudo generar el RIDE del comprobante", e);
        }
    }

    private static String xml(ComprobanteElectronico comprobante) {
        String xml = comprobante.getXmlAutorizado() != null && !comprobante.getXmlAutorizado().isBlank()
                ? comprobante.getXmlAutorizado()
                : comprobante.getXmlFirmado();
        if (xml == null || xml.isBlank()) {
            throw new BusinessException("COMPROBANTE_SIN_XML",
                    "El comprobante aún no tiene XML firmado para generar el RIDE");
        }
        return xml;
    }

    // ------------------------------------------------------------------
    // Cabecera
    // ------------------------------------------------------------------

    private void cabecera(Document doc, ComprobanteElectronico comprobante, RideDatos datos)
            throws DocumentException {
        Font emisor = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, PRIMARIO);
        Font gris = FontFactory.getFont(FontFactory.HELVETICA, 9, GRIS);
        Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
        Font alerta = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, ALERTA);

        Paragraph nombre = new Paragraph(nuloVacio(datos.razonSocialEmisor()), emisor);
        nombre.setAlignment(Element.ALIGN_CENTER);
        doc.add(nombre);

        if (datos.rucEmisor() != null) {
            Paragraph ruc = new Paragraph("RUC: " + datos.rucEmisor(), gris);
            ruc.setAlignment(Element.ALIGN_CENTER);
            doc.add(ruc);
        }
        if (datos.nombreComercial() != null) {
            Paragraph comercial = new Paragraph(datos.nombreComercial(), gris);
            comercial.setAlignment(Element.ALIGN_CENTER);
            doc.add(comercial);
        }

        Paragraph encabezado = new Paragraph(titulo(comprobante.getTipoComprobante())
                + "  " + nuloVacio(comprobante.getNumeroComprobante()), titulo);
        encabezado.setAlignment(Element.ALIGN_CENTER);
        encabezado.setSpacingBefore(6);
        doc.add(encabezado);

        if (comprobante.isSimulado()) {
            Paragraph sello = new Paragraph("SIMULADO — SIN VALIDEZ TRIBUTARIA", alerta);
            sello.setAlignment(Element.ALIGN_CENTER);
            doc.add(sello);
        }

        Paragraph fechas = new Paragraph("N° de comprobante: "
                + nuloVacio(comprobante.getNumeroComprobante())
                + "   ·   Fecha de emisión: " + nuloVacio(datos.fechaEmision()), gris);
        fechas.setAlignment(Element.ALIGN_CENTER);
        fechas.setSpacingAfter(6);
        doc.add(fechas);
    }

    private static String titulo(TipoComprobanteSri tipo) {
        if (tipo == null) {
            return "COMPROBANTE ELECTRÓNICO";
        }
        return switch (tipo) {
            case FACTURA -> "FACTURA ELECTRÓNICA";
            case NOTA_CREDITO -> "NOTA DE CRÉDITO ELECTRÓNICA";
            case NOTA_DEBITO -> "NOTA DE DÉBITO ELECTRÓNICA";
            case GUIA_REMISION -> "GUÍA DE REMISIÓN ELECTRÓNICA";
            case RETENCION -> "COMPROBANTE DE RETENCIÓN ELECTRÓNICO";
        };
    }

    private static List<String[]> emisor(RideDatos datos) {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"Razón social", nuloVacio(datos.razonSocialEmisor())});
        agregar(filas, "Nombre comercial", datos.nombreComercial());
        filas.add(new String[]{"RUC", nuloVacio(datos.rucEmisor())});
        agregar(filas, "Dirección matriz", datos.direccionMatriz());
        agregar(filas, "Dirección del establecimiento", datos.direccionEstablecimiento());
        filas.add(new String[]{"Obligado a llevar contabilidad",
                nuloVacio(datos.obligadoContabilidad())});
        return filas;
    }

    private static List<String[]> adquiriente(RideDatos datos) {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"Tipo de identificación",
                etiquetaIdentificacion(datos.tipoIdentificacionComprador())});
        filas.add(new String[]{"Identificación", nuloVacio(datos.identificacionComprador())});
        filas.add(new String[]{"Razón social", nuloVacio(datos.razonSocialComprador())});
        agregar(filas, "Dirección", datos.direccionComprador());
        return filas;
    }

    private static void agregar(List<String[]> filas, String etiqueta, String valor) {
        if (valor != null && !valor.isBlank()) {
            filas.add(new String[]{etiqueta, valor});
        }
    }

    // ------------------------------------------------------------------
    // Cuerpo
    // ------------------------------------------------------------------

    private void bloque(Document doc, String titulo, List<String[]> filas) throws DocumentException {
        seccion(doc, titulo);
        PdfPTable tabla = new PdfPTable(new float[]{2f, 5f});
        tabla.setWidthPercentage(100);
        Font etiqueta = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, GRIS);
        Font valor = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
        for (String[] fila : filas) {
            PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(fila[0], etiqueta));
            celdaEtiqueta.setBorder(0);
            celdaEtiqueta.setPadding(2);
            tabla.addCell(celdaEtiqueta);

            PdfPCell celdaValor = new PdfPCell(new Phrase(fila[1], valor));
            celdaValor.setBorder(0);
            celdaValor.setPadding(2);
            tabla.addCell(celdaValor);
        }
        doc.add(tabla);
    }

    private void detalle(Document doc, RideDatos datos) throws DocumentException {
        seccion(doc, "DETALLE");
        PdfPTable tabla = new PdfPTable(new float[]{1.4f, 0.9f, 3.4f, 1.4f, 1.2f, 1.4f});
        tabla.setWidthPercentage(100);
        cabeceraTabla(tabla, "Código");
        cabeceraTabla(tabla, "Cant.");
        cabeceraTabla(tabla, "Descripción");
        cabeceraTabla(tabla, "P. unitario");
        cabeceraTabla(tabla, "Descuento");
        cabeceraTabla(tabla, "Total");

        Font celda = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
        for (RideDatos.Linea linea : datos.lineas()) {
            tabla.addCell(new Phrase(linea.codigo() == null ? "—" : linea.codigo(), celda));
            tabla.addCell(new Phrase(cantidad(linea.cantidad()), celda));
            tabla.addCell(new Phrase(nuloVacio(linea.descripcion()), celda));
            tabla.addCell(new Phrase(dinero(linea.precioUnitario()), celda));
            tabla.addCell(new Phrase(dinero(linea.descuento()), celda));
            tabla.addCell(new Phrase(dinero(linea.total()), celda));
        }
        tabla.setHeaderRows(1);
        doc.add(tabla);
    }

    private void impuestos(Document doc, RideDatos datos) throws DocumentException {
        seccion(doc, "IMPUESTOS");
        PdfPTable tabla = new PdfPTable(new float[]{1.4f, 2f, 2f});
        tabla.setWidthPercentage(45);
        tabla.setHorizontalAlignment(Element.ALIGN_LEFT);
        cabeceraTabla(tabla, "Tarifa");
        cabeceraTabla(tabla, "Base imponible");
        cabeceraTabla(tabla, "Valor");

        Font celda = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
        for (RideDatos.Impuesto impuesto : datos.impuestos()) {
            tabla.addCell(new Phrase(impuesto.tarifa() == null
                    ? "" : impuesto.tarifa().stripTrailingZeros().toPlainString() + " %", celda));
            tabla.addCell(new Phrase(dinero(impuesto.baseImponible()), celda));
            tabla.addCell(new Phrase(dinero(impuesto.valor()), celda));
        }
        tabla.setHeaderRows(1);
        doc.add(tabla);
    }

    private void totales(Document doc, RideDatos datos) throws DocumentException {
        PdfPTable tabla = new PdfPTable(new float[]{3f, 2f});
        tabla.setWidthPercentage(50);
        tabla.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
        Font negrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, PRIMARIO);

        filaTotal(tabla, "Total sin impuestos", datos.totalSinImpuestos(), normal, false);
        if (datos.totalDescuento() != null) {
            filaTotal(tabla, "Descuento", datos.totalDescuento(), normal, false);
        }
        for (RideDatos.Impuesto impuesto : datos.impuestos()) {
            filaTotal(tabla, "IVA " + (impuesto.tarifa() == null ? ""
                    : impuesto.tarifa().stripTrailingZeros().toPlainString() + " %"),
                    impuesto.valor(), normal, false);
        }
        filaTotal(tabla, "TOTAL", datos.importeTotal(), negrita, true);
        doc.add(tabla);
    }

    private static void filaTotal(PdfPTable tabla, String etiqueta, BigDecimal valor,
                                  Font font, boolean destacado) {
        PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(etiqueta, font));
        celdaEtiqueta.setBorder(0);
        celdaEtiqueta.setHorizontalAlignment(Element.ALIGN_RIGHT);
        celdaEtiqueta.setPadding(2);
        tabla.addCell(celdaEtiqueta);

        PdfPCell celdaValor = new PdfPCell(new Phrase(dinero(valor), font));
        celdaValor.setBorder(0);
        celdaValor.setHorizontalAlignment(Element.ALIGN_RIGHT);
        celdaValor.setPadding(2);
        if (destacado) {
            celdaValor.setBackgroundColor(new Color(235, 241, 250));
        }
        tabla.addCell(celdaValor);
    }

    private void pagos(Document doc, RideDatos datos) throws DocumentException {
        if (datos.pagos() == null || datos.pagos().isEmpty()) {
            return;
        }
        seccion(doc, "FORMA DE PAGO");
        PdfPTable tabla = new PdfPTable(new float[]{5f, 2f});
        tabla.setWidthPercentage(65);
        tabla.setHorizontalAlignment(Element.ALIGN_LEFT);
        cabeceraTabla(tabla, "Forma de pago");
        cabeceraTabla(tabla, "Valor");

        Font celda = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
        for (RideDatos.Pago pago : datos.pagos()) {
            tabla.addCell(new Phrase(etiquetaPago(pago.formaPago()), celda));
            tabla.addCell(new Phrase(dinero(pago.total()), celda));
        }
        tabla.setHeaderRows(1);
        doc.add(tabla);
    }

    private void adicionales(Document doc, RideDatos datos) throws DocumentException {
        if (datos.adicionales() == null || datos.adicionales().isEmpty()) {
            return;
        }
        seccion(doc, "INFORMACIÓN ADICIONAL");
        Font celda = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
        for (RideDatos.Campo campo : datos.adicionales()) {
            Paragraph linea = new Paragraph(campo.nombre() + ": " + nuloVacio(campo.valor()), celda);
            linea.setSpacingAfter(1);
            doc.add(linea);
        }
    }

    private void autorizacion(Document doc, ComprobanteElectronico comprobante, RideDatos datos)
            throws DocumentException {
        seccion(doc, "INFORMACIÓN DE LA AUTORIZACIÓN SRI");
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
        Font clave = FontFactory.getFont(FontFactory.COURIER, 8, Color.BLACK);

        doc.add(new Paragraph("N° de autorización: "
                + nuloVacio(comprobante.getAutorizacionNumero()), normal));
        doc.add(new Paragraph("Fecha de autorización: "
                + (comprobante.getAutorizacionFecha() == null
                ? "—" : FECHA_AUTORIZACION.format(
                comprobante.getAutorizacionFecha().atZone(ZoneId.of("UTC"))
                        .toLocalDateTime())), normal));
        doc.add(new Paragraph("Ambiente: " + etiquetaAmbiente(datos.ambiente())
                + "   ·   Tipo de emisión: " + etiquetaEmision(datos.tipoEmision()), normal));
        if (datos.claveAcceso() != null) {
            Paragraph acceso = new Paragraph("Clave de acceso:", normal);
            acceso.setSpacingBefore(4);
            doc.add(acceso);
            doc.add(new Paragraph(datos.claveAcceso(), clave));
        }
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static void seccion(Document doc, String titulo) throws DocumentException {
        Paragraph p = new Paragraph(titulo, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, PRIMARIO));
        p.setSpacingBefore(10);
        p.setSpacingAfter(3);
        doc.add(p);
    }

    private static void cabeceraTabla(PdfPTable tabla, String texto) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        PdfPCell celda = new PdfPCell(new Phrase(texto, font));
        celda.setBackgroundColor(PRIMARIO);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setPadding(4);
        tabla.addCell(celda);
    }

    private static String etiquetaPago(String codigo) {
        String etiqueta = FormaPagoCatalogo.etiqueta(codigo);
        return etiqueta != null ? etiqueta + " (" + codigo + ")"
                : (codigo == null ? "" : "Código " + codigo);
    }

    private static String etiquetaIdentificacion(String codigo) {
        if (codigo == null) {
            return "";
        }
        return switch (codigo) {
            case "04" -> "RUC";
            case "05" -> "CÉDULA";
            case "06" -> "PASAPORTE";
            case "07" -> "VENTA AL PÚBLICO";
            default -> codigo;
        };
    }

    private static String etiquetaAmbiente(String codigo) {
        if ("1".equals(codigo)) {
            return "PRUEBAS";
        }
        if ("2".equals(codigo)) {
            return "PRODUCCIÓN";
        }
        return nuloVacio(codigo);
    }

    private static String etiquetaEmision(String codigo) {
        if ("1".equals(codigo)) {
            return "NORMAL";
        }
        if ("2".equals(codigo)) {
            return "CONTINGENCIA";
        }
        return nuloVacio(codigo);
    }

    private static String dinero(BigDecimal valor) {
        return "$ " + (valor == null ? BigDecimal.ZERO : valor).setScale(2);
    }

    private static String cantidad(BigDecimal valor) {
        return valor == null ? "" : valor.stripTrailingZeros().toPlainString();
    }

    private static String nuloVacio(String valor) {
        return valor == null ? "" : valor;
    }
}
