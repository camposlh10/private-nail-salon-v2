package com.nailsalon.backend.communications.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Background polling for the outbox. Disabled in the test profile
 * ({@code app.outbox.enabled=false}) where tests call
 * {@link OutboxWorker#processDue()} directly for determinism.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "app.outbox.enabled", havingValue = "true")
public class OutboxScheduler {

	private final OutboxWorker worker;

	public OutboxScheduler(OutboxWorker worker) {
		this.worker = worker;
	}

	@Scheduled(fixedDelayString = "${app.outbox.poll-ms}")
	void poll() {
		worker.processDue();
	}
}
