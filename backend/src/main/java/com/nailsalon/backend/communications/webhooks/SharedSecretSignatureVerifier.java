package com.nailsalon.backend.communications.webhooks;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Stand-in verifier: compares the X-Webhook-Signature header against a configured
 * shared secret (constant-time). Fails closed — with no secret configured every
 * webhook is rejected, so nothing is processable until deliberately enabled.
 */
@Component
public class SharedSecretSignatureVerifier implements WebhookSignatureVerifier {

	private static final Logger log = LoggerFactory.getLogger(SharedSecretSignatureVerifier.class);

	private final String sharedSecret;

	public SharedSecretSignatureVerifier(@Value("${app.webhooks.shared-secret:}") String sharedSecret) {
		this.sharedSecret = sharedSecret;
		if (sharedSecret.isBlank()) {
			log.info("Webhook shared secret not configured — all provider webhooks will be rejected (fail closed)");
		}
	}

	@Override
	public boolean verify(HttpServletRequest request, String body) {
		if (sharedSecret.isBlank()) {
			return false;
		}
		String header = request.getHeader("X-Webhook-Signature");
		if (header == null) {
			return false;
		}
		return MessageDigest.isEqual(
				header.getBytes(StandardCharsets.UTF_8),
				sharedSecret.getBytes(StandardCharsets.UTF_8));
	}
}
