package com.nailsalon.backend.communications.outbox;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nailsalon.backend.communications.domain.CommunicationChannel;
import com.nailsalon.backend.communications.domain.CommunicationOutbox;
import com.nailsalon.backend.communications.gateway.EmailGateway;
import com.nailsalon.backend.communications.gateway.SmsGateway;
import com.nailsalon.backend.communications.infrastructure.CommunicationOutboxRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Sends PENDING outbox rows through the (currently fake) SMS/email gateways. Rows are
 * claimed under a database lock, each send uses the row id as its idempotency key, and
 * SENT strictly means "accepted by the provider" — delivery is a separate concern
 * (webhooks). Failures retry with exponential backoff until maxAttempts, then FAILED.
 */
@Component
public class OutboxWorker {

	private static final Logger log = LoggerFactory.getLogger(OutboxWorker.class);
	private static final int BATCH_SIZE = 20;

	private final CommunicationOutboxRepository outbox;
	private final SmsGateway smsGateway;
	private final EmailGateway emailGateway;
	private final OutboxProperties properties;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	public OutboxWorker(CommunicationOutboxRepository outbox, SmsGateway smsGateway,
			EmailGateway emailGateway, OutboxProperties properties, ObjectMapper objectMapper, Clock clock) {
		this.outbox = outbox;
		this.smsGateway = smsGateway;
		this.emailGateway = emailGateway;
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	/** One processing pass; returns how many rows were attempted. */
	@Transactional
	public int processDue() {
		Instant now = clock.instant();
		List<CommunicationOutbox> due = outbox.claimDue(CommunicationOutbox.Status.PENDING, now,
				PageRequest.of(0, BATCH_SIZE));
		for (CommunicationOutbox row : due) {
			try {
				String providerMessageId = send(row);
				row.setStatus(CommunicationOutbox.Status.SENT);
				row.setProviderMessageId(providerMessageId);
				row.setLastError(null);
			}
			catch (Exception ex) {
				int attempts = row.getAttempts() + 1;
				row.setAttempts(attempts);
				row.setLastError(truncate(ex.getMessage()));
				if (attempts >= properties.maxAttempts()) {
					row.setStatus(CommunicationOutbox.Status.FAILED);
					log.error("Outbox row {} permanently failed after {} attempts", row.getId(), attempts, ex);
				}
				else {
					long backoffSeconds = properties.baseBackoffSeconds() * (1L << (attempts - 1));
					row.setNextAttemptAt(now.plusSeconds(backoffSeconds));
					log.warn("Outbox row {} failed (attempt {}), retrying in {}s", row.getId(), attempts,
							backoffSeconds, ex);
				}
			}
		}
		return due.size();
	}

	private String send(CommunicationOutbox row) throws Exception {
		JsonNode payload = objectMapper.readTree(row.getPayload());
		// The row id is the idempotency key: a crash between provider-accept and
		// commit re-sends with the same key, and the provider dedupes.
		String idempotencyKey = row.getId().toString();
		if (row.getChannel() == CommunicationChannel.EMAIL) {
			return emailGateway.send(new EmailGateway.EmailRequest(row.getRecipient(),
					payload.path("subject").asText(), payload.path("body").asText(), idempotencyKey))
					.providerMessageId();
		}
		return smsGateway.send(new SmsGateway.SmsRequest(row.getRecipient(),
				payload.path("body").asText(), null, idempotencyKey))
				.providerMessageId();
	}

	private static String truncate(String message) {
		if (message == null) {
			return "unknown error";
		}
		return message.length() > 1000 ? message.substring(0, 1000) : message;
	}
}
