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
import org.dentalcrm.domain.historia.ExamenPeriodontal;
import org.dentalcrm.domain.historia.PeriodontogramaDiente;
import org.dentalcrm.domain.historia.PeriodontogramaSitio;
import org.dentalcrm.domain.historia.SitioPeriodontal;
import org.dentalcrm.web.configuracion.ConsultorioService;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PDF del examen periodontal (plan §4.11): matriz por diente con las
 * mediciones de los cuatro sitios, movilidad y furcación, más resumen
 * clínico. Mismo estilo que {@code PlanPdfService}.
 */
@Service
public class PeriodontogramaPdfService {

    private static final Color PRIMARIO = new Color(20, 90, 160);
    private static final Color GRIS = new Color(90, 90, 90);
    private static final Color ROJO = new Color(176, 46, 46);

    private final ConsultorioService consultorioService;

    public PeriodontogramaPdfService(ConsultorioService consultorioService) {
        this.consultorioService = consultorioService;
    }

    public String nombreArchivo(ExamenPeriodontal examen) {
        return "periodontograma-" + examen.getHistoria().getId() + "-v" + examen.getVersion() + ".pdf";
    }

    public byte[] generar(ExamenPeriodontal examen, String paciente,
                          List<PeriodontogramaSitio> sitios, List<PeriodontogramaDiente> dientes) {
        Map<Integer, Map<SitioPeriodontal, PeriodontogramaSitio>> porDiente = indexar(sitios);
        Map<Integer, PeriodontogramaDiente> piezas = new HashMap<>();
        for (PeriodontogramaDiente d : dientes) {
            piezas.put(d.getDiente(), d);
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            encabezado(doc);
            cabecera(doc, examen, paciente);
            resumen(doc, sitios, piezas);
            matriz(doc, porDiente, piezas);
            notas(doc, piezas);
            leyenda(doc);

            doc.close();
            return out.toByteArray();
        } catch (DocumentException | java.io.IOException e) {
            throw new IllegalStateException("No se pudo generar el PDF del examen periodontal", e);
        }
    }

    private Map<Integer, Map<SitioPeriodontal, PeriodontogramaSitio>> indexar(
            List<PeriodontogramaSitio> sitios) {
        Map<Integer, Map<SitioPeriodontal, PeriodontogramaSitio>> mapa = new HashMap<>();
        for (PeriodontogramaSitio s : sitios) {
            mapa.computeIfAbsent(s.getDiente(), k -> new EnumMap<>(SitioPeriodontal.class))
                    .put(s.getSitio(), s);
        }
        return mapa;
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

    private void cabecera(Document doc, ExamenPeriodontal examen, String paciente)
            throws DocumentException {
        Paragraph t = new Paragraph("Examen periodontal",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK));
        t.setAlignment(Element.ALIGN_CENTER);
        doc.add(t);

        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
        Paragraph datos = new Paragraph();
        datos.add(new Phrase("Paciente: " + paciente + "\n", normal));
        datos.add(new Phrase("Fecha: " + examen.getFecha() + "  ·  Versión: "
                + examen.getVersion() + "\n", normal));
        if (examen.getOdontologo() != null) {
            String nombres = examen.getOdontologo().getNombres() == null
                    ? "" : examen.getOdontologo().getNombres().trim();
            String apellidos = examen.getOdontologo().getApellidos() == null
                    ? "" : examen.getOdontologo().getApellidos().trim();
            datos.add(new Phrase("Profesional: " + (nombres + " " + apellidos).trim() + "\n", normal));
        }
        if (examen.getObservaciones() != null && !examen.getObservaciones().isBlank()) {
            datos.add(new Phrase("Observaciones: " + examen.getObservaciones(), normal));
        }
        doc.add(datos);
        doc.add(new Paragraph(" "));
    }

    private void resumen(Document doc, List<PeriodontogramaSitio> sitios,
                         Map<Integer, PeriodontogramaDiente> piezas) throws DocumentException {
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, PRIMARIO);
        doc.add(new Paragraph("Resumen", bold));

        int conSangrado = 0;
        int conSupuracion = 0;
        int conPlaca = 0;
        int conMovilidad = 0;
        int conFurcacion = 0;
        BigDecimal suma = BigDecimal.ZERO;
        int conteo = 0;
        BigDecimal maxSondaje = BigDecimal.ZERO;
        for (PeriodontogramaSitio s : sitios) {
            if (s.isSangrado()) conSangrado++;
            if (s.isPlaca()) conPlaca++;
            if (s.isSupuracion()) conSupuracion++;
            if (s.getSondajeMm() != null) {
                suma = suma.add(s.getSondajeMm());
                conteo++;
                if (s.getSondajeMm().compareTo(maxSondaje) > 0) {
                    maxSondaje = s.getSondajeMm();
                }
            }
        }
        for (PeriodontogramaDiente d : piezas.values()) {
            if (d.getMovilidad() != null && d.getMovilidad() > 0) conMovilidad++;
            if (d.getFurcacion() != null && d.getFurcacion() > 0) conFurcacion++;
        }

        String promedio = conteo == 0 ? "—"
                : suma.divide(BigDecimal.valueOf(conteo), 1, RoundingMode.HALF_UP)
                        .stripTrailingZeros().toPlainString();

        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
        Paragraph p = new Paragraph();
        p.add(new Phrase("Sitios medidos: " + sitios.size() + "\n", normal));
        p.add(new Phrase("Promedio de sondaje: " + promedio + " mm  ·  Sondaje máximo: "
                + (maxSondaje.signum() > 0
                ? maxSondaje.stripTrailingZeros().toPlainString() : "0") + " mm\n", normal));
        p.add(new Phrase("Sitios con sangrado: " + conSangrado + "  ·  con placa: " + conPlaca
                + "  ·  con supuración: " + conSupuracion + "\n", normal));
        p.add(new Phrase("Piezas con movilidad: " + conMovilidad + "  ·  con furcación: "
                + conFurcacion, normal));
        doc.add(p);
        doc.add(new Paragraph(" "));
    }

    private void matriz(Document doc, Map<Integer, Map<SitioPeriodontal, PeriodontogramaSitio>> porDiente,
                        Map<Integer, PeriodontogramaDiente> piezas) throws DocumentException {
        PdfPTable tabla = new PdfPTable(new float[]{0.9f, 1.6f, 1.6f, 1.6f, 1.6f, 0.7f, 0.7f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(4);
        cabeceraTabla(tabla, "Diente");
        cabeceraTabla(tabla, "Vestibular");
        cabeceraTabla(tabla, "Mesial");
        cabeceraTabla(tabla, "Distal");
        cabeceraTabla(tabla, "Lingual");
        cabeceraTabla(tabla, "Mov.");
        cabeceraTabla(tabla, "Furc.");

        Font celda = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
        boolean marcado = false;
        for (int diente : ordenados(porDiente, piezas)) {
            Map<SitioPeriodontal, PeriodontogramaSitio> sitios = porDiente.get(diente);
            PeriodontogramaDiente pieza = piezas.get(diente);

            PdfPCell dCell = new PdfPCell(new Phrase(String.valueOf(diente),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK)));
            dCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            dCell.setPadding(4);
            if (marcado) dCell.setGrayFill(0.94f);
            tabla.addCell(dCell);

            for (SitioPeriodontal sitio : new SitioPeriodontal[]{
                    SitioPeriodontal.VESTIBULAR, SitioPeriodontal.MESIAL,
                    SitioPeriodontal.DISTAL, SitioPeriodontal.LINGUAL}) {
                PeriodontogramaSitio s = sitios == null ? null : sitios.get(sitio);
                boolean alerta = s != null && (s.isSangrado() || s.isSupuracion());
                Font fuenteCelda = alerta
                        ? FontFactory.getFont(FontFactory.HELVETICA, 8, ROJO)
                        : celda;
                PdfPCell cell = new PdfPCell(new Phrase(celdaSitio(s), fuenteCelda));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(4);
                if (marcado) cell.setGrayFill(0.94f);
                tabla.addCell(cell);
            }

            tabla.addCell(celdaTexto(pieza == null || pieza.getMovilidad() == null
                    ? "—" : String.valueOf(pieza.getMovilidad()), celda, marcado));
            tabla.addCell(celdaTexto(pieza == null || pieza.getFurcacion() == null
                    ? "—" : String.valueOf(pieza.getFurcacion()), celda, marcado));
            marcado = !marcado;
        }
        doc.add(tabla);
        doc.add(new Paragraph(" "));
    }

    private void notas(Document doc, Map<Integer, PeriodontogramaDiente> piezas) throws DocumentException {
        List<String> lines = piezas.values().stream()
                .filter(d -> d.getNotas() != null && !d.getNotas().isBlank())
                .sorted((a, b) -> Integer.compare(a.getDiente(), b.getDiente()))
                .map(d -> "• Pieza " + d.getDiente() + ": " + d.getNotas())
                .toList();
        if (lines.isEmpty()) {
            return;
        }
        doc.add(new Paragraph("Notas por pieza",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, PRIMARIO)));
        Paragraph p = new Paragraph();
        for (String line : lines) {
            p.add(new Phrase(line + "\n", FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK)));
        }
        doc.add(p);
        doc.add(new Paragraph(" "));
    }

    private void leyenda(Document doc) throws DocumentException {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, GRIS);
        doc.add(new Paragraph("Cada celda muestra sondaje / margen / inserción en mm. "
                + "Letras finales: S = sangrado, P = placa, U = supuración. "
                + "Mov.: movilidad 0–3. Furc.: furcación 0–3. "
                + "Las celdas en rojo indican sangrado o supuración.", font));
    }

    private String celdaSitio(PeriodontogramaSitio s) {
        if (s == null) {
            return "—";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(num(s.getSondajeMm())).append('/').append(num(s.getMargenMm()))
                .append('/').append(num(s.getInsercionMm()));
        if (s.isSangrado()) sb.append(" S");
        if (s.isPlaca()) sb.append(" P");
        if (s.isSupuracion()) sb.append(" U");
        return sb.toString();
    }

    private String num(BigDecimal v) {
        return v == null ? "–" : v.stripTrailingZeros().toPlainString();
    }

    private PdfPCell celdaTexto(String texto, Font font, boolean marcado) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(4);
        if (marcado) cell.setGrayFill(0.94f);
        return cell;
    }

    private List<Integer> ordenados(Map<Integer, Map<SitioPeriodontal, PeriodontogramaSitio>> porDiente,
                                    Map<Integer, PeriodontogramaDiente> piezas) {
        java.util.TreeSet<Integer> todos = new java.util.TreeSet<>(porDiente.keySet());
        todos.addAll(piezas.keySet());
        return List.copyOf(todos);
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
