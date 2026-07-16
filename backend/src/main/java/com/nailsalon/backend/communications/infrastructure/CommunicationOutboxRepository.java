package com.nailsalon.backend.communications.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.nailsalon.backend.communications.domain.CommunicationOutbox;

import jakarta.persistence.LockModeType;

public interface CommunicationOutboxRepository extends JpaRepository<CommunicationOutbox, UUID> {

	/** Work-claiming query for the future background sender. */
	List<CommunicationOutbox> findTop20ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(
			CommunicationOutbox.Status status, Instant now);

	/**
	 * Locked claim for the outbox worker: rows are picked up under PESSIMISTIC_WRITE
	 * so overlapping worker runs (or a second app instance) can't send the same row
	 * twice.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from CommunicationOutbox o where o.status = :status and o.nextAttemptAt <= :now "
			+ "order by o.createdAt asc")
	List<CommunicationOutbox> claimDue(CommunicationOutbox.Status status, Instant now, Pageable pageable);
}
