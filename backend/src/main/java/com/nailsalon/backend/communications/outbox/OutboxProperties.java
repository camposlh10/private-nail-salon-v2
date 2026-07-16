package com.nailsalon.backend.communications.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Outbox sender tuning (see {@code app.outbox} in application.yml).
 *
 * @param enabled            background polling on/off (tests invoke the worker directly)
 * @param pollMs             poll interval for PENDING rows
 * @param maxAttempts        after this many failed tries a row goes FAILED for good
 * @param baseBackoffSeconds retry n waits base * 2^(n-1) seconds
 */
@ConfigurationProperties(prefix = "app.outbox")
public record OutboxProperties(boolean enabled, long pollMs, int maxAttempts, long baseBackoffSeconds) {
}
