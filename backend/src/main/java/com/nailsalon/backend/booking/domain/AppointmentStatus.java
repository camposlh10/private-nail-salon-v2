package com.nailsalon.backend.booking.domain;

import java.util.Set;

/** Lifecycle of an appointment. */
public enum AppointmentStatus {

	CONFIRMED, CHECKED_IN, IN_PROGRESS, COMPLETED, CANCELLED_BY_CLIENT, CANCELLED_BY_OWNER, NO_SHOW;

	/**
	 * Statuses that occupy their time slot for availability and overlap purposes.
	 * Cancellations and no-shows free the slot. Mirrors the PostgreSQL exclusion
	 * constraint in V16 — keep the two in sync.
	 */
	public static final Set<AppointmentStatus> BLOCKING = Set.of(CONFIRMED, CHECKED_IN, IN_PROGRESS, COMPLETED);

	public boolean blocksTime() {
		return BLOCKING.contains(this);
	}

	public boolean isTerminal() {
		return this == COMPLETED || this == CANCELLED_BY_CLIENT || this == CANCELLED_BY_OWNER || this == NO_SHOW;
	}
}
