package org.dentalcrm.web.google;

import org.dentalcrm.domain.google.GoogleCredentialRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.exception.GlobalExceptionHandler;
import org.dentalcrm.service.SocialTokenCipher;
import org.dentalcrm.web.google.dto.GoogleStatusResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GoogleControllerTest {

    private static final String URL_OK = "http://localhost:4200/dental_crm/google?google=connected";
    private static final String URL_ERROR = "http://localhost:4200/dental_crm/google?google=error";

    private final GoogleCalendarService googleCalendarService = mock(GoogleCalendarService.class);
    private final GoogleCredentialRepository credentialRepository = mock(GoogleCredentialRepository.class);
    private final GoogleService googleService = mock(GoogleService.class);
    private final SocialTokenCipher secretCipher = mock(SocialTokenCipher.class);

    private MockMvc mvc() {
        return MockMvcBuilders.standaloneSetup(
                    new GoogleController(googleCalendarService, credentialRepository, googleService, secretCipher))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void callbackConCodigoRedirigeAConectado() throws Exception {
        when(googleCalendarService.completarConexion("codigo-123"))
                .thenReturn(new GoogleStatusResponse(true, true, "doctor@clinica.com", List.of(), null, 0, 0, 0));
        when(googleCalendarService.urlRetorno(true)).thenReturn(URL_OK);

        mvc().perform(get("/api/v1/google/callback").param("code", "codigo-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", URL_OK));

        verify(googleCalendarService).completarConexion("codigo-123");
    }

    @Test
    void callbackConErrorDeGoogleNoIntercambiaElCodigo() throws Exception {
        when(googleCalendarService.urlRetorno(false)).thenReturn(URL_ERROR);

        mvc().perform(get("/api/v1/google/callback").param("error", "access_denied"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", URL_ERROR));

        verify(googleCalendarService, never()).completarConexion(any());
    }

    @Test
    void callbackConCodigoInvalidoRedirigeAError() throws Exception {
        when(googleCalendarService.completarConexion("mal"))
                .thenThrow(new BusinessException("GOOGLE_AUTH_ERROR", "No se recibió el código de autorización de Google"));
        when(googleCalendarService.urlRetorno(false)).thenReturn(URL_ERROR);

        mvc().perform(get("/api/v1/google/callback").param("code", "mal"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", URL_ERROR));
    }

    @Test
    void callbackSinParametrosRedirigeAError() throws Exception {
        when(googleCalendarService.completarConexion(null))
                .thenThrow(new BusinessException("GOOGLE_AUTH_ERROR", "No se recibió el código de autorización de Google"));
        when(googleCalendarService.urlRetorno(false)).thenReturn(URL_ERROR);

        mvc().perform(get("/api/v1/google/callback"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", URL_ERROR));
    }
}
