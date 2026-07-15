package com.nailsalon.backend.communications.domain;

/** Marketing consent stays separate from transactional even though marketing is outside V1. */
public enum ConsentPurpose {
	TRANSACTIONAL, MARKETING
}
