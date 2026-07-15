package com.nailsalon.backend.communications.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nailsalon.backend.communications.domain.CommunicationOutbox;

public interface CommunicationOutboxRepository extends JpaRepository<CommunicationOutbox, UUID> {

	/** Work-claiming query for the future background sender. */
	List<CommunicationOutbox> findTop20ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(
			CommunicationOutbox.Status status, Instant now);
}
