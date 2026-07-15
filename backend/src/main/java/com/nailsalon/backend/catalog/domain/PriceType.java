package com.nailsalon.backend.catalog.domain;

/** FIXED and STARTING_AT require a positive amount; FREE requires zero. */
public enum PriceType {
	FIXED, STARTING_AT, FREE
}
