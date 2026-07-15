package com.nailsalon.backend.business;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, UUID> {

	/** The singleton business row (at most one exists — see single_row constraint). */
	Optional<BusinessProfile> findFirstByOrderByCreatedAtAsc();
}
