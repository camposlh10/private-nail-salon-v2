package com.nailsalon.backend.catalog.domain;

/** Lifecycle for categories and services: archived items are hidden, never deleted. */
public enum ServiceStatus {
	DRAFT, ACTIVE, ARCHIVED
}
