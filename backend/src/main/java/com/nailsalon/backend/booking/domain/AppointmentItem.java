package com.nailsalon.backend.booking.domain;

import java.util.UUID;

import com.nailsalon.backend.shared.persistence.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Immutable per-line snapshot (the service plus each add-on) of name, duration and
 * price at booking time. serviceId/addOnId are traceability pointers only — later
 * catalog edits or deletions never change what the client agreed to.
 */
@Entity
@Table(name = "appointment_item")
public class AppointmentItem extends AuditableEntity {

	public enum ItemType {
		SERVICE, ADD_ON
	}

	@Column(name = "appointment_id", nullable = false)
	private UUID appointmentId;

	@Enumerated(EnumType.STRING)
	@Column(name = "item_type", nullable = false, length = 10)
	private ItemType itemType;

	@Column(name = "service_id")
	private UUID serviceId;

	@Column(name = "add_on_id")
	private UUID addOnId;

	@Column(nullable = false, length = 160)
	private String name;

	@Column(name = "duration_minutes", nullable = false)
	private int durationMinutes;

	@Column(name = "price_cents", nullable = false)
	private int priceCents;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	public UUID getAppointmentId() {
		return appointmentId;
	}

	public void setAppointmentId(UUID appointmentId) {
		this.appointmentId = appointmentId;
	}

	public ItemType getItemType() {
		return itemType;
	}

	public void setItemType(ItemType itemType) {
		this.itemType = itemType;
	}

	public UUID getServiceId() {
		return serviceId;
	}

	public void setServiceId(UUID serviceId) {
		this.serviceId = serviceId;
	}

	public UUID getAddOnId() {
		return addOnId;
	}

	public void setAddOnId(UUID addOnId) {
		this.addOnId = addOnId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getDurationMinutes() {
		return durationMinutes;
	}

	public void setDurationMinutes(int durationMinutes) {
		this.durationMinutes = durationMinutes;
	}

	public int getPriceCents() {
		return priceCents;
	}

	public void setPriceCents(int priceCents) {
		this.priceCents = priceCents;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}
}
