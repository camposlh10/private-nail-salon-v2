package com.nailsalon.backend.catalog.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nailsalon.backend.business.BusinessProfileRepository;
import com.nailsalon.backend.catalog.api.admin.AdminDtos.AdminCategory;
import com.nailsalon.backend.catalog.api.admin.AdminDtos.CategoryWrite;
import com.nailsalon.backend.catalog.domain.ServiceCategory;
import com.nailsalon.backend.catalog.domain.ServiceStatus;
import com.nailsalon.backend.catalog.infrastructure.ServiceCategoryRepository;
import com.nailsalon.backend.shared.Slugs;
import com.nailsalon.backend.shared.audit.AuditRecorder;
import com.nailsalon.backend.shared.error.ApiException;

@Service
public class CategoryAdminService {

	private final ServiceCategoryRepository categories;
	private final BusinessProfileRepository businesses;
	private final AuditRecorder audit;

	public CategoryAdminService(ServiceCategoryRepository categories,
			BusinessProfileRepository businesses, AuditRecorder audit) {
		this.categories = categories;
		this.businesses = businesses;
		this.audit = audit;
	}

	@Transactional(readOnly = true)
	public List<AdminCategory> list() {
		return categories.findAllByOrderByDisplayOrderAscNameAsc().stream()
				.map(AdminCategory::from)
				.toList();
	}

	@Transactional
	public AdminCategory create(CategoryWrite request) {
		UUID businessId = requireBusinessId();
		if (categories.existsByBusinessIdAndNameNormalized(businessId,
				ServiceCategory.normalizeName(request.name()))) {
			throw ApiException.conflict("A category with this name already exists");
		}
		ServiceCategory category = new ServiceCategory();
		category.setBusinessId(businessId);
		category.setName(request.name());
		category.setSlug(uniqueSlug(request.name()));
		category.setDescription(request.description());
		category.setDisplayOrder((int) categories.count());
		category.setStatus(ServiceStatus.ACTIVE);
		categories.saveAndFlush(category);
		audit.record("CATEGORY_CREATED", "ServiceCategory", category.getId(), category.getName());
		return AdminCategory.from(category);
	}

	@Transactional
	public AdminCategory update(UUID id, CategoryWrite request) {
		ServiceCategory category = categories.findById(id)
				.orElseThrow(() -> ApiException.notFound("Category not found"));
		requireVersion(request.version(), category.getVersion());
		String normalized = ServiceCategory.normalizeName(request.name());
		categories.findByBusinessIdAndNameNormalized(category.getBusinessId(), normalized)
				.filter(other -> !other.getId().equals(id))
				.ifPresent(other -> {
					throw ApiException.conflict("A category with this name already exists");
				});
		category.setName(request.name());
		category.setDescription(request.description());
		categories.saveAndFlush(category);
		audit.record("CATEGORY_UPDATED", "ServiceCategory", category.getId(), category.getName());
		return AdminCategory.from(category);
	}

	@Transactional
	public AdminCategory changeStatus(UUID id, String status) {
		ServiceCategory category = categories.findById(id)
				.orElseThrow(() -> ApiException.notFound("Category not found"));
		category.setStatus(parseStatus(status));
		categories.saveAndFlush(category);
		audit.record("CATEGORY_STATUS_CHANGED", "ServiceCategory", category.getId(), status);
		return AdminCategory.from(category);
	}

	@Transactional
	public void reorder(List<UUID> orderedIds) {
		for (int i = 0; i < orderedIds.size(); i++) {
			ServiceCategory category = categories.findById(orderedIds.get(i))
					.orElseThrow(() -> ApiException.notFound("Category not found"));
			category.setDisplayOrder(i);
		}
		audit.record("CATEGORIES_REORDERED", "ServiceCategory", null, orderedIds.size() + " categories");
	}

	private UUID requireBusinessId() {
		return businesses.findFirstByOrderByCreatedAtAsc()
				.orElseThrow(() -> ApiException.conflict("Business profile is not configured"))
				.getId();
	}

	private String uniqueSlug(String name) {
		String base = Slugs.slugify(name);
		String slug = base;
		int suffix = 2;
		while (categories.existsBySlug(slug)) {
			slug = base + "-" + suffix++;
		}
		return slug;
	}

	static ServiceStatus parseStatus(String status) {
		try {
			return ServiceStatus.valueOf(status);
		} catch (IllegalArgumentException ex) {
			throw ApiException.badRequest("Unknown status: " + status);
		}
	}

	static void requireVersion(Long requestVersion, long entityVersion) {
		if (requestVersion == null) {
			throw ApiException.badRequest("version is required when editing");
		}
		if (requestVersion != entityVersion) {
			throw ApiException.conflict("The resource was modified by someone else — reload and retry");
		}
	}
}
