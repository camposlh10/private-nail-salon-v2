package com.nailsalon.backend.communications.webhooks;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Skeleton webhook endpoints for the SMS provider (Twilio-shaped):
 * - /inbound  — client replies (will create/reuse a Conversation in the CRM milestone)
 * - /status   — outbound delivery lifecycle (SENT/DELIVERED/FAILED on the outbox row)
 *
 * Already implemented per the webhook contract: signature validation (fail closed),
 * provider-message-id deduplication, redacted logging, and a fast 2xx. Actual
 * processing is intentionally a TODO for the booking/communications milestone and
 * will run asynchronously off this request thread.
 */
@RestController
@RequestMapping("/api/v1/webhooks/sms")
public class SmsWebhookController {

	private static final Logger log = LoggerFactory.getLogger(SmsWebhookController.class);

	private final WebhookSignatureVerifier signatureVerifier;

	// In-memory replay guard; becomes a DB table (unique provider_message_id) when
	// real processing lands. Bounded to avoid unbounded growth.
	private final Set<String> seenMessageIds =
			Collections.synchronizedSet(Collections.newSetFromMap(new BoundedMap<>(10_000)));

	public SmsWebhookController(WebhookSignatureVerifier signatureVerifier) {
		this.signatureVerifier = signatureVerifier;
	}

	@PostMapping("/inbound")
	public ResponseEntity<Void> inbound(HttpServletRequest request, @RequestBody(required = false) String body) {
		return handle(request, body, "inbound");
	}

	@PostMapping("/status")
	public ResponseEntity<Void> status(HttpServletRequest request, @RequestBody(required = false) String body) {
		return handle(request, body, "status");
	}

	private ResponseEntity<Void> handle(HttpServletRequest request, String body, String kind) {
		if (!signatureVerifier.verify(request, body)) {
			return ResponseEntity.status(401).build();
		}
		String messageId = request.getHeader("X-Provider-Message-Id");
		if (messageId != null && !seenMessageIds.add(messageId)) {
			log.debug("Duplicate {} webhook for provider message {} — ignored", kind, messageId);
			return ResponseEntity.noContent().build();
		}
		// Redacted: never log full payloads (they contain phone numbers / message text).
		log.info("Accepted {} SMS webhook (providerMessageId={}, {} bytes) — processing TODO",
				kind, messageId, body != null ? body.length() : 0);
		return ResponseEntity.noContent().build();
	}

	/** Simple LRU bound so the replay guard can't grow forever. */
	private static final class BoundedMap<K, V> extends LinkedHashMap<K, V> {

		private final int maxEntries;

		private BoundedMap(int maxEntries) {
			super(16, 0.75f, false);
			this.maxEntries = maxEntries;
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
			return size() > maxEntries;
		}
	}
}
