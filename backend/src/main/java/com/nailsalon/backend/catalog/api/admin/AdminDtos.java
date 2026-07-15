package com.nailsalon.backend.catalog.api.admin;

import java.util.List;
import java.util.UUID;

import com.nailsalon.backend.catalog.domain.PriceType;
import com.nailsalon.backend.catalog.domain.AddOnStatus;
import com.nailsalon.backend.catalog.domain.SalonService;
import com.nailsalon.backend.catalog.domain.ServiceAddOn;
import com.nailsalon.backend.catalog.domain.ServiceCategory;
import com.nailsalon.backend.catalog.domain.ServiceStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Request/response records for the owner CRM API, mirroring openapi.yaml. */
public final class AdminDtos {

	private AdminDtos() {
	}

	// --- requests -----------------------------------------------------------------

	public record CategoryWrite(
			@NotBlank @Size(max = 120) String name,
			@Size(max = 4000) String description,
			Long version) {
	}

	public record ServiceWrite(
			@NotNull UUID categoryId,
			@NotBlank @Size(max = 160) String name,
			@Size(max = 4000) String description,
			@NotNull @Positive Integer durationMinutes,
			@NotNull PriceType priceType,
			@Min(0) Integer priceCents,
			Boolean onlineBookable,
			Boolean hiddenFromNewClients,
			UUID imageId,
			Long version) {

		public int priceCentsOrZero() {
			return priceCents != null ? priceCents : 0;
		}
	}

	public record AddOnWrite(
			@NotBlank @Size(max = 160) String name,
			@Size(max = 2000) String description,
			@Min(0) Integer addedDurationMinutes,
			@Min(0) Integer priceCents,
			Long version) {
	}

	public record StatusChange(@NotBlank String status) {
	}

	public record Reorder(@NotEmpty List<UUID> orderedIds) {
	}

	// --- responses ----------------------------------------------------------------

	public record AdminCategory(UUID id, String name, String slug, String description,
			int displayOrder, ServiceStatus status, long version) {

		public static AdminCategory from(ServiceCategory c) {
			return new AdminCategory(c.getId(), c.getName(), c.getSlug(), c.getDescription(),
					c.getDisplayOrder(), c.getStatus(), c.getVersion());
		}
	}

	public record AdminAddOn(UUID id, UUID serviceId, String name, String description,
			int addedDurationMinutes, int priceCents, int displayOrder, AddOnStatus status, long version) {

		public static AdminAddOn from(ServiceAddOn a) {
			return new AdminAddOn(a.getId(), a.getServiceId(), a.getName(), a.getDescription(),
					a.getAddedDurationMinutes(), a.getPriceCents(), a.getDisplayOrder(), a.getStatus(),
					a.getVersion());
		}
	}

	public record AdminService(UUID id, UUID categoryId, String name, String slug, String description,
			int durationMinutes, PriceType priceType, int priceCents, boolean onlineBookable,
			boolean hiddenFromNewClients, UUID imageId, int displayOrder, ServiceStatus status,
			long version, List<AdminAddOn> addOns) {

		public static AdminService from(SalonService s, List<ServiceAddOn> addOns) {
			return new AdminService(s.getId(), s.getCategoryId(), s.getName(), s.getSlug(),
					s.getDescription(), s.getDurationMinutes(), s.getPriceType(), s.getPriceCents(),
					s.isOnlineBookable(), s.isHiddenFromNewClients(), s.getImageId(), s.getDisplayOrder(),
					s.getStatus(), s.getVersion(), addOns.stream().map(AdminAddOn::from).toList());
		}
	}

	public record PageDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
	}
}
