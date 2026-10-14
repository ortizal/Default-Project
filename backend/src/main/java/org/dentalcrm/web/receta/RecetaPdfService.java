package org.dentalcrm.web.receta;

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
import org.dentalcrm.domain.receta.Receta;
import org.dentalcrm.domain.receta.RecetaLinea;
import org.dentalcrm.web.configuracion.ConsultorioService;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;

/**
 * PDF de la receta médica (cabecera de la clínica, pauta por medicamento
 * e instrucciones). Sigue el mismo estilo que {@code PlanPdfService}.
 */
@Service
public class RecetaPdfService {

    private static final Color PRIMARIO = new Color(20, 90, 160);
    private static final Color GRIS = new Color(90, 90, 90);

    private final ConsultorioService consultorioService;

    public RecetaPdfService(ConsultorioService consultorioService) {
        this.consultorioService = consultorioService;
    }

    public String nombreArchivo(Receta receta) {
        return "receta-" + receta.getId() + ".pdf";
    }

    public byte[] generar(Receta receta, String paciente) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            encabezado(doc);
            cabecera(doc, receta, paciente);
            tablaMedicamentos(doc, receta.getLineas());
            instrucciones(doc, receta.getLineas());
            if (receta.getMotivoAnulacion() != null) {
                anulacion(doc, receta);
            }

            doc.close();
            return out.toByteArray();
        } catch (DocumentException | java.io.IOException e) {
            throw new IllegalStateException("No se pudo generar el PDF de la receta", e);
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

    private void cabecera(Document doc, Receta receta, String paciente) throws DocumentException {
        Paragraph t = new Paragraph("Receta médica",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK));
        t.setAlignment(Element.ALIGN_CENTER);
        doc.add(t);

        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
        Paragraph datos = new Paragraph();
        datos.add(new Phrase("Paciente: " + paciente + "\n", normal));
        datos.add(new Phrase("Receta No. " + receta.getId() + "  ·  Fecha: " + receta.getFecha()
                + "  ·  Estado: " + estado(receta) + "\n", normal));
        if (receta.getOdontologo() != null) {
            String nombres = receta.getOdontologo().getNombres() == null
                    ? "" : receta.getOdontologo().getNombres().trim();
            String apellidos = receta.getOdontologo().getApellidos() == null
                    ? "" : receta.getOdontologo().getApellidos().trim();
            datos.add(new Phrase("Profesional: " + (nombres + " " + apellidos).trim() + "\n", normal));
        }
        if (receta.getDiagnostico() != null) {
            datos.add(new Phrase("Diagnóstico: " + receta.getDiagnostico(), normal));
        }
        doc.add(datos);

        if (receta.getObservaciones() != null && !receta.getObservaciones().isBlank()) {
            doc.add(new Paragraph(receta.getObservaciones(),
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, GRIS)));
        }
        doc.add(new Paragraph(" "));
    }

    private void tablaMedicamentos(Document doc, List<RecetaLinea> lineas) throws DocumentException {
        PdfPTable tabla = new PdfPTable(new float[]{0.5f, 1.8f, 1.1f, 1.2f, 1f, 1.1f, 1.3f, 1f});
        tabla.setWidthPercentage(100);
        cabeceraTabla(tabla, "#");
        cabeceraTabla(tabla, "Medicamento");
        cabeceraTabla(tabla, "Conc.");
        cabeceraTabla(tabla, "Presentación");
        cabeceraTabla(tabla, "Dosis");
        cabeceraTabla(tabla, "Vía");
        cabeceraTabla(tabla, "Frecuencia");
        cabeceraTabla(tabla, "Duración");

        Font celda = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
        boolean marcado = false;
        for (RecetaLinea linea : lineas) {
            for (String texto : List.of(String.valueOf(linea.getNumero()), linea.getMedicamento(),
                    texto(linea.getConcentracion()), texto(linea.getPresentacion()),
                    linea.getDosis(), via(linea), texto(linea.getFrecuencia()),
                    texto(linea.getDuracion()))) {
                PdfPCell cell = new PdfPCell(new Phrase(texto, celda));
                cell.setPadding(5);
                if (marcado) {
                    cell.setGrayFill(0.94f);
                }
                tabla.addCell(cell);
            }
            marcado = !marcado;
        }
        doc.add(tabla);
        doc.add(new Paragraph(" "));
    }

    private void instrucciones(Document doc, List<RecetaLinea> lineas) throws DocumentException {
        List<RecetaLinea> notas = lineas.stream()
                .filter(l -> l.getInstrucciones() != null && !l.getInstrucciones().isBlank())
                .toList();
        if (notas.isEmpty()) {
            return;
        }
        doc.add(new Paragraph("Instrucciones",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, PRIMARIO)));
        Paragraph p = new Paragraph();
        for (RecetaLinea linea : notas) {
            p.add(new Phrase("• " + linea.getMedicamento() + ": " + linea.getInstrucciones() + "\n",
                    FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK)));
        }
        doc.add(p);
    }

    private void anulacion(Document doc, Receta receta) throws DocumentException {
        Font rojo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(176, 46, 46));
        doc.add(new Paragraph("Receta ANULADA — motivo: " + receta.getMotivoAnulacion(), rojo));
    }

    private void cabeceraTabla(PdfPTable tabla, String texto) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        PdfPCell celda = new PdfPCell(new Phrase(texto, font));
        celda.setBackgroundColor(PRIMARIO);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setPadding(5);
        tabla.addCell(celda);
    }

    private String via(RecetaLinea linea) {
        return linea.getVia() == null ? "—" : linea.getVia().name().toLowerCase(Locale.ROOT);
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor;
    }

    private String estado(Receta receta) {
        return receta.getEstado() == null ? "" : receta.getEstado().name();
    }
}
