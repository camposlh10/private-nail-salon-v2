package com.nailsalon.backend.communications.outbox;

import java.time.Clock;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.nailsalon.backend.communications.domain.CommunicationChannel;
import com.nailsalon.backend.communications.domain.CommunicationOutbox;
import com.nailsalon.backend.communications.infrastructure.CommunicationOutboxRepository;

import tools.jackson.databind.ObjectMapper;

/**
 * Writes outbox rows INSIDE the caller's transaction — that is the whole point of the
 * outbox pattern: if the booking rolls back, so do its notifications. The background
 * {@link OutboxWorker} sends them later.
 */
@Component
public class OutboxMessages {

	private final CommunicationOutboxRepository outbox;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	public OutboxMessages(CommunicationOutboxRepository outbox, ObjectMapper objectMapper, Clock clock) {
		this.outbox = outbox;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	public void enqueueSms(String toPhone, String messageType, String body) {
		enqueue(CommunicationChannel.SMS, toPhone, messageType, Map.of("body", body));
	}

	public void enqueueEmail(String toEmail, String messageType, String subject, String body) {
		enqueue(CommunicationChannel.EMAIL, toEmail, messageType, Map.of("subject", subject, "body", body));
	}

	private void enqueue(CommunicationChannel channel, String recipient, String messageType,
			Map<String, String> payload) {
		CommunicationOutbox row = new CommunicationOutbox();
		row.setChannel(channel);
		row.setRecipient(recipient);
		row.setMessageType(messageType);
		row.setPayload(toJson(payload));
		row.setStatus(CommunicationOutbox.Status.PENDING);
		row.setNextAttemptAt(clock.instant());
		outbox.save(row);
	}

	private String toJson(Map<String, String> payload) {
		return objectMapper.writeValueAsString(payload);
	}
}
