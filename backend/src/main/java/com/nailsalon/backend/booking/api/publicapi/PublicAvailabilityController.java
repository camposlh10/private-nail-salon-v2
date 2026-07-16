package com.nailsalon.backend.booking.api.publicapi;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nailsalon.backend.booking.api.publicapi.PublicBookingDtos.AvailabilityResponse;
import com.nailsalon.backend.booking.application.AvailabilityService;

@RestController
@RequestMapping("/api/v1/public/availability")
public class PublicAvailabilityController {

	private final AvailabilityService availabilityService;

	public PublicAvailabilityController(AvailabilityService availabilityService) {
		this.availabilityService = availabilityService;
	}

	@GetMapping
	public AvailabilityResponse availability(
			@RequestParam UUID serviceId,
			@RequestParam(required = false) List<UUID> addOnIds,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return availabilityService.availability(serviceId, addOnIds, from, to);
	}
}
