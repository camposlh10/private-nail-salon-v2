package com.nailsalon.backend.communications.providers;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.nailsalon.backend.communications.gateway.EmailGateway;

/**
 * Local fake for {@link EmailGateway}: logs the message and returns a synthetic
 * provider message id — the exact email twin of {@link LoggingSmsGateway}. Swap for a
 * real provider (SES, Postmark, ...) behind a config property when going live; no real
 * email is ever sent by this class.
 */
@Component
public class LoggingEmailGateway implements EmailGateway {

	private static final Logger log = LoggerFactory.getLogger(LoggingEmailGateway.class);

	@Override
	public EmailResult send(EmailRequest request) {
		String providerMessageId = "fake-email-" + UUID.randomUUID();
		log.info("[FAKE EMAIL] to={} idempotencyKey={} providerMessageId={} subject={} body={}",
				request.to(), request.idempotencyKey(), providerMessageId, request.subject(), request.body());
		return new EmailResult(providerMessageId);
	}
}
