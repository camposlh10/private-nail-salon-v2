package com.nailsalon.backend.booking.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nailsalon.backend.booking.domain.PhoneVerifiedSession;

public interface PhoneVerifiedSessionRepository extends JpaRepository<PhoneVerifiedSession, UUID> {

	Optional<PhoneVerifiedSession> findByTokenHash(String tokenHash);
}
