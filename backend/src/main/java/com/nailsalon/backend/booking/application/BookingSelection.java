package com.nailsalon.backend.booking.application;

import java.time.ZoneId;
import java.util.List;

import com.nailsalon.backend.business.BusinessProfile;
import com.nailsalon.backend.catalog.domain.SalonService;
import com.nailsalon.backend.catalog.domain.ServiceAddOn;

/**
 * A validated "what the customer wants": an active bookable service plus zero or more
 * of its active add-ons. Duration and price are always recomputed server-side from
 * this — client-supplied numbers are never trusted.
 */
public record BookingSelection(BusinessProfile business, SalonService service, List<ServiceAddOn> addOns) {

	public int durationMinutes() {
		return service.getDurationMinutes()
				+ addOns.stream().mapToInt(ServiceAddOn::getAddedDurationMinutes).sum();
	}

	public int totalCents() {
		return service.getPriceCents() + addOns.stream().mapToInt(ServiceAddOn::getPriceCents).sum();
	}

	public ZoneId zone() {
		return ZoneId.of(business.getTimezone());
	}
}
