package com.nailsalon.backend.catalog.domain;

import java.util.UUID;

import com.nailsalon.backend.shared.error.ApiException;
import com.nailsalon.backend.shared.persistence.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A bookable salon service ("service" table; class named SalonService to avoid colliding
 * with Spring's {@code @Service}). Prices are integer cents. Only ACTIVE services with
 * {@code onlineBookable=true} appear in the public catalog.
 */
@Entity
@Table(name = "service")
public class SalonService extends AuditableEntity {

	@Column(name = "category_id", nullable = false)
	private UUID categoryId;

	@Column(nullable = false, length = 160)
	private String name;

	@Column(nullable = false, length = 180)
	private String slug;

	@Column(length = 4000)
	private String description;

	@Column(name = "duration_minutes", nullable = false)
	private int durationMinutes;

	@Enumerated(EnumType.STRING)
	@Column(name = "price_type", nullable = false, length = 20)
	private PriceType priceType;

	@Column(name = "price_cents", nullable = false)
	private int priceCents;

	@Column(name = "online_bookable", nullable = false)
	private boolean onlineBookable = true;

	@Column(name = "hidden_from_new_clients", nullable = false)
	private boolean hiddenFromNewClients;

	@Column(name = "image_id")
	private UUID imageId;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ServiceStatus status = ServiceStatus.DRAFT;

	/** Domain invariants for duration and pricing. Mirrored by DB CHECK constraints. */
	public static void validatePricing(PriceType priceType, int priceCents, int durationMinutes) {
		if (durationMinutes <= 0) {
			throw ApiException.badRequest("durationMinutes must be positive");
		}
		if (priceType == PriceType.FREE && priceCents != 0) {
			throw ApiException.badRequest("FREE services must have priceCents = 0");
		}
		if (priceType != PriceType.FREE && priceCents <= 0) {
			throw ApiException.badRequest(priceType + " services require a positive priceCents");
		}
	}

	public UUID getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(UUID categoryId) {
		this.categoryId = categoryId;
	}

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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getDurationMinutes() {
		return durationMinutes;
	}

	public void setDurationMinutes(int durationMinutes) {
		this.durationMinutes = durationMinutes;
	}

	public PriceType getPriceType() {
		return priceType;
	}

	public void setPriceType(PriceType priceType) {
		this.priceType = priceType;
	}

	public int getPriceCents() {
		return priceCents;
	}

	public void setPriceCents(int priceCents) {
		this.priceCents = priceCents;
	}

	public boolean isOnlineBookable() {
		return onlineBookable;
	}

	public void setOnlineBookable(boolean onlineBookable) {
		this.onlineBookable = onlineBookable;
	}

	public boolean isHiddenFromNewClients() {
		return hiddenFromNewClients;
	}

	public void setHiddenFromNewClients(boolean hiddenFromNewClients) {
		this.hiddenFromNewClients = hiddenFromNewClients;
	}

	public UUID getImageId() {
		return imageId;
	}

	public void setImageId(UUID imageId) {
		this.imageId = imageId;
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
