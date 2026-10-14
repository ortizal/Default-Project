package org.dentalcrm.web.plan;

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
import org.dentalcrm.domain.plan.PlanLinea;
import org.dentalcrm.domain.plan.PlanTratamiento;
import org.dentalcrm.domain.plan.Presupuesto;
import org.dentalcrm.web.configuracion.ConsultorioService;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * PDF del plan de tratamiento (cabecera de la clínica, líneas y totales,
 * más el presupuesto vigente si existe). Sigue el mismo estilo que
 * {@code HorarioPdfService}.
 */
@Service
public class PlanPdfService {

    private static final Color PRIMARIO = new Color(20, 90, 160);
    private static final Color GRIS = new Color(90, 90, 90);

    private final ConsultorioService consultorioService;

    public PlanPdfService(ConsultorioService consultorioService) {
        this.consultorioService = consultorioService;
    }

    public String nombreArchivo(PlanTratamiento plan) {
        return "plan-tratamiento-" + plan.getId() + ".pdf";
    }

    public byte[] generar(PlanTratamiento plan, String paciente, String odontologo,
                          List<PlanLinea> lineas, Presupuesto vigente) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            encabezado(doc);
            cabecera(doc, plan, paciente, odontologo);
            tablaLineas(doc, lineas);
            totales(doc, plan);
            if (vigente != null) {
                presupuesto(doc, vigente);
            }

            doc.close();
            return out.toByteArray();
        } catch (DocumentException | java.io.IOException e) {
            throw new IllegalStateException("No se pudo generar el PDF del plan", e);
        }
    }

    private void encabezado(Document doc) {
        var consultorio = consultorioService == null ? null : consultorioService.obtener();
        String nombre = consultorio == null ? "Clínica Dental" : consultorio.nombre();
        Paragraph p = new Paragraph(nombre,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, PRIMARIO));
        p.setAlignment(Element.ALIGN_CENTER);
        doc.add(p);

        StringBuilder datos = new StringBuilder();
        if (consultorio != null && consultorio.direccion() != null
                && !consultorio.direccion().isBlank()) {
            datos.append(consultorio.direccion());
        }
        if (consultorio != null && consultorio.telefono() != null
                && !consultorio.telefono().isBlank()) {
            if (datos.length() > 0) {
                datos.append("  ·  ");
            }
            datos.append("Tel: ").append(consultorio.telefono());
        }
        if (datos.length() > 0) {
            Paragraph d = new Paragraph(datos.toString(),
                    FontFactory.getFont(FontFactory.HELVETICA, 10, GRIS));
            d.setAlignment(Element.ALIGN_CENTER);
            doc.add(d);
        }
        doc.add(new Paragraph(" "));
    }

    private void cabecera(Document doc, PlanTratamiento plan, String paciente, String odontologo)
            throws DocumentException {
        Paragraph t = new Paragraph("Plan de tratamiento",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK));
        t.setAlignment(Element.ALIGN_CENTER);
        doc.add(t);

        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
        Paragraph datos = new Paragraph();
        datos.add(new Phrase("Paciente: " + paciente + "\n", normal));
        datos.add(new Phrase("Plan No. " + plan.getId() + "  ·  Fecha: " + plan.getFecha()
                + "  ·  Estado: " + estado(plan) + "\n", normal));
        if (odontologo != null && !odontologo.isBlank()) {
            datos.add(new Phrase("Profesional: " + odontologo + "\n", normal));
        }
        datos.add(new Phrase("Título: " + plan.getTitulo(), normal));
        doc.add(datos);

        if (plan.getObservaciones() != null && !plan.getObservaciones().isBlank()) {
            Paragraph obs = new Paragraph(plan.getObservaciones(),
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, GRIS));
            doc.add(obs);
        }
        doc.add(new Paragraph(" "));
    }

    private void tablaLineas(Document doc, List<PlanLinea> lineas) throws DocumentException {
        PdfPTable tabla = new PdfPTable(new float[]{0.6f, 0.6f, 1f, 4.2f, 1.3f, 1f, 1.6f, 1f, 1.8f});
        tabla.setWidthPercentage(100);
        cabeceraTabla(tabla, "#");
        cabeceraTabla(tabla, "Fase");
        cabeceraTabla(tabla, "Diente");
        cabeceraTabla(tabla, "Procedimiento");
        cabeceraTabla(tabla, "Prioridad");
        cabeceraTabla(tabla, "Ses.");
        cabeceraTabla(tabla, "Precio");
        cabeceraTabla(tabla, "Dto %");
        cabeceraTabla(tabla, "Subtotal");

        Font celda = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
        for (PlanLinea linea : lineas) {
            tabla.addCell(new Phrase(String.valueOf(linea.getNumero()), celda));
            tabla.addCell(new Phrase(String.valueOf(linea.getFase()), celda));
            tabla.addCell(new Phrase(textoDiente(linea), celda));
            tabla.addCell(new Phrase(linea.getProcedimiento(), celda));
            tabla.addCell(new Phrase(linea.getPrioridad().name(), celda));
            tabla.addCell(new Phrase(String.valueOf(linea.getSesiones()), celda));
            tabla.addCell(new Phrase(moneda(linea.getPrecioUnitario()), celda));
            tabla.addCell(new Phrase(linea.getDescuentoPct().stripTrailingZeros().toPlainString(), celda));
            tabla.addCell(new Phrase(moneda(linea.getSubtotal()), celda));
        }
        doc.add(tabla);
        doc.add(new Paragraph(" "));
    }

    private void cabeceraTabla(PdfPTable tabla, String texto) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        PdfPCell celda = new PdfPCell(new Phrase(texto, font));
        celda.setBackgroundColor(PRIMARIO);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setPadding(4);
        tabla.addCell(celda);
    }

    private void totales(Document doc, PlanTratamiento plan) throws DocumentException {
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
        Paragraph t = new Paragraph();
        t.setAlignment(Element.ALIGN_RIGHT);
        t.add(new Phrase("Total del plan: " + moneda(plan.getTotal()) + "   ", bold));
        doc.add(t);
    }

    private void presupuesto(Document doc, Presupuesto vigente) throws DocumentException {
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9, GRIS);
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, GRIS);
        Paragraph p = new Paragraph();
        p.add(new Phrase("Presupuesto No. " + vigente.getNumero() + " ("
                + vigente.getEstado() + ") — total " + moneda(vigente.getTotal()), bold));
        p.add(new Phrase("  ·  " + vigente.getFecha(), normal));
        doc.add(p);
        if (vigente.getFirmante() != null) {
            Paragraph firma = new Paragraph();
            firma.add(new Phrase("Aceptado por " + vigente.getFirmante()
                    + (vigente.getAceptadoEn() == null ? "" : " el " + vigente.getAceptadoEn().toString()
                    .replace('T', ' ').substring(0, 16)), normal));
            doc.add(firma);
            if (vigente.getFirmaHash() != null) {
                Paragraph hash = new Paragraph("Firma (SHA-256): " + vigente.getFirmaHash(),
                        FontFactory.getFont(FontFactory.HELVETICA, 7, GRIS));
                doc.add(hash);
            }
        }
    }

    private String textoDiente(PlanLinea linea) {
        if (linea.getDiente() == null) {
            return "—";
        }
        return linea.getSuperficie() == null
                ? String.valueOf(linea.getDiente())
                : linea.getDiente() + " · " + linea.getSuperficie().toLowerCase(Locale.ROOT);
    }

    private String estado(PlanTratamiento plan) {
        return plan.getEstado() == null ? "" : plan.getEstado().name().replace('_', ' ');
    }

    private String moneda(BigDecimal valor) {
        return "$ " + (valor == null ? BigDecimal.ZERO : valor).setScale(2);
    }
}
