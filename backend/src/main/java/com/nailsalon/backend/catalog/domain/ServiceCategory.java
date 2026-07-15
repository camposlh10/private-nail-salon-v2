package com.nailsalon.backend.catalog.domain;

import java.util.Locale;
import java.util.UUID;

import com.nailsalon.backend.shared.persistence.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Grouping of services (Manicure, Pedicure, ...). Names are unique case-insensitively per
 * business via {@code name_normalized}, maintained by {@link #setName}.
 */
@Entity
@Table(name = "service_category")
public class ServiceCategory extends AuditableEntity {

	@Column(name = "business_id", nullable = false)
	private UUID businessId;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(name = "name_normalized", nullable = false, length = 120)
	private String nameNormalized;

	@Column(nullable = false, length = 140)
	private String slug;

	@Column(length = 4000)
	private String description;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ServiceStatus status = ServiceStatus.DRAFT;

	public static String normalizeName(String name) {
		return name.trim().toLowerCase(Locale.ROOT);
	}

	public UUID getBusinessId() {
		return businessId;
	}

	public void setBusinessId(UUID businessId) {
		this.businessId = businessId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		this.nameNormalized = normalizeName(name);
	}

	public String getNameNormalized() {
		return nameNormalized;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(int displayOrder) {
		this.displayOrder = displayOrder;
	}

	public ServiceStatus getStatus() {
		return status;
	}

	public void setStatus(ServiceStatus status) {
		this.status = status;
	}
}
