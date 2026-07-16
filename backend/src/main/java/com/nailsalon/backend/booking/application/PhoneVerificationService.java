package com.nailsalon.backend.booking.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nailsalon.backend.booking.BookingProperties;
import com.nailsalon.backend.booking.domain.PhoneVerifiedSession;
import com.nailsalon.backend.booking.infrastructure.PhoneVerifiedSessionRepository;
import com.nailsalon.backend.booking.infrastructure.SlotHoldRepository;
import com.nailsalon.backend.communications.gateway.PhoneVerificationGateway;
import com.nailsalon.backend.shared.error.ApiException;

/**
 * Anonymous-customer phone verification. A successful OTP check mints a short-lived,
 * single-use verified-phone session: the browser gets an opaque token (HttpOnly
 * cookie, set by the controller); the database stores only its SHA-256 hash. The
 * session is bound to the verified phone and optionally to a slot hold.
 */
@Service
public class PhoneVerificationService {

	/** Minted session: {@code token} goes to the cookie and is never persisted. */
	public record MintedSession(String token, Instant expiresAt) {
	}

	private final PhoneVerificationGateway gateway;
	private final PhoneVerifiedSessionRepository sessions;
	private final SlotHoldRepository holds;
	private final BookingProperties properties;
	private final Clock clock;
	private final SecureRandom random = new SecureRandom();

	public PhoneVerificationService(PhoneVerificationGateway gateway,
			PhoneVerifiedSessionRepository sessions, SlotHoldRepository holds,
			BookingProperties properties, Clock clock) {
		this.gateway = gateway;
		this.sessions = sessions;
		this.holds = holds;
		this.properties = properties;
		this.clock = clock;
	}

	public void start(String phone) {
		gateway.startVerification(normalize(phone), PhoneVerificationGateway.Purpose.BOOKING);
	}

	@Transactional
	public MintedSession check(String phone, String code, UUID slotHoldId) {
		String normalized = normalize(phone);
		if (!gateway.checkVerification(normalized, code)) {
			throw ApiException.badRequest("Invalid or expired verification code");
		}
		if (slotHoldId != null) {
			holds.findById(slotHoldId)
					.filter(h -> h.isUsable(clock.instant()))
					.orElseThrow(() -> ApiException.badRequest("The slot hold has expired"));
		}

		byte[] tokenBytes = new byte[32];
		random.nextBytes(tokenBytes);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
		Instant expiresAt = clock.instant().plus(Duration.ofMinutes(properties.verifiedSessionMinutes()));

		PhoneVerifiedSession session = new PhoneVerifiedSession();
		session.setPhoneE164(normalized);
		session.setTokenHash(sha256(token));
		session.setSlotHoldId(slotHoldId);
		session.setExpiresAt(expiresAt);
		sessions.save(session);
		return new MintedSession(token, expiresAt);
	}

	/** Loose input ("+1 (555) 010-0200") normalized to strict E.164, or 400. */
	public static String normalize(String phone) {
		String cleaned = phone == null ? "" : phone.replaceAll("[\\s().-]", "");
		if (!cleaned.matches("\\+[1-9][0-9]{7,14}")) {
			throw ApiException.badRequest("Phone must be in international format, e.g. +15550100200");
		}
		return cleaned;
	}

	public static String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 unavailable", ex);
		}
	}
}
