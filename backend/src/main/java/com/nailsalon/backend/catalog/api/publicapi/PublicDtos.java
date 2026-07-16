package com.nailsalon.backend.catalog.api.publicapi;

import java.util.List;
import java.util.UUID;

import com.nailsalon.backend.catalog.domain.PriceType;
import com.nailsalon.backend.shared.Money;

/**
 * Client-visible projections for the anonymous booking site. Deliberately narrow: no
 * storage keys, no audit/internal fields. Service/add-on ids are exposed because the
 * availability and slot-hold endpoints key on them.
 */
public final class PublicDtos {

	private PublicDtos() {
	}

	public record PublicBusiness(String name, String slug, String phone, String email, String address,
			String timezone, String currency, int appointmentStartWindowMinutes, String appointmentStartNotice) {
	}

	public record PublicCategory(String slug, String name, String description) {
	}

	public record PublicServiceSummary(UUID id, String slug, String name, String categorySlug,
			int durationMinutes, PriceType priceType, Money price, String imageUrl) {
	}

	public record PublicAddOn(UUID id, String name, String description, int addedDurationMinutes, Money price) {
	}

	public record PublicServiceDetail(UUID id, String slug, String name, String categorySlug,
			int durationMinutes, PriceType priceType, Money price, String imageUrl, String description,
			List<PublicAddOn> addOns) {
	}
}
