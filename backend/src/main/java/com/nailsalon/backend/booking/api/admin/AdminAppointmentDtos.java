package com.nailsalon.backend.booking.api.admin;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.nailsalon.backend.booking.domain.AppointmentStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request/response records for the owner appointment API, mirroring openapi.yaml. */
public final class AdminAppointmentDtos {

	private AdminAppointmentDtos() {
	}

	public record AdminClientView(UUID id, String name, String phone, String email) {
	}

	public record AdminAppointmentItemView(String itemType, String name, int durationMinutes, int priceCents) {
	}

	public record AdminAppointmentView(
			UUID id, AppointmentStatus status, OffsetDateTime start, OffsetDateTime end, String timezone,
			String serviceName, AdminClientView client, int totalCents) {
	}

	public record AppointmentEventView(String eventType, String actor, String detail, OffsetDateTime occurredAt) {
	}

	public record AdminAppointmentDetail(
			UUID id, AppointmentStatus status, OffsetDateTime start, OffsetDateTime end, String timezone,
			String serviceName, AdminClientView client, int totalCents, String notes,
			OffsetDateTime actualStart, OffsetDateTime actualEnd,
			List<AdminAppointmentItemView> items, List<AppointmentEventView> events) {
	}

	public record AdminAppointmentCreate(
			@NotNull UUID serviceId,
			List<UUID> addOnIds,
			@NotNull OffsetDateTime start,
			@NotBlank @Size(max = 200) String clientName,
			@NotBlank @Size(max = 20) String clientPhone,
			@Email @Size(max = 320) String clientEmail,
			@Size(max = 2000) String notes) {
	}

	public record StatusChangeRequest(@NotNull AppointmentStatus status) {
	}
}
