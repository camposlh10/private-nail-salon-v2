package com.nailsalon.backend.booking.domain;

import java.util.UUID;

import com.nailsalon.backend.shared.persistence.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** Append-only appointment history entry (created, status changes, ...). */
@Entity
@Table(name = "appointment_event")
public class AppointmentEvent extends AuditableEntity {

	public static final String CREATED = "APPOINTMENT_CREATED";
	public static final String STATUS_CHANGED = "STATUS_CHANGED";

	public enum Actor {
		CLIENT, OWNER, SYSTEM
	}

	@Column(name = "appointment_id", nullable = false)
	private UUID appointmentId;

	@Column(name = "event_type", nullable = false, length = 60)
	private String eventType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Actor actor;

	@Column(length = 2000)
	private String detail;

	public static AppointmentEvent of(UUID appointmentId, String eventType, Actor actor, String detail) {
		AppointmentEvent event = new AppointmentEvent();
		event.appointmentId = appointmentId;
		event.eventType = eventType;
		event.actor = actor;
		event.detail = detail;
		return event;
	}

	public UUID getAppointmentId() {
		return appointmentId;
	}

	public String getEventType() {
		return eventType;
	}

	public Actor getActor() {
		return actor;
	}

	public String getDetail() {
		return detail;
	}
}
