package com.nailsalon.backend.communications.gateway;

/**
 * Provider-neutral outbound SMS, shaped after Twilio Programmable Messaging.
 * Callers pass an idempotency key (typically the outbox row id) so retries never
 * double-send, and a callback URL for delivery-status webhooks.
 */
public interface SmsGateway {

	record SmsRequest(String to, String body, String statusCallbackUrl, String idempotencyKey) {
	}

	record SmsResult(String providerMessageId) {
	}

	SmsResult send(SmsRequest request);
}
