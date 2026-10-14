package org.dentalcrm.web.archivo.dto;

/** Contenido binario de un archivo clínico listo para la respuesta HTTP. */
public record ContenidoArchivo(String nombre, String mime, byte[] contenido) {
}
