package com.nailsalon.backend.shared;

/** Money as integer minor units (cents) — never floating point. */
public record Money(int amountCents, String currency) {

	public static Money usd(int amountCents) {
		return new Money(amountCents, "USD");
	}
}
