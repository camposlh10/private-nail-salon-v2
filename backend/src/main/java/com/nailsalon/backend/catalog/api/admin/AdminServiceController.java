package com.nailsalon.backend.catalog.api.admin;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nailsalon.backend.catalog.api.admin.AdminDtos.AddOnWrite;
import com.nailsalon.backend.catalog.api.admin.AdminDtos.AdminAddOn;
import com.nailsalon.backend.catalog.api.admin.AdminDtos.AdminService;
import com.nailsalon.backend.catalog.api.admin.AdminDtos.PageDto;
import com.nailsalon.backend.catalog.api.admin.AdminDtos.Reorder;
import com.nailsalon.backend.catalog.api.admin.AdminDtos.ServiceWrite;
import com.nailsalon.backend.catalog.api.admin.AdminDtos.StatusChange;
import com.nailsalon.backend.catalog.application.ServiceAdminService;
import com.nailsalon.backend.catalog.domain.ServiceStatus;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/services")
public class AdminServiceController {

	private final ServiceAdminService serviceAdmin;

	public AdminServiceController(ServiceAdminService serviceAdmin) {
		this.serviceAdmin = serviceAdmin;
	}

	@GetMapping
	public PageDto<AdminService> page(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) ServiceStatus status,
			@RequestParam(required = false) UUID categoryId) {
		return serviceAdmin.page(page, size, status, categoryId);
	}

	@PostMapping
	public ResponseEntity<AdminService> create(@Valid @RequestBody ServiceWrite request) {
		AdminService created = serviceAdmin.create(request);
		return ResponseEntity.created(URI.create("/api/v1/admin/services/" + created.id())).body(created);
	}

	@PutMapping("/order")
	public ResponseEntity<Void> reorder(@Valid @RequestBody Reorder request) {
		serviceAdmin.reorder(request.orderedIds());
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}")
	public AdminService get(@PathVariable UUID id) {
		return serviceAdmin.get(id);
	}

	@PutMapping("/{id}")
	public AdminService update(@PathVariable UUID id, @Valid @RequestBody ServiceWrite request) {
		return serviceAdmin.update(id, request);
	}

	@PatchMapping("/{id}/status")
	public AdminService changeStatus(@PathVariable UUID id, @Valid @RequestBody StatusChange request) {
		return serviceAdmin.changeStatus(id, request.status());
	}

	// --- add-ons ------------------------------------------------------------------

	@PostMapping("/{id}/addons")
	public ResponseEntity<AdminAddOn> createAddOn(@PathVariable UUID id,
			@Valid @RequestBody AddOnWrite request) {
		AdminAddOn created = serviceAdmin.createAddOn(id, request);
		return ResponseEntity
				.created(URI.create("/api/v1/admin/services/" + id + "/addons/" + created.id()))
				.body(created);
	}

	@PutMapping("/{id}/addons/{addOnId}")
	public AdminAddOn updateAddOn(@PathVariable UUID id, @PathVariable UUID addOnId,
			@Valid @RequestBody AddOnWrite request) {
		return serviceAdmin.updateAddOn(id, addOnId, request);
	}

	@PatchMapping("/{id}/addons/{addOnId}/status")
	public AdminAddOn changeAddOnStatus(@PathVariable UUID id, @PathVariable UUID addOnId,
			@Valid @RequestBody StatusChange request) {
		return serviceAdmin.changeAddOnStatus(id, addOnId, request.status());
	}
}
