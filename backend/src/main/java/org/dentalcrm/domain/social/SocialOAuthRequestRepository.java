package org.dentalcrm.domain.social;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialOAuthRequestRepository extends JpaRepository<SocialOAuthRequest, String> {

    Optional<SocialOAuthRequest> findByStateAndProvider(String state, SocialProvider provider);
}