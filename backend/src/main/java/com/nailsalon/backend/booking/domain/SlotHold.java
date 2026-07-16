package com.nailsalon.backend.booking.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.nailsalon.backend.shared.persistence.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Temporary reservation while the customer completes checkout. Blocks availability
 * while ACTIVE and unexpired (10 minutes); expiry needs no background job because
 * expired holds simply stop matching conflict queries.
 */
@Entity
@Table(name = "slot_hold")
public class SlotHold extends AuditableEntity {

	public enum Status {
		ACTIVE, CONSUMED, RELEASED
	}

	@Column(name = "service_id", nullable = false)
	private UUID serviceId;

	/** Comma-separated UUID snapshot of the selected add-ons ("" / null = none). */
	@Column(name = "add_on_ids", length = 1000)
	private String addOnIds;

	@Column(name = "start_at", nullable = false)
	private Instant startAt;

	@Column(name = "end_at", nullable = false)
	private Instant endAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status = Status.ACTIVE;

	public boolean isUsable(Instant now) {
		return status == Status.ACTIVE && expiresAt.isAfter(now);
	}

	public UUID getServiceId() {
		return serviceId;
	}

	public void setServiceId(UUID serviceId) {
		this.serviceId = serviceId;
	}

	public List<UUID> getAddOnIdList() {
		if (addOnIds == null || addOnIds.isBlank()) {
			return List.of();
		}
		return java.util.Arrays.stream(addOnIds.split(",")).map(UUID::fromString).toList();
	}

	public void setAddOnIdList(List<UUID> ids) {
		this.addOnIds = (ids == null || ids.isEmpty()) ? null
				: String.join(",", ids.stream().map(UUID::toString).toList());
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

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}
}
