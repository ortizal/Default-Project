package org.dentalcrm.web.facturacion.sri;

import org.dentalcrm.exception.BusinessException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

/**
 * Implementación real de {@link SriGateway}: habla SOAP (document/literal)
 * con los servicios offline del SRI mediante HTTP, como indica la ficha
 * técnica de comprobantes electrónicos.
 *
 * <p>No hace ninguna llamada en construcción: sólo cuando el módulo de
 * emisión la invoca, y nunca en modo simulado (ver
 * {@link AuthorizationService}).
 */
@Component
public class SriHttpGateway implements SriGateway {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    private final WebClient.Builder builder;

    public SriHttpGateway(WebClient.Builder builder) {
        this.builder = builder;
    }

    @Override
    public RespuestaRecepcion recepcionar(String xmlFirmadoBase64, SriEntorno entorno) {
        String cuerpo = post(entorno.urlRecepcion(), SriSoapXml.envelopeRecepcion(xmlFirmadoBase64));
        sinFalla(cuerpo);
        return SriSoapXml.parseRecepcion(cuerpo);
    }

    @Override
    public RespuestaAutorizacion autorizar(String claveAcceso, SriEntorno entorno) {
        String cuerpo = post(entorno.urlAutorizacion(), SriSoapXml.envelopeAutorizacion(claveAcceso));
        sinFalla(cuerpo);
        return SriSoapXml.parseAutorizacion(cuerpo);
    }

    private String post(String url, String envelope) {
        try {
            return builder.clone()
                    .build()
                    .post()
                    .uri(url)
                    .contentType(MediaType.TEXT_XML)
                    .accept(MediaType.TEXT_XML, MediaType.APPLICATION_XML)
                    .header("SOAPAction", "")
                    .bodyValue(envelope)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(TIMEOUT);
        } catch (WebClientResponseException e) {
            throw new BusinessException("SRI_ERROR_HTTP",
                    "El SRI respondió HTTP " + e.getStatusCode().value()
                            + ": " + SriSoapXml.recorte(e.getResponseBodyAsString()));
        } catch (RuntimeException e) {
            throw new BusinessException("SRI_SIN_CONEXION",
                    "No se pudo contactar al SRI: " + detalle(e));
        }
    }

    private void sinFalla(String cuerpo) {
        String falla = SriSoapXml.fault(cuerpo);
        if (falla != null) {
            throw new BusinessException("SRI_ERROR_SOAP",
                    "El SRI devolvió un fallo SOAP: " + falla);
        }
    }

    private String detalle(RuntimeException e) {
        String mensaje = e.getMessage();
        return mensaje == null ? e.getClass().getSimpleName() : SriSoapXml.recorte(mensaje);
    }
}
