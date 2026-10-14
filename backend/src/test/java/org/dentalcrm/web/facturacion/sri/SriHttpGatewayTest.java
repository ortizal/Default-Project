package org.dentalcrm.web.facturacion.sri;

import com.sun.net.httpserver.HttpServer;
import org.dentalcrm.domain.configuracion.AmbienteSri;
import org.dentalcrm.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El webservice del SRI se prueba contra un servidor HTTP local: así se
 * verifica el envelope SOAP que salió y la lectura de la respuesta que
 * entró, sin depender de la red ni de un entorno de pruebas real.
 */
class SriHttpGatewayTest {

    private HttpServer servidor;
    private SriHttpGateway gateway;
    private SriEntorno entorno;

    private final AtomicReference<String> ultimaPeticion = new AtomicReference<>("");
    private final AtomicReference<String> ultimaRuta = new AtomicReference<>("");
    private final AtomicReference<String> cuerpoRespuesta = new AtomicReference<>("");
    private final AtomicInteger codigoRespuesta = new AtomicInteger(200);

    private static final String CLAVE = "0810202601179123456700110010010000000011234567814";

    @BeforeEach
    void setUp() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var manejador = (com.sun.net.httpserver.HttpHandler) exchange -> {
            ultimaRuta.set(exchange.getRequestURI().getPath());
            ultimaPeticion.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] cuerpo = cuerpoRespuesta.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/xml;charset=UTF-8");
            if (codigoRespuesta.get() == 200) {
                exchange.sendResponseHeaders(200, cuerpo.length);
                exchange.getResponseBody().write(cuerpo);
            } else {
                exchange.sendResponseHeaders(codigoRespuesta.get(), -1);
            }
            exchange.close();
        };
        String base = "/comprobantes-electronicos-ws/";
        servidor.createContext(base + "RecepcionComprobantesOffline", manejador);
        servidor.createContext(base + "AutorizacionComprobantesOffline", manejador);
        servidor.start();

        entorno = new SriEntorno("http://127.0.0.1:" + servidor.getAddress().getPort(), AmbienteSri.PRUEBAS);
        gateway = new SriHttpGateway(WebClient.builder());
    }

    @AfterEach
    void tearDown() {
        servidor.stop(0);
    }

    @Test
    void enviaElXmlFirmadoCodificadoEnBase64() {
        cuerpoRespuesta.set("""
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                  <soapenv:Body>
                    <ns2:validarComprobanteResponse xmlns:ns2="http://ec.gob.sri.ws.recepcion">
                      <RespuestaRecepcionComprobante>
                        <estado>RECIBIDA</estado>
                        <comprobantes><comprobante><claveAcceso>%s</claveAcceso></comprobante></comprobantes>
                      </RespuestaRecepcionComprobante>
                    </ns2:validarComprobanteResponse>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(CLAVE));
        String xml = "<factura/>";

        RespuestaRecepcion respuesta = gateway.recepcionar(
                Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8)), entorno);

        assertTrue(respuesta.recibida());
        assertEquals("/comprobantes-electronicos-ws/RecepcionComprobantesOffline", ultimaRuta.get());
        assertTrue(ultimaPeticion.get().contains("<rec:validarComprobante>"),
                "el envelope debe llamar a validarComprobante");
        String esperado = Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8));
        assertTrue(ultimaPeticion.get().contains("<xml>" + esperado + "</xml>"),
                "el XML firmado debe viajar en base64 dentro de <xml>");
        assertTrue(ultimaPeticion.get().contains("http://ec.gob.sri.ws.recepcion"));
    }

    @Test
    void interpretaLosMensajesDeUnaDevolucion() {
        cuerpoRespuesta.set("""
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                  <soapenv:Body>
                    <ns2:validarComprobanteResponse xmlns:ns2="http://ec.gob.sri.ws.recepcion">
                      <RespuestaRecepcionComprobante>
                        <estado>DEVUELTA</estado>
                        <comprobantes><comprobante>
                          <claveAcceso>N/A</claveAcceso>
                          <mensajes><mensaje>
                            <identificador>43</identificador>
                            <mensaje>CLAVE ACCESO REGISTRADA</mensaje>
                            <informacionAdicional>Comprobante ya enviado</informacionAdicional>
                            <tipo>ERROR</tipo>
                          </mensaje></mensajes>
                        </comprobante></comprobantes>
                      </RespuestaRecepcionComprobante>
                    </ns2:validarComprobanteResponse>
                  </soapenv:Body>
                </soapenv:Envelope>
                """);

        RespuestaRecepcion respuesta = gateway.recepcionar("PD94bWwvPg==", entorno);

        assertTrue(respuesta.devuelta());
        assertEquals(1, respuesta.mensajes().size());
        assertEquals("43", respuesta.mensajes().get(0).identificador());
        assertTrue(respuesta.detalle().contains("CLAVE ACCESO REGISTRADA"));
        assertTrue(respuesta.detalle().contains("Comprobante ya enviado"));
    }

    @Test
    void consultaLaAutorizacionPorClaveDeAcceso() {
        cuerpoRespuesta.set("""
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                  <soapenv:Body>
                    <ns2:autorizacionComprobanteResponse xmlns:ns2="http://ec.gob.sri.ws.autorizacion">
                      <RespuestaAutorizacionComprobante>
                        <claveAccesoConsultada>%s</claveAccesoConsultada>
                        <numeroComprobantes>1</numeroComprobantes>
                        <autorizaciones><autorizacion>
                          <estado>AUTORIZADO</estado>
                          <numeroAutorizacion>%s</numeroAutorizacion>
                          <fechaAutorizacion>2026-10-08T10:15:30.123-05:00</fechaAutorizacion>
                          <ambiente>PRUEBAS</ambiente>
                          <comprobante><![CDATA[<factura version="1.1.0"/>]]></comprobante>
                          <mensajes/>
                        </autorizacion></autorizaciones>
                      </RespuestaAutorizacionComprobante>
                    </ns2:autorizacionComprobanteResponse>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(CLAVE, CLAVE));

        RespuestaAutorizacion respuesta = gateway.autorizar(CLAVE, entorno);

        assertTrue(respuesta.autorizado());
        assertEquals(CLAVE, respuesta.claveAccesoConsultada());
        assertEquals(CLAVE, respuesta.numeroAutorizacion());
        assertNotNull(respuesta.fechaAutorizacion());
        assertEquals("PRUEBAS", respuesta.ambiente());
        assertEquals("<factura version=\"1.1.0\"/>", respuesta.xmlAutorizado());
        assertFalse(respuesta.sinResultado());
        assertEquals("/comprobantes-electronicos-ws/AutorizacionComprobantesOffline", ultimaRuta.get());
        assertTrue(ultimaPeticion.get().contains("<claveAccesoComprobante>" + CLAVE + "</claveAccesoComprobante>"),
                "debe consultar por la clave de acceso");
    }

    @Test
    void sinResultadoElSriTodaviaNoProcesoElComprobante() {
        cuerpoRespuesta.set("""
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                  <soapenv:Body>
                    <ns2:autorizacionComprobanteResponse xmlns:ns2="http://ec.gob.sri.ws.autorizacion">
                      <RespuestaAutorizacionComprobante>
                        <claveAccesoConsultada>%s</claveAccesoConsultada>
                        <numeroComprobantes>0</numeroComprobantes>
                      </RespuestaAutorizacionComprobante>
                    </ns2:autorizacionComprobanteResponse>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(CLAVE));

        RespuestaAutorizacion respuesta = gateway.autorizar(CLAVE, entorno);

        assertTrue(respuesta.enProceso());
        assertTrue(respuesta.sinResultado());
        assertNull(respuesta.numeroAutorizacion());
    }

    @Test
    void erroresDeNoAutorizacionLleganEnLosMensajes() {
        cuerpoRespuesta.set("""
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                  <soapenv:Body>
                    <ns2:autorizacionComprobanteResponse xmlns:ns2="http://ec.gob.sri.ws.autorizacion">
                      <RespuestaAutorizacionComprobante>
                        <claveAccesoConsultada>%s</claveAccesoConsultada>
                        <numeroComprobantes>1</numeroComprobantes>
                        <autorizaciones><autorizacion>
                          <estado>NO_AUTORIZADO</estado>
                          <fechaAutorizacion>2026-10-08T10:15:30-05:00</fechaAutorizacion>
                          <mensajes><mensaje>
                            <identificador>39</identificador>
                            <mensaje>RUC EMISOR NO VALIDO</mensaje>
                            <tipo>ERROR</tipo>
                          </mensaje></mensajes>
                        </autorizacion></autorizaciones>
                      </RespuestaAutorizacionComprobante>
                    </ns2:autorizacionComprobanteResponse>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(CLAVE));

        RespuestaAutorizacion respuesta = gateway.autorizar(CLAVE, entorno);

        assertTrue(respuesta.noAutorizado());
        assertEquals(1, respuesta.mensajes().size());
        assertTrue(respuesta.detalle().contains("RUC EMISOR NO VALIDO"));
    }

    @Test
    void unErrorHttpDelSriSeReportaComoTal() {
        codigoRespuesta.set(500);

        var error = assertThrows(BusinessException.class, () -> gateway.recepcionar("PD94bWwvPg==", entorno));

        assertEquals("SRI_ERROR_HTTP", error.getCode());
        assertTrue(error.getMessage().contains("500"));
    }

    @Test
    void unFalloSoapSeReportaAparte() {
        cuerpoRespuesta.set("""
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                  <soapenv:Body>
                    <soapenv:Fault>
                      <faultcode>soapenv:Server</faultcode>
                      <faultstring>Internal Service Error</faultstring>
                    </soapenv:Fault>
                  </soapenv:Body>
                </soapenv:Envelope>
                """);

        var error = assertThrows(BusinessException.class, () -> gateway.autorizar(CLAVE, entorno));

        assertEquals("SRI_ERROR_SOAP", error.getCode());
        assertTrue(error.getMessage().contains("Internal Service Error"));
    }

    @Test
    void unaRespuestaQueNoEsXmlSeReportaComoInvalida() {
        cuerpoRespuesta.set("<html><body>Servicio no disponible</body></html>");

        var error = assertThrows(BusinessException.class, () -> gateway.recepcionar("PD94bWwvPg==", entorno));

        assertEquals("SRI_RESPUESTA_INVALIDA", error.getCode());
    }

    @Test
    void siElSriNoRespondeSeReportaSinConexion() {
        int puerto = servidor.getAddress().getPort();
        servidor.stop(0);
        SriEntorno caido = new SriEntorno("http://127.0.0.1:" + puerto, AmbienteSri.PRUEBAS);

        var error = assertThrows(BusinessException.class, () -> gateway.recepcionar("PD94bWwvPg==", caido));

        assertEquals("SRI_SIN_CONEXION", error.getCode());
    }
}
