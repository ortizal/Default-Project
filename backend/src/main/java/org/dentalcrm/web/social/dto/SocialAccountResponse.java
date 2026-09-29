package org.dentalcrm.web.social.dto;

import org.dentalcrm.domain.social.SocialPlatform;

public record SocialAccountResponse(Long id, SocialPlatform platform, String accountName) {
}