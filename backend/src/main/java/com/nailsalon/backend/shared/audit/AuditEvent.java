package com.nailsalon.backend.shared.audit;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** Append-only record of an important change (who did what to which resource). */
@Entity
@Table(name = "audit_event")
public class AuditEvent {

	@Id
	private UUID id;

	@Column(length = 320)
	private String actor;

	@Column(nullable = false, length = 80)
	private String action;

	@Column(name = "resource_type", nullable = false, length = 80)
	private String resourceType;

	@Column(name = "resource_id", length = 80)
	private String resourceId;

	@Column(name = "change_summary", length = 4000)
	private String changeSummary;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	protected AuditEvent() {
	}

	public AuditEvent(String actor, String action, String resourceType, String resourceId, String changeSummary) {
		this.actor = actor;
		this.action = action;
		this.resourceType = resourceType;
		this.resourceId = resourceId;
		this.changeSummary = changeSummary;
	}

	@PrePersist
	void onCreate() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		occurredAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getActor() {
		return actor;
	}

	public String getAction() {
		return action;
	}

	public String getResourceType() {
		return resourceType;
	}

	public String getResourceId() {
		return resourceId;
	}

	public String getChangeSummary() {
		return changeSummary;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}
}
