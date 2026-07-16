package com.nailsalon.backend.communications.gateway;

/**
 * Provider-neutral outbound email, mirroring {@link SmsGateway}. Callers pass an
 * idempotency key (typically the outbox row id) so retries never double-send. The
 * result's providerMessageId means ACCEPTED by the provider — not delivered.
 */
public interface EmailGateway {

	record EmailRequest(String to, String subject, String body, String idempotencyKey) {
	}

	record EmailResult(String providerMessageId) {
	}

	EmailResult send(EmailRequest request);
}
