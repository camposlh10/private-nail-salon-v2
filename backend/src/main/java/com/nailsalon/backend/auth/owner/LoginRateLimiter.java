package com.nailsalon.backend.auth.owner;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.nailsalon.backend.shared.error.ApiException;

/**
 * Fixed-window in-memory rate limit for login attempts, keyed by client IP + email.
 * Sufficient for a single-instance deployment; swap for a shared store if the app
 * ever scales horizontally.
 */
@Component
public class LoginRateLimiter {

	private static final int MAX_ATTEMPTS = 10;
	private static final Duration WINDOW = Duration.ofMinutes(15);

	private record Bucket(Instant windowStart, int attempts) {
	}

	private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

	public void checkAllowed(String key) {
		Bucket bucket = buckets.get(key);
		if (bucket != null && !windowExpired(bucket) && bucket.attempts() >= MAX_ATTEMPTS) {
			throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
					"Too many login attempts — try again later");
		}
	}

	public void recordFailure(String key) {
		buckets.compute(key, (k, bucket) ->
				bucket == null || windowExpired(bucket)
						? new Bucket(Instant.now(), 1)
						: new Bucket(bucket.windowStart(), bucket.attempts() + 1));
	}

	public void reset(String key) {
		buckets.remove(key);
	}

	private boolean windowExpired(Bucket bucket) {
		return bucket.windowStart().plus(WINDOW).isBefore(Instant.now());
	}
}
