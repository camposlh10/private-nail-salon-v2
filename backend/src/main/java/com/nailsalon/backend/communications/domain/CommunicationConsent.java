package com.nailsalon.backend.communications.domain;

import java.time.Instant;

import com.nailsalon.backend.shared.persistence.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Permission to contact a phone number on a channel for a purpose, plus which
 * disclosure text version was shown when it was captured. Keyed by phone (E.164)
 * rather than a Client id because clients are only created after a confirmed
 * booking; the booking flow records consent first.
 */
@Entity
@Table(name = "communication_consent")
public class CommunicationConsent extends AuditableEntity {

	@Column(nullable = false, length = 20)
	private String phone;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private CommunicationChannel channel;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ConsentPurpose purpose;

	@Column(nullable = false)
	private boolean granted;

	@Column(name = "disclosure_version", nullable = false, length = 40)
	private String disclosureVersion;

	@Column(name = "captured_at", nullable = false)
	private Instant capturedAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public CommunicationChannel getChannel() {
		return channel;
	}

	public void setChannel(CommunicationChannel channel) {
		this.channel = channel;
	}

	public ConsentPurpose getPurpose() {
		return purpose;
	}

	public void setPurpose(ConsentPurpose purpose) {
		this.purpose = purpose;
	}

	public boolean isGranted() {
		return granted;
	}

	public void setGranted(boolean granted) {
		this.granted = granted;
	}

	public String getDisclosureVersion() {
		return disclosureVersion;
	}

	public void setDisclosureVersion(String disclosureVersion) {
		this.disclosureVersion = disclosureVersion;
	}

	public Instant getCapturedAt() {
		return capturedAt;
	}

	public void setCapturedAt(Instant capturedAt) {
		this.capturedAt = capturedAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public void setRevokedAt(Instant revokedAt) {
		this.revokedAt = revokedAt;
	}
}
