package com.nailsalon.backend.booking;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Booking-engine tuning knobs (see {@code app.booking} in application.yml).
 *
 * @param minNoticeMinutes       earliest bookable start = now + this
 * @param horizonDays            latest bookable date = today + this
 * @param slotGridMinutes        candidate start times snap to this grid
 * @param holdMinutes            how long a slot hold reserves its time
 * @param verifiedSessionMinutes lifetime of a verified-phone session
 */
@ConfigurationProperties(prefix = "app.booking")
public record BookingProperties(
		int minNoticeMinutes,
		int horizonDays,
		int slotGridMinutes,
		int holdMinutes,
		int verifiedSessionMinutes) {
}
