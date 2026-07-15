package com.nailsalon.backend.communications.providers;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.nailsalon.backend.communications.gateway.PhoneVerificationGateway;

/**
 * Local fake for {@link PhoneVerificationGateway}: generates a real 6-digit code but
 * LOGS it instead of sending SMS, and verifies against an in-memory store. Replace with
 * a Twilio Verify implementation (behind a config property) when going live — no real
 * SMS is ever sent by this class.
 */
@Component
public class LoggingPhoneVerificationGateway implements PhoneVerificationGateway {

	private static final Logger log = LoggerFactory.getLogger(LoggingPhoneVerificationGateway.class);
	private static final Duration CODE_TTL = Duration.ofMinutes(10);

	private record PendingCode(String code, Instant expiresAt) {
	}

	private final Map<String, PendingCode> pending = new ConcurrentHashMap<>();
	private final SecureRandom random = new SecureRandom();

	@Override
	public void startVerification(String phone, Purpose purpose) {
		String code = "%06d".formatted(random.nextInt(1_000_000));
		pending.put(phone, new PendingCode(code, Instant.now().plus(CODE_TTL)));
		log.info("[FAKE SMS] OTP for {} ({}): {} (valid {} min)", phone, purpose, code, CODE_TTL.toMinutes());
	}

	@Override
	public boolean checkVerification(String phone, String code) {
		PendingCode entry = pending.get(phone);
		if (entry == null || entry.expiresAt().isBefore(Instant.now()) || !entry.code().equals(code)) {
			return false;
		}
		pending.remove(phone); // single use
		return true;
	}

	/** Test hook: the code that would have been texted to this phone. */
	public String pendingCodeFor(String phone) {
		PendingCode entry = pending.get(phone);
		return entry != null ? entry.code() : null;
	}
}
