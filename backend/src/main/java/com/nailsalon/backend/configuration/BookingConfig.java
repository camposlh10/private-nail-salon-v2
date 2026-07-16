package com.nailsalon.backend.configuration;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.nailsalon.backend.booking.BookingProperties;
import com.nailsalon.backend.communications.outbox.OutboxProperties;

@Configuration
@EnableConfigurationProperties({ BookingProperties.class, OutboxProperties.class })
public class BookingConfig {

	/**
	 * All booking "now" decisions flow through this Clock so tests can pin time
	 * (DST transitions, hold expiry) with an overriding {@code @Primary} bean.
	 */
	@Bean
	@ConditionalOnMissingBean(Clock.class)
	Clock clock() {
		return Clock.systemUTC();
	}
}
