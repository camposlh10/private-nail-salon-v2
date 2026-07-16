package com.nailsalon.backend.booking.domain;

import java.time.Instant;

import com.nailsalon.backend.shared.persistence.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Owner-blocked time (break, errand, buffer): absolute instants so blocks stay
 * unambiguous across DST transitions. Interval is [startAt, endAt).
 */
@Entity
@Table(name = "blocked_time")
public class BlockedTime extends AuditableEntity {

	@Column(name = "start_at", nullable = false)
	private Instant startAt;

	@Column(name = "end_at", nullable = false)
	private Instant endAt;

	@Column(length = 500)
	private String reason;

	public Instant getStartAt() {
		return startAt;
	}

	public void setStartAt(Instant startAt) {
		this.startAt = startAt;
	}

	public Instant getEndAt() {
		return endAt;
	}

	public void setEndAt(Instant endAt) {
		this.endAt = endAt;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}
}
