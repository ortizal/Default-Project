package org.dentalcrm.domain.social;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    List<SocialAccount> findByPlatformOrderByAccountName(SocialPlatform platform);

    Optional<SocialAccount> findFirstByPlatformAndExternalAccountId(SocialPlatform platform, String externalAccountId);

    void deleteByPlatform(SocialPlatform platform);
}