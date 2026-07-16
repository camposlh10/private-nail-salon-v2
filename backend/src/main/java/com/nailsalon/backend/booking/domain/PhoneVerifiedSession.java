package com.nailsalon.backend.booking.domain;

import java.time.Instant;
import java.util.UUID;

import com.nailsalon.backend.shared.persistence.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Short-lived, single-use proof that a phone passed OTP verification. The browser holds
 * an opaque token in an HttpOnly cookie; only its SHA-256 hash is stored here.
 */
@Entity
@Table(name = "phone_verified_session")
public class PhoneVerifiedSession extends AuditableEntity {

	@Column(name = "phone_e164", nullable = false, length = 20)
	private String phoneE164;

	@Column(name = "token_hash", nullable = false, length = 64, unique = true)
	private String tokenHash;

	@Column(name = "slot_hold_id")
	private UUID slotHoldId;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "consumed_at")
	private Instant consumedAt;

	public boolean isUsable(Instant now) {
		return consumedAt == null && expiresAt.isAfter(now);
	}

	public String getPhoneE164() {
		return phoneE164;
	}

	public void setPhoneE164(String phoneE164) {
		this.phoneE164 = phoneE164;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public void setTokenHash(String tokenHash) {
		this.tokenHash = tokenHash;
	}

	public UUID getSlotHoldId() {
		return slotHoldId;
	}

	public void setSlotHoldId(UUID slotHoldId) {
		this.slotHoldId = slotHoldId;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	public Instant getConsumedAt() {
		return consumedAt;
	}

	public void setConsumedAt(Instant consumedAt) {
		this.consumedAt = consumedAt;
	}
}
