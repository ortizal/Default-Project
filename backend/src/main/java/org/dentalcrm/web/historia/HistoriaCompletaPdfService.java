package org.dentalcrm.web.historia;

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
import org.dentalcrm.web.configuracion.ConsultorioService;
import org.dentalcrm.web.historia.dto.HistoriaCompletaResponse;
import org.dentalcrm.web.historia.dto.HistoriaCompletaResponse.Fila;
import org.dentalcrm.web.historia.dto.HistoriaCompletaResponse.Seccion;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * PDF de la historia clínica completa (plan §4.17): cabecera de la
 * clínica, datos del paciente, resumen y cada sección como tabla.
 *
 * <p>El servicio no conoce el modelo de dominio: sólo recibe el DTO ya
 * construido por {@link HistoriaCompletaService}, de modo que el render es
 * puro y fácil de probar.
 */
@Service
public class HistoriaCompletaPdfService {

    private static final Color PRIMARIO = new Color(20, 90, 160);
    private static final Color GRIS = new Color(90, 90, 90);

    private final ConsultorioService consultorioService;

    public HistoriaCompletaPdfService(ConsultorioService consultorioService) {
        this.consultorioService = consultorioService;
    }

    public String nombreArchivo(Long pacienteId) {
        return "historia-clinica-completa-" + pacienteId + ".pdf";
    }

    public byte[] generar(HistoriaCompletaResponse datos) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            encabezado(doc);
            paciente(doc, datos);
            resumen(doc, datos.resumen());
            for (Seccion seccion : datos.secciones()) {
                seccion(doc, seccion);
            }

            doc.close();
            return out.toByteArray();
        } catch (DocumentException | java.io.IOException e) {
            throw new IllegalStateException("No se pudo generar la historia clínica en PDF", e);
        }
    }

    // ------------------------------------------------------------------

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

    private void paciente(Document doc, HistoriaCompletaResponse datos) throws DocumentException {
        Paragraph t = new Paragraph("Historia clínica completa",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK));
        t.setAlignment(Element.ALIGN_CENTER);
        doc.add(t);

        var info = datos.paciente();
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
        Paragraph bloque = new Paragraph();
        bloque.add(new Phrase("Paciente: " + info.nombre() + "\n", normal));
        bloque.add(new Phrase("Cédula: " + info.cedula() + "  ·  Edad: " + info.edad()
                + "  ·  Tel: " + info.telefono() + "  ·  Correo: " + info.correo() + "\n", normal));
        bloque.add(new Phrase("Historia clínica: " + info.historia()
                + "  ·  Odontólogo: " + info.odontologo()
                + "  ·  Emitido: " + info.fechaEmision(), normal));
        doc.add(bloque);
        doc.add(new Paragraph(" "));
    }

    private void resumen(Document doc, List<String> lineas) throws DocumentException {
        if (lineas == null || lineas.isEmpty()) {
            return;
        }
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, PRIMARIO);
        doc.add(new Paragraph("Resumen", bold));
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
        for (String linea : lineas) {
            Paragraph p = new Paragraph("· " + linea, normal);
            p.setSpacingAfter(2);
            doc.add(p);
        }
        doc.add(new Paragraph(" "));
    }

    private void seccion(Document doc, Seccion seccion) throws DocumentException {
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, PRIMARIO);
        doc.add(new Paragraph(seccion.titulo(), bold));

        if (seccion.filas() == null || seccion.filas().isEmpty()) {
            Paragraph vacia = new Paragraph("Sin registros.",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, GRIS));
            vacia.setSpacingAfter(8);
            doc.add(vacia);
            return;
        }

        int columnas = Math.max(1, seccion.columnas().size());
        PdfPTable tabla = new PdfPTable(columnas);
        tabla.setWidthPercentage(100);
        for (String columna : seccion.columnas()) {
            cabeceraTabla(tabla, columna);
        }
        Font celda = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
        for (Fila fila : seccion.filas()) {
            List<String> celdas = fila.celdas();
            for (int i = 0; i < columnas; i++) {
                String valor = i < celdas.size() ? celdas.get(i) : "";
                tabla.addCell(new Phrase(valor == null ? "" : valor, celda));
            }
        }
        doc.add(tabla);
        doc.add(new Paragraph(" "));
    }

    private void cabeceraTabla(PdfPTable tabla, String texto) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        PdfPCell celda = new PdfPCell(new Phrase(texto, font));
        celda.setBackgroundColor(PRIMARIO);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setPadding(4);
        tabla.addCell(celda);
    }
}
