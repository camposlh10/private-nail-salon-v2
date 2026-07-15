package com.nailsalon.backend.business;

import com.nailsalon.backend.shared.persistence.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * The single salon business. A UNIQUE always-true {@code single_row} column enforces the
 * singleton at the database level; public endpoints resolve this row internally and never
 * accept a businessId.
 */
@Entity
@Table(name = "business_profile")
public class BusinessProfile extends AuditableEntity {

	@Column(name = "single_row", nullable = false)
	private boolean singleRow = true;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(nullable = false, length = 120)
	private String slug;

	@Column(length = 40)
	private String phone;

	@Column(length = 320)
	private String email;

	@Column(nullable = false, length = 64)
	private String timezone;

	@Column(nullable = false, length = 3)
	private String currency = "USD";

	@Column(length = 500)
	private String address;

	@Column(name = "appointment_start_window_minutes", nullable = false)
	private int appointmentStartWindowMinutes = 10;

	@Column(name = "appointment_start_notice", length = 1000)
	private String appointmentStartNotice;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getAppointmentStartWindowMinutes() {
		return appointmentStartWindowMinutes;
	}

	public void setAppointmentStartWindowMinutes(int appointmentStartWindowMinutes) {
		this.appointmentStartWindowMinutes = appointmentStartWindowMinutes;
	}

	public String getAppointmentStartNotice() {
		return appointmentStartNotice;
	}

	public void setAppointmentStartNotice(String appointmentStartNotice) {
		this.appointmentStartNotice = appointmentStartNotice;
	}
}
