package com.nailsalon.backend.booking.domain;

import com.nailsalon.backend.shared.persistence.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A booking customer, created (or matched) only after a confirmed booking with a
 * verified phone. The E.164 phone is the natural identity — one client per phone.
 */
@Entity
@Table(name = "client")
public class Client extends AuditableEntity {

	@Column(nullable = false, length = 200)
	private String name;

	@Column(name = "phone_e164", nullable = false, length = 20, unique = true)
	private String phoneE164;

	@Column(length = 320)
	private String email;

	@Column(length = 4000)
	private String notes;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPhoneE164() {
		return phoneE164;
	}

	public void setPhoneE164(String phoneE164) {
		this.phoneE164 = phoneE164;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}
}
