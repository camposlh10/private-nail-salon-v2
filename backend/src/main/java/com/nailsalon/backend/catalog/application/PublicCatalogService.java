package com.nailsalon.backend.catalog.application;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nailsalon.backend.business.BusinessProfile;
import com.nailsalon.backend.business.BusinessProfileRepository;
import com.nailsalon.backend.catalog.api.publicapi.PublicDtos.PublicAddOn;
import com.nailsalon.backend.catalog.api.publicapi.PublicDtos.PublicBusiness;
import com.nailsalon.backend.catalog.api.publicapi.PublicDtos.PublicCategory;
import com.nailsalon.backend.catalog.api.publicapi.PublicDtos.PublicServiceDetail;
import com.nailsalon.backend.catalog.api.publicapi.PublicDtos.PublicServiceSummary;
import com.nailsalon.backend.catalog.domain.AddOnStatus;
import com.nailsalon.backend.catalog.domain.SalonService;
import com.nailsalon.backend.catalog.domain.ServiceCategory;
import com.nailsalon.backend.catalog.domain.ServiceStatus;
import com.nailsalon.backend.catalog.infrastructure.SalonServiceRepository;
import com.nailsalon.backend.catalog.infrastructure.ServiceAddOnRepository;
import com.nailsalon.backend.catalog.infrastructure.ServiceCategoryRepository;
import com.nailsalon.backend.shared.Money;
import com.nailsalon.backend.shared.error.ApiException;

/**
 * Read-side of the public catalog. Only ACTIVE, online-bookable services inside ACTIVE
 * categories are visible. A single salon's catalog is tens of rows, so category/search
 * filters run in memory over the active set instead of bespoke SQL.
 */
@Service
public class PublicCatalogService {

	private final BusinessProfileRepository businesses;
	private final ServiceCategoryRepository categories;
	private final SalonServiceRepository services;
	private final ServiceAddOnRepository addOns;

	public PublicCatalogService(BusinessProfileRepository businesses, ServiceCategoryRepository categories,
			SalonServiceRepository services, ServiceAddOnRepository addOns) {
		this.businesses = businesses;
		this.categories = categories;
		this.services = services;
		this.addOns = addOns;
	}

	@Transactional(readOnly = true)
	public PublicBusiness business() {
		BusinessProfile b = requireBusiness();
		return new PublicBusiness(b.getName(), b.getSlug(), b.getPhone(), b.getEmail(), b.getAddress(),
				b.getTimezone(), b.getCurrency(), b.getAppointmentStartWindowMinutes(),
				b.getAppointmentStartNotice());
	}

	@Transactional(readOnly = true)
	public List<PublicCategory> categories() {
		return activeCategories().stream()
				.map(c -> new PublicCategory(c.getSlug(), c.getName(), c.getDescription()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<PublicServiceSummary> services(String categorySlug, String query) {
		String currency = requireBusiness().getCurrency();
		Map<UUID, String> activeCategorySlugs = activeCategories().stream()
				.collect(Collectors.toMap(ServiceCategory::getId, ServiceCategory::getSlug));
		return activeServices().stream()
				.filter(s -> activeCategorySlugs.containsKey(s.getCategoryId()))
				.filter(s -> categorySlug == null || categorySlug.equals(activeCategorySlugs.get(s.getCategoryId())))
				.filter(s -> matches(s, query))
				.map(s -> new PublicServiceSummary(s.getSlug(), s.getName(),
						activeCategorySlugs.get(s.getCategoryId()), s.getDurationMinutes(), s.getPriceType(),
						new Money(s.getPriceCents(), currency), imageUrl(s)))
				.toList();
	}

	@Transactional(readOnly = true)
	public PublicServiceDetail service(String slug) {
		String currency = requireBusiness().getCurrency();
		SalonService s = services.findBySlug(slug)
				.filter(svc -> svc.getStatus() == ServiceStatus.ACTIVE && svc.isOnlineBookable())
				.orElseThrow(() -> ApiException.notFound("Service not found"));
		ServiceCategory category = categories.findById(s.getCategoryId())
				.filter(c -> c.getStatus() == ServiceStatus.ACTIVE)
				.orElseThrow(() -> ApiException.notFound("Service not found"));

		List<PublicAddOn> activeAddOns = addOns
				.findByServiceIdAndStatusOrderByDisplayOrderAscNameAsc(s.getId(), AddOnStatus.ACTIVE)
				.stream()
				.map(a -> new PublicAddOn(a.getName(), a.getDescription(), a.getAddedDurationMinutes(),
						new Money(a.getPriceCents(), currency)))
				.toList();

		return new PublicServiceDetail(s.getSlug(), s.getName(), category.getSlug(), s.getDurationMinutes(),
				s.getPriceType(), new Money(s.getPriceCents(), currency), imageUrl(s), s.getDescription(),
				activeAddOns);
	}

	private List<ServiceCategory> activeCategories() {
		return categories.findByStatusOrderByDisplayOrderAscNameAsc(ServiceStatus.ACTIVE);
	}

	private List<SalonService> activeServices() {
		return services.findByStatusAndOnlineBookableTrueOrderByDisplayOrderAscNameAsc(ServiceStatus.ACTIVE);
	}

	private static boolean matches(SalonService s, String query) {
		if (query == null || query.isBlank()) {
			return true;
		}
		String q = query.toLowerCase(Locale.ROOT);
		return s.getName().toLowerCase(Locale.ROOT).contains(q)
				|| (s.getDescription() != null && s.getDescription().toLowerCase(Locale.ROOT).contains(q));
	}

	private static String imageUrl(SalonService s) {
		return s.getImageId() != null ? "/api/v1/public/media/" + s.getImageId() : null;
	}

	private BusinessProfile requireBusiness() {
		return businesses.findFirstByOrderByCreatedAtAsc()
				.orElseThrow(() -> ApiException.notFound("Business is not configured"));
	}
}
