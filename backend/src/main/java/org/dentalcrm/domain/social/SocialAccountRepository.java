package org.dentalcrm.domain.social;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    List<SocialAccount> findByPlatformOrderByAccountName(SocialPlatform platform);

    void deleteByPlatform(SocialPlatform platform);
}