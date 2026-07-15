package com.nailsalon.backend.communications.domain;

import java.time.Instant;

import com.nailsalon.backend.shared.persistence.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Transactional outbox: the appointment transaction inserts a row here (e.g.
 * APPOINTMENT_CONFIRMED) in the SAME transaction, and a background worker later claims
 * it, renders the template, and sends via {@link
 * com.nailsalon.backend.communications.gateway.SmsGateway}. The worker arrives with the
 * booking milestone — this PR ships the reliable-message model only.
 */
@Entity
@Table(name = "communication_outbox")
public class CommunicationOutbox extends AuditableEntity {

	public enum Status {
		PENDING, SENT, FAILED
	}

	@Column(name = "message_type", nullable = false, length = 60)
	private String messageType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private CommunicationChannel channel = CommunicationChannel.SMS;

	@Column(name = "recipient", nullable = false, length = 320)
	private String recipient;

	/** Template parameters as a small JSON blob (rendered by the future worker). */
	@Column(nullable = false, length = 4000)
	private String payload;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status = Status.PENDING;

	@Column(nullable = false)
	private int attempts;

	@Column(name = "next_attempt_at")
	private Instant nextAttemptAt;

	@Column(name = "provider_message_id", length = 80)
	private String providerMessageId;

	@Column(name = "last_error", length = 1000)
	private String lastError;

	public String getMessageType() {
		return messageType;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

	public CommunicationChannel getChannel() {
		return channel;
	}

	public void setChannel(CommunicationChannel channel) {
		this.channel = channel;
	}

	public String getRecipient() {
		return recipient;
	}

	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public int getAttempts() {
		return attempts;
	}

	public void setAttempts(int attempts) {
		this.attempts = attempts;
	}

	public Instant getNextAttemptAt() {
		return nextAttemptAt;
	}

	public void setNextAttemptAt(Instant nextAttemptAt) {
		this.nextAttemptAt = nextAttemptAt;
	}

	public String getProviderMessageId() {
		return providerMessageId;
	}

	public void setProviderMessageId(String providerMessageId) {
		this.providerMessageId = providerMessageId;
	}

	public String getLastError() {
		return lastError;
	}

	public void setLastError(String lastError) {
		this.lastError = lastError;
	}
}
