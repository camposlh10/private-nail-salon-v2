package com.nailsalon.backend.booking.api.publicapi;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nailsalon.backend.booking.api.publicapi.PublicBookingDtos.AppointmentCreate;
import com.nailsalon.backend.booking.api.publicapi.PublicBookingDtos.PublicAppointment;
import com.nailsalon.backend.booking.application.BookingService;
import com.nailsalon.backend.booking.application.BookingService.ConfirmResult;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/public/appointments")
public class PublicAppointmentController {

	private final BookingService bookingService;

	public PublicAppointmentController(BookingService bookingService) {
		this.bookingService = bookingService;
	}

	@PostMapping
	public ResponseEntity<PublicAppointment> confirm(
			@CookieValue(name = PublicPhoneAuthController.SESSION_COOKIE, required = false) String sessionToken,
			@RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
			@Valid @RequestBody AppointmentCreate request) {
		ConfirmResult result = bookingService.confirm(sessionToken, request, idempotencyKey);
		return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
				.body(result.appointment());
	}
}
