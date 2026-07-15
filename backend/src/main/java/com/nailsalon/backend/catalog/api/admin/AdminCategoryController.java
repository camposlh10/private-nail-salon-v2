package com.nailsalon.backend.catalog.api.admin;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nailsalon.backend.catalog.api.admin.AdminDtos.AdminCategory;
import com.nailsalon.backend.catalog.api.admin.AdminDtos.CategoryWrite;
import com.nailsalon.backend.catalog.api.admin.AdminDtos.Reorder;
import com.nailsalon.backend.catalog.api.admin.AdminDtos.StatusChange;
import com.nailsalon.backend.catalog.application.CategoryAdminService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/categories")
public class AdminCategoryController {

	private final CategoryAdminService categoryService;

	public AdminCategoryController(CategoryAdminService categoryService) {
		this.categoryService = categoryService;
	}

	@GetMapping
	public List<AdminCategory> list() {
		return categoryService.list();
	}

	@PostMapping
	public ResponseEntity<AdminCategory> create(@Valid @RequestBody CategoryWrite request) {
		AdminCategory created = categoryService.create(request);
		return ResponseEntity.created(URI.create("/api/v1/admin/categories/" + created.id())).body(created);
	}

	@PutMapping("/order")
	public ResponseEntity<Void> reorder(@Valid @RequestBody Reorder request) {
		categoryService.reorder(request.orderedIds());
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{id}")
	public AdminCategory update(@PathVariable UUID id, @Valid @RequestBody CategoryWrite request) {
		return categoryService.update(id, request);
	}

	@PatchMapping("/{id}/status")
	public AdminCategory changeStatus(@PathVariable UUID id, @Valid @RequestBody StatusChange request) {
		return categoryService.changeStatus(id, request.status());
	}
}
