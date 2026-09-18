package org.dentalcrm.domain.google;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoogleAccountRepository extends JpaRepository<GoogleAccount, Long> {

    Optional<GoogleAccount> findByEmailIgnoreCase(String email);

    Optional<GoogleAccount> findTopByOrderByIdAsc();
}