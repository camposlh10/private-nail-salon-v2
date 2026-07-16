package com.nailsalon.backend.booking.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nailsalon.backend.booking.BookingProperties;
import com.nailsalon.backend.booking.api.publicapi.PublicBookingDtos.SlotHoldCreate;
import com.nailsalon.backend.booking.api.publicapi.PublicBookingDtos.SlotHoldView;
import com.nailsalon.backend.booking.domain.SlotHold;
import com.nailsalon.backend.booking.infrastructure.SlotHoldRepository;
import com.nailsalon.backend.business.BusinessProfileRepository;
import com.nailsalon.backend.shared.error.ApiException;

/**
 * Slot holds: a 10-minute reservation created atomically. The transaction first takes
 * a pessimistic lock on the singleton business row, which serializes ALL hold/booking
 * writes — so "expire stale holds, check availability, insert" can never interleave
 * between two requests, and exactly one of two simultaneous attempts wins.
 */
@Service
public class SlotHoldService {

	private final BusinessProfileRepository businesses;
	private final SlotHoldRepository holds;
	private final AvailabilityService availability;
	private final BookingProperties properties;
	private final Clock clock;

	public SlotHoldService(BusinessProfileRepository businesses, SlotHoldRepository holds,
			AvailabilityService availability, BookingProperties properties, Clock clock) {
		this.businesses = businesses;
		this.holds = holds;
		this.availability = availability;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional
	public SlotHoldView create(SlotHoldCreate request) {
		businesses.lockSingleton().orElseThrow(() -> ApiException.notFound("Business is not configured"));
		BookingSelection selection = availability.loadSelection(request.serviceId(), request.addOnIds());
		Instant now = clock.instant();

		// Hygiene: flip stale ACTIVE holds to RELEASED (expired holds already don't
		// block availability, this just keeps the table tidy).
		holds.findByStatusAndExpiresAtBefore(SlotHold.Status.ACTIVE, now)
				.forEach(h -> h.setStatus(SlotHold.Status.RELEASED));

		Instant start = request.start().toInstant();
		if (!availability.isSlotAvailable(selection, start, null)) {
			throw ApiException.conflict("This time is no longer available");
		}

		SlotHold hold = new SlotHold();
		hold.setServiceId(selection.service().getId());
		hold.setAddOnIdList(selection.addOns().stream().map(a -> a.getId()).toList());
		hold.setStartAt(start);
		hold.setEndAt(start.plus(Duration.ofMinutes(selection.durationMinutes())));
		hold.setExpiresAt(now.plus(Duration.ofMinutes(properties.holdMinutes())));
		return toView(holds.save(hold), selection.zone());
	}

	/** Idempotent: releasing an unknown or already consumed/released hold is a no-op. */
	@Transactional
	public void release(UUID id) {
		holds.findById(id)
				.filter(h -> h.getStatus() == SlotHold.Status.ACTIVE)
				.ifPresent(h -> h.setStatus(SlotHold.Status.RELEASED));
	}

	static SlotHoldView toView(SlotHold hold, ZoneId zone) {
		return new SlotHoldView(hold.getId(), hold.getServiceId(), hold.getAddOnIdList(),
				OffsetDateTime.ofInstant(hold.getStartAt(), zone),
				OffsetDateTime.ofInstant(hold.getEndAt(), zone),
				OffsetDateTime.ofInstant(hold.getExpiresAt(), zone));
	}
}
