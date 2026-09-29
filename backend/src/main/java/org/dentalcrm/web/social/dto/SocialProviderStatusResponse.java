package org.dentalcrm.web.social.dto;

import java.util.List;

public record SocialProviderStatusResponse(boolean configured, List<SocialAccountResponse> accounts) {
}