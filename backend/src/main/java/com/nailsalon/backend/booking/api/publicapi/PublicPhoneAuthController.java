package com.nailsalon.backend.booking.api.publicapi;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nailsalon.backend.booking.api.publicapi.PublicBookingDtos.PhoneCheckRequest;
import com.nailsalon.backend.booking.api.publicapi.PublicBookingDtos.PhoneStartRequest;
import com.nailsalon.backend.booking.api.publicapi.PublicBookingDtos.VerifiedSessionView;
import com.nailsalon.backend.booking.application.PhoneVerificationService;
import com.nailsalon.backend.booking.application.PhoneVerificationService.MintedSession;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * Customer phone verification. The verified-phone session travels in an HttpOnly
 * cookie so page JavaScript can never read the token; SameSite=Lax plus the JSON-only
 * booking endpoints keep it useless to cross-site attackers.
 */
@RestController
@RequestMapping("/api/v1/public/auth/phone")
public class PublicPhoneAuthController {

	public static final String SESSION_COOKIE = "BOOKING_SESSION";

	private final PhoneVerificationService verificationService;

	public PublicPhoneAuthController(PhoneVerificationService verificationService) {
		this.verificationService = verificationService;
	}

	@PostMapping("/start")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void start(@Valid @RequestBody PhoneStartRequest request) {
		verificationService.start(request.phone());
	}

	@PostMapping("/check")
	public VerifiedSessionView check(@Valid @RequestBody PhoneCheckRequest body,
			HttpServletRequest request, HttpServletResponse response) {
		MintedSession session = verificationService.check(body.phone(), body.code(), body.slotHoldId());
		ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE, session.token())
				.httpOnly(true)
				.secure(request.isSecure())
				.path("/api/v1/public")
				.sameSite("Lax")
				.maxAge(Duration.ofMinutes(35)) // slightly past server-side expiry
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
		return new VerifiedSessionView(OffsetDateTime.ofInstant(session.expiresAt(), ZoneOffset.UTC));
	}
}
