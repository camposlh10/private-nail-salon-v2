package com.nailsalon.backend.communications.webhooks;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Validates that a webhook call genuinely comes from the messaging provider.
 * The Twilio implementation will verify X-Twilio-Signature (HMAC over URL + params);
 * until then a shared-secret fake stands in. Fails closed when unconfigured.
 */
public interface WebhookSignatureVerifier {

	boolean verify(HttpServletRequest request, String body);
}
