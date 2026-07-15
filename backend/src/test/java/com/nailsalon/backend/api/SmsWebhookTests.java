package com.nailsalon.backend.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SmsWebhookTests {

	// Matches app.webhooks.shared-secret in the test profile.
	private static final String SECRET = "test-webhook-secret";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void unsignedWebhooksAreRejected() throws Exception {
		mockMvc.perform(post("/api/v1/webhooks/sms/inbound")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"From\":\"+15550100200\",\"Body\":\"hi\"}"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/v1/webhooks/sms/inbound")
						.header("X-Webhook-Signature", "wrong-secret")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void signedWebhooksAreAcceptedWithoutSessionOrCsrf() throws Exception {
		mockMvc.perform(post("/api/v1/webhooks/sms/status")
						.header("X-Webhook-Signature", SECRET)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"MessageStatus\":\"delivered\"}"))
				.andExpect(status().isNoContent());
	}

	@Test
	void duplicateProviderMessageIdsAreDeduplicated() throws Exception {
		for (int i = 0; i < 2; i++) {
			mockMvc.perform(post("/api/v1/webhooks/sms/inbound")
							.header("X-Webhook-Signature", SECRET)
							.header("X-Provider-Message-Id", "SM-duplicate-1")
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"Body\":\"hello\"}"))
					.andExpect(status().isNoContent());
		}
	}
}
