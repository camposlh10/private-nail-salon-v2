package com.nailsalon.backend.booking.infrastructure;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nailsalon.backend.booking.domain.Appointment;
import com.nailsalon.backend.booking.domain.AppointmentStatus;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

	/** Blocking-status appointments overlapping [windowStart, windowEnd). */
	List<Appointment> findByStatusInAndStartAtLessThanAndEndAtGreaterThan(
			Collection<AppointmentStatus> statuses, Instant windowEnd, Instant windowStart);

	/** Calendar feed: everything (any status) overlapping the range. */
	List<Appointment> findByStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(
			Instant windowEnd, Instant windowStart);

	Optional<Appointment> findByIdempotencyKey(String idempotencyKey);
}
