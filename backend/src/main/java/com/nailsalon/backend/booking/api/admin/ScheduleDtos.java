package com.nailsalon.backend.booking.api.admin;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request/response records for the owner schedule API, mirroring openapi.yaml. */
public final class ScheduleDtos {

	private ScheduleDtos() {
	}

	public record WeeklyInterval(
			@NotNull DayOfWeek dayOfWeek,
			@NotNull LocalTime startTime,
			@NotNull LocalTime endTime) {
	}

	public record WeeklySchedule(@NotNull @Valid List<WeeklyInterval> days) {
	}

	public record OverrideWrite(
			@NotNull LocalDate date,
			@NotNull Boolean closed,
			LocalTime startTime,
			LocalTime endTime,
			@Size(max = 500) String reason) {
	}

	public record AdminOverride(
			UUID id, LocalDate date, boolean closed, LocalTime startTime, LocalTime endTime, String reason) {
	}

	public record BlockWrite(
			@NotNull OffsetDateTime start,
			@NotNull OffsetDateTime end,
			@Size(max = 500) String reason) {
	}

	public record AdminBlock(UUID id, OffsetDateTime start, OffsetDateTime end, String reason) {
	}
}
