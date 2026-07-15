package com.nailsalon.backend.catalog.application;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nailsalon.backend.catalog.api.admin.AdminDtos.AddOnWrite;
import com.nailsalon.backend.catalog.api.admin.AdminDtos.AdminAddOn;
import com.nailsalon.backend.catalog.api.admin.AdminDtos.AdminService;
import com.nailsalon.backend.catalog.api.admin.AdminDtos.PageDto;
import com.nailsalon.backend.catalog.api.admin.AdminDtos.ServiceWrite;
import com.nailsalon.backend.catalog.domain.AddOnStatus;
import com.nailsalon.backend.catalog.domain.SalonService;
import com.nailsalon.backend.catalog.domain.ServiceAddOn;
import com.nailsalon.backend.catalog.domain.ServiceStatus;
import com.nailsalon.backend.catalog.infrastructure.SalonServiceRepository;
import com.nailsalon.backend.catalog.infrastructure.ServiceAddOnRepository;
import com.nailsalon.backend.catalog.infrastructure.ServiceCategoryRepository;
import com.nailsalon.backend.shared.Slugs;
import com.nailsalon.backend.shared.audit.AuditRecorder;
import com.nailsalon.backend.shared.error.ApiException;

@Service
public class ServiceAdminService {

	private final SalonServiceRepository services;
	private final ServiceCategoryRepository categories;
	private final ServiceAddOnRepository addOns;
	private final AuditRecorder audit;

	public ServiceAdminService(SalonServiceRepository services, ServiceCategoryRepository categories,
			ServiceAddOnRepository addOns, AuditRecorder audit) {
		this.services = services;
		this.categories = categories;
		this.addOns = addOns;
		this.audit = audit;
	}

	@Transactional(readOnly = true)
	public PageDto<AdminService> page(int page, int size, ServiceStatus status, UUID categoryId) {
		Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100),
				Sort.by("displayOrder").ascending().and(Sort.by("name").ascending()));
		Page<SalonService> result;
		if (categoryId != null && status != null) {
			result = services.findByCategoryIdAndStatus(categoryId, status, pageable);
		} else if (categoryId != null) {
			result = services.findByCategoryId(categoryId, pageable);
		} else if (status != null) {
			result = services.findByStatus(status, pageable);
		} else {
			result = services.findAll(pageable);
		}
		List<AdminService> content = result.getContent().stream().map(this::toDto).toList();
		return new PageDto<>(content, result.getNumber(), result.getSize(),
				result.getTotalElements(), result.getTotalPages());
	}

	@Transactional(readOnly = true)
	public AdminService get(UUID id) {
		return toDto(requireService(id));
	}

	@Transactional
	public AdminService create(ServiceWrite request) {
		if (!categories.existsById(request.categoryId())) {
			throw ApiException.badRequest("Unknown categoryId");
		}
		SalonService.validatePricing(request.priceType(), request.priceCentsOrZero(), request.durationMinutes());
		SalonService service = new SalonService();
		service.setCategoryId(request.categoryId());
		service.setName(request.name());
		service.setSlug(uniqueSlug(request.name()));
		applyEditableFields(service, request);
		service.setDisplayOrder((int) services.countByCategoryId(request.categoryId()));
		service.setStatus(ServiceStatus.ACTIVE);
		services.saveAndFlush(service);
		audit.record("SERVICE_CREATED", "SalonService", service.getId(), service.getName());
		return toDto(service);
	}

	@Transactional
	public AdminService update(UUID id, ServiceWrite request) {
		SalonService service = requireService(id);
		CategoryAdminService.requireVersion(request.version(), service.getVersion());
		if (!categories.existsById(request.categoryId())) {
			throw ApiException.badRequest("Unknown categoryId");
		}
		SalonService.validatePricing(request.priceType(), request.priceCentsOrZero(), request.durationMinutes());
		service.setCategoryId(request.categoryId());
		service.setName(request.name());
		applyEditableFields(service, request);
		services.saveAndFlush(service);
		audit.record("SERVICE_UPDATED", "SalonService", service.getId(), service.getName());
		return toDto(service);
	}

	@Transactional
	public AdminService changeStatus(UUID id, String status) {
		SalonService service = requireService(id);
		service.setStatus(CategoryAdminService.parseStatus(status));
		services.saveAndFlush(service);
		audit.record("SERVICE_STATUS_CHANGED", "SalonService", service.getId(), status);
		return toDto(service);
	}

	@Transactional
	public void reorder(List<UUID> orderedIds) {
		for (int i = 0; i < orderedIds.size(); i++) {
			requireService(orderedIds.get(i)).setDisplayOrder(i);
		}
		audit.record("SERVICES_REORDERED", "SalonService", null, orderedIds.size() + " services");
	}

	// --- add-ons ---------------------------------------------------------------------

	@Transactional
	public AdminAddOn createAddOn(UUID serviceId, AddOnWrite request) {
		SalonService service = requireService(serviceId);
		ServiceAddOn addOn = new ServiceAddOn();
		addOn.setServiceId(service.getId());
		applyAddOnFields(addOn, request);
		addOn.setDisplayOrder(addOns.findByServiceIdOrderByDisplayOrderAscNameAsc(serviceId).size());
		addOns.saveAndFlush(addOn);
		audit.record("ADDON_CREATED", "ServiceAddOn", addOn.getId(), addOn.getName());
		return AdminAddOn.from(addOn);
	}

	@Transactional
	public AdminAddOn updateAddOn(UUID serviceId, UUID addOnId, AddOnWrite request) {
		ServiceAddOn addOn = requireAddOn(serviceId, addOnId);
		CategoryAdminService.requireVersion(request.version(), addOn.getVersion());
		applyAddOnFields(addOn, request);
		addOns.saveAndFlush(addOn);
		audit.record("ADDON_UPDATED", "ServiceAddOn", addOn.getId(), addOn.getName());
		return AdminAddOn.from(addOn);
	}

	@Transactional
	public AdminAddOn changeAddOnStatus(UUID serviceId, UUID addOnId, String status) {
		ServiceAddOn addOn = requireAddOn(serviceId, addOnId);
		try {
			addOn.setStatus(AddOnStatus.valueOf(status));
		} catch (IllegalArgumentException ex) {
			throw ApiException.badRequest("Unknown status: " + status);
		}
		addOns.saveAndFlush(addOn);
		audit.record("ADDON_STATUS_CHANGED", "ServiceAddOn", addOn.getId(), status);
		return AdminAddOn.from(addOn);
	}

	// --- helpers ----------------------------------------------------------------------

	private void applyEditableFields(SalonService service, ServiceWrite request) {
		service.setDescription(request.description());
		service.setDurationMinutes(request.durationMinutes());
		service.setPriceType(request.priceType());
		service.setPriceCents(request.priceCentsOrZero());
		service.setOnlineBookable(request.onlineBookable() == null || request.onlineBookable());
		service.setHiddenFromNewClients(Boolean.TRUE.equals(request.hiddenFromNewClients()));
		service.setImageId(request.imageId());
	}

	private void applyAddOnFields(ServiceAddOn addOn, AddOnWrite request) {
		addOn.setName(request.name());
		addOn.setDescription(request.description());
		addOn.setAddedDurationMinutes(request.addedDurationMinutes() != null ? request.addedDurationMinutes() : 0);
		addOn.setPriceCents(request.priceCents() != null ? request.priceCents() : 0);
	}

	private SalonService requireService(UUID id) {
		return services.findById(id).orElseThrow(() -> ApiException.notFound("Service not found"));
	}

	private ServiceAddOn requireAddOn(UUID serviceId, UUID addOnId) {
		ServiceAddOn addOn = addOns.findById(addOnId)
				.orElseThrow(() -> ApiException.notFound("Add-on not found"));
		if (!addOn.getServiceId().equals(serviceId)) {
			throw ApiException.notFound("Add-on not found");
		}
		return addOn;
	}

	private AdminService toDto(SalonService service) {
		return AdminService.from(service,
				addOns.findByServiceIdOrderByDisplayOrderAscNameAsc(service.getId()));
	}

	private String uniqueSlug(String name) {
		String base = Slugs.slugify(name);
		String slug = base;
		int suffix = 2;
		while (services.existsBySlug(slug)) {
			slug = base + "-" + suffix++;
		}
		return slug;
	}
}
