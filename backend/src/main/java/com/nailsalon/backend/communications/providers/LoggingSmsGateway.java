package com.nailsalon.backend.communications.providers;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.nailsalon.backend.communications.gateway.SmsGateway;

/**
 * Local fake for {@link SmsGateway}: logs the message and returns a synthetic provider
 * message id. Swap for Twilio Programmable Messaging in production (requires A2P 10DLC
 * registration for US long-code traffic — tracked as a production-readiness task).
 */
@Component
public class LoggingSmsGateway implements SmsGateway {

	private static final Logger log = LoggerFactory.getLogger(LoggingSmsGateway.class);

	@Override
	public SmsResult send(SmsRequest request) {
		String providerMessageId = "fake-" + UUID.randomUUID();
		log.info("[FAKE SMS] to={} idempotencyKey={} providerMessageId={} body={}",
				request.to(), request.idempotencyKey(), providerMessageId, request.body());
		return new SmsResult(providerMessageId);
	}
}
