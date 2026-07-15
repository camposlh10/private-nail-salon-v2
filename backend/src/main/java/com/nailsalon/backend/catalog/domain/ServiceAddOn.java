package com.nailsalon.backend.catalog.domain;

import java.util.UUID;

import com.nailsalon.backend.shared.persistence.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** Optional extra belonging to exactly one service (V1). May add duration, price, or both. */
@Entity
@Table(name = "service_add_on")
public class ServiceAddOn extends AuditableEntity {

	@Column(name = "service_id", nullable = false)
	private UUID serviceId;

	@Column(nullable = false, length = 160)
	private String name;

	@Column(length = 2000)
	private String description;

	@Column(name = "added_duration_minutes", nullable = false)
	private int addedDurationMinutes;

	@Column(name = "price_cents", nullable = false)
	private int priceCents;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AddOnStatus status = AddOnStatus.ACTIVE;

	public UUID getServiceId() {
		return serviceId;
	}

	public void setServiceId(UUID serviceId) {
		this.serviceId = serviceId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getAddedDurationMinutes() {
		return addedDurationMinutes;
	}

	public void setAddedDurationMinutes(int addedDurationMinutes) {
		this.addedDurationMinutes = addedDurationMinutes;
	}

	public int getPriceCents() {
		return priceCents;
	}

	public void setPriceCents(int priceCents) {
		this.priceCents = priceCents;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(int displayOrder) {
		this.displayOrder = displayOrder;
	}

	public AddOnStatus getStatus() {
		return status;
	}

	public void setStatus(AddOnStatus status) {
		this.status = status;
	}
}
