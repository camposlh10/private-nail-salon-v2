package com.nailsalon.backend.auth.owner;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** One-time password-reset token; only its SHA-256 hash is stored. */
@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {

	@Id
	private UUID id;

	@Column(name = "owner_user_id", nullable = false)
	private UUID ownerUserId;

	@Column(name = "token_hash", nullable = false, length = 64)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "used_at")
	private Instant usedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected PasswordResetToken() {
	}

	public PasswordResetToken(UUID ownerUserId, String tokenHash, Instant expiresAt) {
		this.ownerUserId = ownerUserId;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	@PrePersist
	void onCreate() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		createdAt = Instant.now();
	}

	public boolean isUsable() {
		return usedAt == null && expiresAt.isAfter(Instant.now());
	}

	public UUID getOwnerUserId() {
		return ownerUserId;
	}

	public void markUsed() {
		this.usedAt = Instant.now();
	}
}
