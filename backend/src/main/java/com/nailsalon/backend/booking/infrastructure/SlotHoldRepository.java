package com.nailsalon.backend.booking.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nailsalon.backend.booking.domain.SlotHold;

public interface SlotHoldRepository extends JpaRepository<SlotHold, UUID> {

	/** ACTIVE, unexpired holds overlapping [windowStart, windowEnd). */
	List<SlotHold> findByStatusAndExpiresAtAfterAndStartAtLessThanAndEndAtGreaterThan(
			SlotHold.Status status, Instant now, Instant windowEnd, Instant windowStart);

	/** Stale ACTIVE holds, released as hygiene before inserting a new hold. */
	List<SlotHold> findByStatusAndExpiresAtBefore(SlotHold.Status status, Instant now);
}
