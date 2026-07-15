package com.nailsalon.backend.communications.gateway;

/**
 * Provider-neutral phone OTP verification, shaped after Twilio Verify so the real
 * implementation can drop in without changing callers. The future booking flow uses
 * this to establish a short-lived verified-phone session; CRM clients are created
 * only after a confirmed booking, never on OTP request.
 */
public interface PhoneVerificationGateway {

	enum Purpose {
		BOOKING
	}

	/** Kick off an OTP to the given E.164 phone number. */
	void startVerification(String phone, Purpose purpose);

	/** @return true when the code matches the pending verification for this phone. */
	boolean checkVerification(String phone, String code);
}
