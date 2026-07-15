package com.nailsalon.backend.auth.owner;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nailsalon.backend.shared.error.ApiException;

/**
 * Owner password reset. Until an email provider is wired up (communications module,
 * PR6+), the reset token is logged instead of emailed — same stub pattern the
 * SMS gateway will use. request() never reveals whether the email exists.
 */
@Service
public class PasswordResetService {

	private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
	private static final Duration TOKEN_TTL = Duration.ofMinutes(30);

	private final OwnerUserRepository owners;
	private final PasswordResetTokenRepository tokens;
	private final PasswordEncoder passwordEncoder;
	private final SecureRandom random = new SecureRandom();

	public PasswordResetService(OwnerUserRepository owners, PasswordResetTokenRepository tokens,
			PasswordEncoder passwordEncoder) {
		this.owners = owners;
		this.tokens = tokens;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public void requestReset(String email) {
		owners.findByEmailIgnoreCase(email).ifPresent(owner -> {
			byte[] bytes = new byte[32];
			random.nextBytes(bytes);
			String token = HexFormat.of().formatHex(bytes);
			tokens.save(new PasswordResetToken(owner.getId(), sha256(token), Instant.now().plus(TOKEN_TTL)));
			// TODO(communications): send via email gateway instead of logging.
			log.info("Password reset requested for {}. Reset token (valid {} min): {}",
					owner.getEmail(), TOKEN_TTL.toMinutes(), token);
		});
	}

	@Transactional
	public void reset(String token, String newPassword) {
		PasswordResetToken resetToken = tokens.findByTokenHash(sha256(token))
				.filter(PasswordResetToken::isUsable)
				.orElseThrow(() -> ApiException.badRequest("Invalid or expired reset token"));
		OwnerUser owner = owners.findById(resetToken.getOwnerUserId())
				.orElseThrow(() -> ApiException.badRequest("Invalid or expired reset token"));
		owner.setPasswordHash(passwordEncoder.encode(newPassword));
		resetToken.markUsed();
	}

	private static String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 unavailable", ex);
		}
	}
}
