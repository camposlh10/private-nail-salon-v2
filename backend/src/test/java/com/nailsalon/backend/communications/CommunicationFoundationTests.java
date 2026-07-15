package com.nailsalon.backend.communications;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.nailsalon.backend.communications.domain.CommunicationChannel;
import com.nailsalon.backend.communications.domain.CommunicationConsent;
import com.nailsalon.backend.communications.domain.CommunicationOutbox;
import com.nailsalon.backend.communications.domain.ConsentPurpose;
import com.nailsalon.backend.communications.gateway.PhoneVerificationGateway;
import com.nailsalon.backend.communications.gateway.SmsGateway;
import com.nailsalon.backend.communications.infrastructure.CommunicationConsentRepository;
import com.nailsalon.backend.communications.infrastructure.CommunicationOutboxRepository;
import com.nailsalon.backend.communications.providers.LoggingPhoneVerificationGateway;

@SpringBootTest
@ActiveProfiles("test")
class CommunicationFoundationTests {

	@Autowired
	private PhoneVerificationGateway verificationGateway;
	@Autowired
	private LoggingPhoneVerificationGateway fakeVerification;
	@Autowired
	private SmsGateway smsGateway;
	@Autowired
	private CommunicationConsentRepository consents;
	@Autowired
	private CommunicationOutboxRepository outbox;

	@Test
	void fakeOtpFlowVerifiesTheLoggedCodeExactlyOnce() {
		String phone = "+15550100200";
		verificationGateway.startVerification(phone, PhoneVerificationGateway.Purpose.BOOKING);
		String code = fakeVerification.pendingCodeFor(phone);

		assertThat(code).hasSize(6);
		assertThat(verificationGateway.checkVerification(phone, "999999")).isFalse();
		assertThat(verificationGateway.checkVerification(phone, code)).isTrue();
		// Single use — replaying the same code fails.
		assertThat(verificationGateway.checkVerification(phone, code)).isFalse();
	}

	@Test
	void fakeSmsGatewayReturnsSyntheticProviderIdWithoutSendingAnything() {
		SmsGateway.SmsResult result = smsGateway.send(new SmsGateway.SmsRequest(
				"+15550100200", "Your appointment is confirmed", "/api/v1/webhooks/sms/status", "outbox-1"));
		assertThat(result.providerMessageId()).startsWith("fake-");
	}

	@Test
	void consentAndOutboxModelsRoundTrip() {
		CommunicationConsent consent = new CommunicationConsent();
		consent.setPhone("+15550100200");
		consent.setChannel(CommunicationChannel.SMS);
		consent.setPurpose(ConsentPurpose.TRANSACTIONAL);
		consent.setGranted(true);
		consent.setDisclosureVersion("2026-07-v1");
		consent.setCapturedAt(Instant.now());
		consents.saveAndFlush(consent);

		assertThat(consents.findByPhoneAndChannelAndPurpose("+15550100200",
				CommunicationChannel.SMS, ConsentPurpose.TRANSACTIONAL))
				.isPresent()
				.hasValueSatisfying(saved -> assertThat(saved.isGranted()).isTrue());

		CommunicationOutbox message = new CommunicationOutbox();
		message.setMessageType("APPOINTMENT_CONFIRMED");
		message.setRecipient("+15550100200");
		message.setPayload("{\"service\":\"Gel Manicure\"}");
		message.setNextAttemptAt(Instant.now());
		outbox.saveAndFlush(message);

		assertThat(outbox.findTop20ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(
				CommunicationOutbox.Status.PENDING, Instant.now().plusSeconds(1)))
				.extracting(CommunicationOutbox::getMessageType)
				.contains("APPOINTMENT_CONFIRMED");
	}
}
