package org.dentalcrm.web.agenda;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.dentalcrm.domain.horario.HorarioOdontologo;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Genera el horario de atención de un odontólogo en PDF (adjunto de WhatsApp
 * y descarga desde el panel de agenda).
 */
@Service
public class HorarioPdfService {

    private static final String[] DIAS = {"Lunes", "Martes", "Miércoles", "Jueves",
            "Viernes", "Sábado", "Domingo"};

    private final String clinicaNombre;
    private final String clinicaDireccion;
    private final String clinicaTelefono;

    public HorarioPdfService(@Value("${app.clinica.nombre:Clínica Dental}") String clinicaNombre,
                             @Value("${app.clinica.direccion:}") String clinicaDireccion,
                             @Value("${app.clinica.telefono:}") String clinicaTelefono) {
        this.clinicaNombre = clinicaNombre;
        this.clinicaDireccion = clinicaDireccion;
        this.clinicaTelefono = clinicaTelefono;
    }

    public String nombreArchivo(Odontologo doctor) {
        String apellido = doctor.getApellidos() == null ? "doctor" : doctor.getApellidos().toLowerCase();
        String sinAcentos = java.text.Normalizer.normalize(apellido, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return "horario-" + sinAcentos.replaceAll("[^a-z0-9-]", "") + ".pdf";
    }

    public byte[] generar(Odontologo doctor, List<HorarioOdontologo> horarios) {
        return generar(doctor, horarios, null, null);
    }

    /**
     * {@code proximosCupos} es una lista de líneas ya formateadas
     * ("• jueves 18/09 a las 09:00") que se imprime al final del documento.
     */
    public byte[] generar(Odontologo doctor, List<HorarioOdontologo> horarios, List<String> proximosCupos) {
        return generar(doctor, horarios, proximosCupos, null);
    }

    /**
     * {@code dias} detalla, día por día, qué horas quedan libres y qué hay
     * ocupado (citas y bloqueos), para que el paciente vea de un vistazo dónde
     * encaja su cita y no tenga que interpretar el horario semanal.
     */
    public byte[] generar(Odontologo doctor, List<HorarioOdontologo> horarios,
                          List<String> proximosCupos, List<DiaDisponibilidad> dias) {
        Document doc = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            encabezado(doc);
            titulo(doc, doctor);
            tablaHorarios(doc, horarios);
            if (dias != null && !dias.isEmpty()) {
                tablaDisponibilidad(doc, dias);
            }
            if (proximosCupos != null && !proximosCupos.isEmpty()) {
                proximosCupos(doc, proximosCupos);
            }
            doc.close();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF del horario: " + e.getMessage(), e);
        }
        return out.toByteArray();
    }

    /** Disponibilidad de un día concreto para la tabla "Libre / Ocupado". */
    public record DiaDisponibilidad(String dia, String libre, String ocupado) {
    }

    private void tablaDisponibilidad(Document doc, List<DiaDisponibilidad> dias) {
        doc.add(new Paragraph("Disponibilidad — próximos " + dias.size() + " días",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(20, 90, 160))));

        PdfPTable tabla = new PdfPTable(new float[]{3, 4.5f, 4.5f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(6);
        tabla.setSpacingAfter(10);

        for (String encabezado : List.of("Día", "Horas libres", "Horas ocupadas")) {
            PdfPCell c = new PdfPCell(new Phrase(encabezado, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE)));
            c.setBackgroundColor(new Color(20, 90, 160));
            c.setPadding(6);
            tabla.addCell(c);
        }

        boolean marcado = false;
        Font fuente = FontFactory.getFont(FontFactory.HELVETICA, 9);
        for (DiaDisponibilidad d : dias) {
            PdfPCell dCell = new PdfPCell(new Phrase(d.dia(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
            PdfPCell libreCell = new PdfPCell(new Phrase(d.libre(), fuente));
            PdfPCell ocupadaCell = new PdfPCell(new Phrase(d.ocupado(), fuente));
            for (PdfPCell celda : List.of(dCell, libreCell, ocupadaCell)) {
                celda.setPadding(5);
                if (marcado) {
                    celda.setGrayFill(0.94f);
                }
            }
            tabla.addCell(dCell);
            tabla.addCell(libreCell);
            tabla.addCell(ocupadaCell);
            marcado = !marcado;
        }
        doc.add(tabla);
    }

    private void encabezado(Document doc) {
        Paragraph p = new Paragraph(clinicaNombre, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, new Color(20, 90, 160)));
        p.setAlignment(Element.ALIGN_CENTER);
        doc.add(p);
        StringBuilder datos = new StringBuilder();
        if (clinicaDireccion != null && !clinicaDireccion.isBlank()) {
            datos.append(clinicaDireccion);
        }
        if (clinicaTelefono != null && !clinicaTelefono.isBlank()) {
            if (datos.length() > 0) {
                datos.append("  ·  ");
            }
            datos.append("Tel: ").append(clinicaTelefono);
        }
        if (datos.length() > 0) {
            Paragraph d = new Paragraph(datos.toString(),
                    FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(90, 90, 90)));
            d.setAlignment(Element.ALIGN_CENTER);
            doc.add(d);
        }
        doc.add(new Paragraph(" "));
    }

    private void titulo(Document doc, Odontologo doctor) {
        Font fuente = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
        Paragraph p = new Paragraph("Horario de atención — Dr. "
                + doctor.getNombres() + " " + doctor.getApellidos(), fuente);
        if (doctor.getEspecialidad() != null && !doctor.getEspecialidad().isBlank()) {
            p.add(new Phrase("  (" + doctor.getEspecialidad() + ")",
                    FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(80, 80, 80))));
        }
        doc.add(p);
        doc.add(new Paragraph(" "));
    }

    private void tablaHorarios(Document doc, List<HorarioOdontologo> horarios) {
        PdfPTable tabla = new PdfPTable(new float[]{5, 3, 3});
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(6);
        tabla.setSpacingAfter(10);

        PdfPCell c = new PdfPCell(new Phrase("Día", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE)));
        c.setBackgroundColor(new Color(20, 90, 160));
        c.setPadding(6);
        tabla.addCell(c);
        PdfPCell c2 = new PdfPCell(new Phrase("Desde", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE)));
        c2.setBackgroundColor(new Color(20, 90, 160));
        c2.setPadding(6);
        tabla.addCell(c2);
        PdfPCell c3 = new PdfPCell(new Phrase("Hasta", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE)));
        c3.setBackgroundColor(new Color(20, 90, 160));
        c3.setPadding(6);
        tabla.addCell(c3);

        boolean marcado = false;
        for (DayOfWeek dia : DayOfWeek.values()) {
            String nombre = dia.getDisplayName(TextStyle.FULL, new Locale("es", "EC"));
            LocalTime inicio = null;
            LocalTime fin = null;
            for (HorarioOdontologo h : horarios) {
                if (h.getDiaSemana() == dia.getValue() && "ACTIVO".equals(h.getEstado())) {
                    inicio = h.getHoraInicio();
                    fin = h.getHoraFin();
                    break;
                }
            }
            PdfPCell dCell = new PdfPCell(new Phrase(nombre, FontFactory.getFont(FontFactory.HELVETICA, 11)));
            PdfPCell iCell = new PdfPCell(new Phrase(inicio == null ? "—" : inicio.toString(),
                    FontFactory.getFont(FontFactory.HELVETICA, 11)));
            PdfPCell fCell = new PdfPCell(new Phrase(fin == null ? "—" : fin.toString(),
                    FontFactory.getFont(FontFactory.HELVETICA, 11)));
            if (marcado) {
                for (PdfPCell celda : List.of(dCell, iCell, fCell)) {
                    celda.setGrayFill(0.94f);
                }
            }
            for (PdfPCell celda : List.of(dCell, iCell, fCell)) {
                celda.setPadding(5);
            }
            tabla.addCell(dCell);
            tabla.addCell(iCell);
            tabla.addCell(fCell);
            marcado = !marcado;
        }
        doc.add(tabla);
    }

    private void proximosCupos(Document doc, List<String> cupos) {
        doc.add(new Paragraph("Próximos cupos disponibles",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(20, 90, 160))));
        Paragraph p = new Paragraph();
        for (String cupo : cupos) {
            p.add(new Phrase(cupo + "\n", FontFactory.getFont(FontFactory.HELVETICA, 11)));
        }
        doc.add(p);
    }
}