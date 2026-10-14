package org.dentalcrm.web.consentimiento;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.dentalcrm.domain.consentimiento.Consentimiento;
import org.dentalcrm.domain.consentimiento.EstadoConsentimiento;
import org.dentalcrm.web.configuracion.ConsultorioService;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * PDF del consentimiento informado: texto congelado, bloque de firma con
 * hash SHA-256 y, si existe, la firma manuscrita. Los firmados salen sin
 * marcas; los pendientes se sellan como BORRADOR.
 */
@Service
public class ConsentimientoPdfService {

    private static final Color PRIMARIO = new Color(20, 90, 160);
    private static final Color GRIS = new Color(90, 90, 90);
    private static final Color ROJO = new Color(176, 46, 46);

    private final ConsultorioService consultorioService;

    public ConsentimientoPdfService(ConsultorioService consultorioService) {
        this.consultorioService = consultorioService;
    }

    public String nombreArchivo(Consentimiento c) {
        return "consentimiento-" + c.getId() + ".pdf";
    }

    public byte[] generar(Consentimiento c, String paciente) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 44, 44, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            encabezado(doc);
            cabecera(doc, c, paciente);
            cuerpo(doc, c.getContenido());
            if (c.getEstado() == EstadoConsentimiento.FIRMADO) {
                bloqueFirma(doc, c);
            } else {
                Font aviso = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, ROJO);
                doc.add(new Paragraph("BORRADOR SIN FIRMAR", aviso));
            }

            doc.close();
            return out.toByteArray();
        } catch (DocumentException | java.io.IOException e) {
            throw new IllegalStateException("No se pudo generar el PDF del consentimiento", e);
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

    private void cabecera(Document doc, Consentimiento c, String paciente) throws DocumentException {
        Paragraph t = new Paragraph(c.getTitulo(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.BLACK));
        t.setAlignment(Element.ALIGN_CENTER);
        doc.add(t);

        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
        Paragraph datos = new Paragraph();
        datos.add(new Phrase("Paciente: " + paciente + "\n", normal));
        datos.add(new Phrase("Consentimiento No. " + c.getId() + "  ·  Fecha: " + c.getFecha()
                + "  ·  Plantilla: " + c.getPlantillaCodigo() + " v" + c.getPlantillaVersion() + "\n", normal));
        if (c.getOdontologo() != null) {
            String nombres = c.getOdontologo().getNombres() == null
                    ? "" : c.getOdontologo().getNombres().trim();
            String apellidos = c.getOdontologo().getApellidos() == null
                    ? "" : c.getOdontologo().getApellidos().trim();
            datos.add(new Phrase("Profesional: " + (nombres + " " + apellidos).trim() + "\n", normal));
        }
        if (c.getTutorNombre() != null && !c.getTutorNombre().isBlank()) {
            StringBuilder tutor = new StringBuilder(c.getTutorNombre());
            if (c.getTutorParentesco() != null && !c.getTutorParentesco().isBlank()) {
                tutor.append(" (").append(c.getTutorParentesco()).append(")");
            }
            if (c.getTutorTelefono() != null && !c.getTutorTelefono().isBlank()) {
                tutor.append("  ·  Tel: ").append(c.getTutorTelefono());
            }
            datos.add(new Phrase("Tutor / representante: " + tutor + "\n", normal));
        }
        datos.add(new Phrase("Estado: " + c.getEstado().name(), normal));
        doc.add(datos);

        if (c.getObservaciones() != null && !c.getObservaciones().isBlank()) {
            doc.add(new Paragraph(c.getObservaciones(),
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, GRIS)));
        }
        doc.add(new Paragraph(" "));
    }

    private void cuerpo(Document doc, String contenido) throws DocumentException {
        Font texto = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
        for (String bloque : contenido.split("\n\n")) {
            Paragraph p = new Paragraph(bloque.trim().replaceAll("\n", " "), texto);
            p.setSpacingAfter(8);
            doc.add(p);
        }
    }

    private void bloqueFirma(Document doc, Consentimiento c) throws DocumentException {
        doc.add(new Paragraph(" "));
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1.2f, 1f});

        Font etiqueta = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, GRIS);
        Font valor = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
        Font mono = FontFactory.getFont(FontFactory.COURIER, 8, Color.BLACK);

        PdfPCell izq = new PdfPCell();
        izq.setPadding(8);
        izq.setBorderColor(new Color(200, 200, 200));
        if (c.getFirmaImagen() != null && !c.getFirmaImagen().isBlank()) {
            Image firma = imagen(c.getFirmaImagen());
            if (firma != null) {
                firma.scaleToFit(200, 70);
                izq.addElement(firma);
            }
        }
        Paragraph firmante = new Paragraph();
        firmante.add(new Phrase("Firma del paciente/tutor\n", etiqueta));
        firmante.add(new Phrase(c.getFirmante() == null ? "—" : c.getFirmante(), valor));
        firmante.add(new Phrase("\n" + (c.getFirmadoEn() == null ? "" : c.getFirmadoEn().toString()
                .replace('T', ' ').substring(0, 16) + " UTC"), etiqueta));
        izq.addElement(firmante);

        PdfPCell der = new PdfPCell();
        der.setPadding(8);
        der.setBorderColor(new Color(200, 200, 200));
        Paragraph hash = new Paragraph();
        hash.add(new Phrase("Huella SHA-256\n", etiqueta));
        hash.add(new Phrase(c.getFirmaHash() == null ? "—" : textoPartido(c.getFirmaHash()), mono));
        hash.add(new Phrase("\n\nEste documento queda congelado desde su firma.", etiqueta));
        der.addElement(hash);

        tabla.addCell(izq);
        tabla.addCell(der);
        doc.add(tabla);
    }

    private String textoPartido(String hash) {
        return hash.substring(0, 32) + "\n" + hash.substring(32);
    }

    private Image imagen(String dataUrl) {
        try {
            int corte = dataUrl.indexOf("base64,");
            String base64 = corte >= 0 ? dataUrl.substring(corte + 7) : dataUrl;
            byte[] bytes = Base64.getDecoder().decode(base64);
            return Image.getInstance(bytes);
        } catch (Exception e) {
            return null;
        }
    }
}
