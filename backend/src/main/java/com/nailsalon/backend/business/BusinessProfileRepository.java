package com.nailsalon.backend.business;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, UUID> {

	/** The singleton business row (at most one exists — see single_row constraint). */
	Optional<BusinessProfile> findFirstByOrderByCreatedAtAsc();

	/**
	 * Serialization point for hold/booking writes: taking a PESSIMISTIC_WRITE lock on
	 * the singleton business row makes concurrent "check availability then insert"
	 * sequences run one at a time, so two simultaneous requests can never both
	 * reserve the same slot (works on PostgreSQL and H2 alike).
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select b from BusinessProfile b")
	Optional<BusinessProfile> lockSingleton();
}
