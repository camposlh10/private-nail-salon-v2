package com.nailsalon.backend.catalog.api.publicapi;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nailsalon.backend.catalog.api.publicapi.PublicDtos.PublicBusiness;
import com.nailsalon.backend.catalog.api.publicapi.PublicDtos.PublicCategory;
import com.nailsalon.backend.catalog.api.publicapi.PublicDtos.PublicServiceDetail;
import com.nailsalon.backend.catalog.api.publicapi.PublicDtos.PublicServiceSummary;
import com.nailsalon.backend.catalog.application.PublicCatalogService;

/** Anonymous read-only catalog for the booking site. */
@RestController
@RequestMapping("/api/v1/public")
public class PublicCatalogController {

	private final PublicCatalogService catalog;

	public PublicCatalogController(PublicCatalogService catalog) {
		this.catalog = catalog;
	}

	@GetMapping("/business")
	public PublicBusiness business() {
		return catalog.business();
	}

	@GetMapping("/categories")
	public List<PublicCategory> categories() {
		return catalog.categories();
	}

	@GetMapping("/services")
	public List<PublicServiceSummary> services(
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String q) {
		return catalog.services(category, q);
	}

	@GetMapping("/services/{slug}")
	public PublicServiceDetail service(@PathVariable String slug) {
		return catalog.service(slug);
	}
}
