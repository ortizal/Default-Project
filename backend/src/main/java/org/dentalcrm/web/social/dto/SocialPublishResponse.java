package org.dentalcrm.web.social.dto;

import org.dentalcrm.domain.social.SocialPlatform;

import java.time.Instant;

public record SocialPublishResponse(Long cuentaId, SocialPlatform platform, String publicacionId,
                                    Instant publicadoEn) {
}
