package com.nailsalon.backend.booking.domain;

import java.time.Instant;
import java.util.UUID;

import com.nailsalon.backend.shared.persistence.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A confirmed booking. startAt/endAt are absolute instants and the interval is
 * half-open [startAt, endAt), so back-to-back appointments never conflict. timezone
 * snapshots the business zone at booking time for stable wall-clock rendering.
 */
@Entity
@Table(name = "appointment")
public class Appointment extends AuditableEntity {

	public enum Source {
		PUBLIC, OWNER
	}

	@Column(name = "client_id", nullable = false)
	private UUID clientId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private AppointmentStatus status = AppointmentStatus.CONFIRMED;

	@Column(name = "start_at", nullable = false)
	private Instant startAt;

	@Column(name = "end_at", nullable = false)
	private Instant endAt;

	@Column(nullable = false, length = 64)
	private String timezone;

	@Column(name = "actual_start_at")
	private Instant actualStartAt;

	@Column(name = "actual_end_at")
	private Instant actualEndAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Source source = Source.PUBLIC;

	@Column(length = 2000)
	private String notes;

	@Column(name = "idempotency_key", length = 80, unique = true)
	private String idempotencyKey;

	public UUID getClientId() {
		return clientId;
	}

	public void setClientId(UUID clientId) {
		this.clientId = clientId;
	}

	public AppointmentStatus getStatus() {
		return status;
	}

	public void setStatus(AppointmentStatus status) {
		this.status = status;
	}

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

	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	public Instant getActualStartAt() {
		return actualStartAt;
	}

	public void setActualStartAt(Instant actualStartAt) {
		this.actualStartAt = actualStartAt;
	}

	public Instant getActualEndAt() {
		return actualEndAt;
	}

	public void setActualEndAt(Instant actualEndAt) {
		this.actualEndAt = actualEndAt;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public void setIdempotencyKey(String idempotencyKey) {
		this.idempotencyKey = idempotencyKey;
	}
}
