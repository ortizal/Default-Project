package org.dentalcrm.web.social;

import org.dentalcrm.domain.social.SocialPlatform;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.exception.GlobalExceptionHandler;
import org.dentalcrm.web.social.dto.SocialPublishResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SocialControllerTest {

    private final SocialService socialService = mock(SocialService.class);
    private final SocialPublishService publicacion = mock(SocialPublishService.class);

    private MockMvc mvc() {
        return MockMvcBuilders.standaloneSetup(new SocialController(socialService, publicacion))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void publicaSinImagen() throws Exception {
        when(publicacion.publicar(eq(7L), eq("Día del odontólogo"), isNull()))
                .thenReturn(new SocialPublishResponse(7L, SocialPlatform.FACEBOOK, "111_222", Instant.now()));

        mvc().perform(multipart("/api/v1/social/publish")
                        .param("cuentaId", "7")
                        .param("texto", "Día del odontólogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicacionId").value("111_222"))
                .andExpect(jsonPath("$.platform").value("FACEBOOK"));

        verify(publicacion).publicar(eq(7L), eq("Día del odontólogo"), isNull());
    }

    @Test
    void publicaConImagen() throws Exception {
        MockMultipartFile imagen = new MockMultipartFile("imagen", "cartel.png", "image/png", new byte[]{1, 2, 3});
        when(publicacion.publicar(eq(7L), eq("Promo"), eq(imagen)))
                .thenReturn(new SocialPublishResponse(7L, SocialPlatform.FACEBOOK, "333", Instant.now()));

        mvc().perform(multipart("/api/v1/social/publish")
                        .file(imagen)
                        .param("cuentaId", "7")
                        .param("texto", "Promo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicacionId").value("333"));

        verify(publicacion).publicar(eq(7L), eq("Promo"), eq(imagen));
    }

    @Test
    void sinTextoDevuelveErrorDeValidacion() throws Exception {
        mvc().perform(multipart("/api/v1/social/publish").param("cuentaId", "7"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION"));
    }

    @Test
    void elErrorDelServicioSeDevuelveComoLoDioElBackend() throws Exception {
        when(publicacion.publicar(eq(9L), eq("Texto"), isNull()))
                .thenThrow(new BusinessException("SOCIAL_ACCOUNT_MISSING", "La cuenta seleccionada ya no está conectada"));

        mvc().perform(multipart("/api/v1/social/publish")
                        .param("cuentaId", "9")
                        .param("texto", "Texto"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOCIAL_ACCOUNT_MISSING"));
    }
}
