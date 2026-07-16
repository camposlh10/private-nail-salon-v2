package com.nailsalon.backend.booking.api.publicapi;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request/response records for the public booking API, mirroring openapi.yaml. */
public final class PublicBookingDtos {

	private PublicBookingDtos() {
	}

	// --- availability -------------------------------------------------------------

	public record AvailabilityDay(LocalDate date, List<OffsetDateTime> slots) {
	}

	public record AvailabilityResponse(
			UUID serviceId, int durationMinutes, String timezone, List<AvailabilityDay> days) {
	}

	// --- slot holds ---------------------------------------------------------------

	public record SlotHoldCreate(
			@NotNull UUID serviceId,
			List<UUID> addOnIds,
			@NotNull OffsetDateTime start) {
	}

	public record SlotHoldView(
			UUID id, UUID serviceId, List<UUID> addOnIds,
			OffsetDateTime start, OffsetDateTime end, OffsetDateTime expiresAt) {
	}

	// --- phone verification ---------------------------------------------------------

	public record PhoneStartRequest(@NotBlank @Size(max = 20) String phone) {
	}

	public record PhoneCheckRequest(
			@NotBlank @Size(max = 20) String phone,
			@NotBlank @Size(max = 10) String code,
			UUID slotHoldId) {
	}

	public record VerifiedSessionView(OffsetDateTime expiresAt) {
	}

	// --- appointment confirmation ----------------------------------------------------

	public record AppointmentCreate(
			@NotNull UUID slotHoldId,
			@NotBlank @Size(max = 200) String clientName,
			@Email @Size(max = 320) String clientEmail,
			@Size(max = 2000) String notes) {
	}

	public record AppointmentItemView(String itemType, String name, int durationMinutes, int priceCents) {
	}

	public record PublicAppointment(
			UUID id, String status, OffsetDateTime start, OffsetDateTime end, String timezone,
			String serviceName, List<AppointmentItemView> items, int totalCents, String currency) {
	}
}
