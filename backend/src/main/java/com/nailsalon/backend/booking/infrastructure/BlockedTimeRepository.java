package com.nailsalon.backend.booking.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nailsalon.backend.booking.domain.BlockedTime;

public interface BlockedTimeRepository extends JpaRepository<BlockedTime, UUID> {

	/** Blocks overlapping [windowStart, windowEnd) — half-open interval semantics. */
	List<BlockedTime> findByStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(
			Instant windowEnd, Instant windowStart);
}
