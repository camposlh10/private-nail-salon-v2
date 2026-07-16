package com.nailsalon.backend.booking.domain;

import java.time.LocalDate;
import java.time.LocalTime;

import com.nailsalon.backend.shared.persistence.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Per-date exception to the weekly hours: either fully closed, or special hours that
 * REPLACE (not merge with) the weekly hours for that date.
 */
@Entity
@Table(name = "availability_override")
public class AvailabilityOverride extends AuditableEntity {

	@Column(name = "override_date", nullable = false, unique = true)
	private LocalDate date;

	@Column(nullable = false)
	private boolean closed;

	@Column(name = "start_time")
	private LocalTime startTime;

	@Column(name = "end_time")
	private LocalTime endTime;

	@Column(length = 500)
	private String reason;

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public boolean isClosed() {
		return closed;
	}

	public void setClosed(boolean closed) {
		this.closed = closed;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalTime endTime) {
		this.endTime = endTime;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}
}
