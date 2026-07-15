package com.nailsalon.backend.catalog.api.publicapi;

import java.util.List;

import com.nailsalon.backend.catalog.domain.PriceType;
import com.nailsalon.backend.shared.Money;

/**
 * Client-visible projections for the anonymous booking site. Deliberately narrow:
 * no ids beyond slugs, no storage keys, no audit/internal fields.
 */
public final class PublicDtos {

	private PublicDtos() {
	}

	public record PublicBusiness(String name, String slug, String phone, String email, String address,
			String timezone, String currency, int appointmentStartWindowMinutes, String appointmentStartNotice) {
	}

	public record PublicCategory(String slug, String name, String description) {
	}

	public record PublicServiceSummary(String slug, String name, String categorySlug, int durationMinutes,
			PriceType priceType, Money price, String imageUrl) {
	}

	public record PublicAddOn(String name, String description, int addedDurationMinutes, Money price) {
	}

	public record PublicServiceDetail(String slug, String name, String categorySlug, int durationMinutes,
			PriceType priceType, Money price, String imageUrl, String description, List<PublicAddOn> addOns) {
	}
}
