package com.nailsalon.backend.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.nailsalon.backend.auth.owner.OwnerUserRepository;
import com.nailsalon.backend.auth.owner.PasswordResetTokenRepository;
import com.nailsalon.backend.business.BusinessProfile;
import com.nailsalon.backend.business.BusinessProfileRepository;
import com.nailsalon.backend.catalog.domain.AddOnStatus;
import com.nailsalon.backend.catalog.domain.PriceType;
import com.nailsalon.backend.catalog.domain.SalonService;
import com.nailsalon.backend.catalog.domain.ServiceAddOn;
import com.nailsalon.backend.catalog.domain.ServiceCategory;
import com.nailsalon.backend.catalog.domain.ServiceStatus;
import com.nailsalon.backend.catalog.infrastructure.SalonServiceRepository;
import com.nailsalon.backend.catalog.infrastructure.ServiceAddOnRepository;
import com.nailsalon.backend.catalog.infrastructure.ServiceCategoryRepository;
import com.nailsalon.backend.shared.audit.AuditEventRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicCatalogApiTests {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ServiceAddOnRepository addOnRepository;
	@Autowired
	private SalonServiceRepository serviceRepository;
	@Autowired
	private ServiceCategoryRepository categoryRepository;
	@Autowired
	private PasswordResetTokenRepository resetTokenRepository;
	@Autowired
	private OwnerUserRepository ownerRepository;
	@Autowired
	private AuditEventRepository auditRepository;
	@Autowired
	private BusinessProfileRepository businessRepository;

	@BeforeEach
	void seedCatalog() {
		addOnRepository.deleteAll();
		serviceRepository.deleteAll();
		categoryRepository.deleteAll();
		resetTokenRepository.deleteAll();
		ownerRepository.deleteAll();
		auditRepository.deleteAll();
		businessRepository.deleteAll();

		BusinessProfile business = new BusinessProfile();
		business.setName("Test Salon");
		business.setSlug("test-salon");
		business.setTimezone("America/New_York");
		business.setAppointmentStartWindowMinutes(10);
		business.setAppointmentStartNotice("We may start up to 10 minutes late.");
		businessRepository.save(business);

		ServiceCategory manicure = category(business, "Manicure", "manicure", ServiceStatus.ACTIVE, 0);
		ServiceCategory archivedCat = category(business, "Old Stuff", "old-stuff", ServiceStatus.ARCHIVED, 1);

		// Visible: active + bookable in active category
		SalonService gel = service(manicure, "Gel Manicure", "gel-manicure", ServiceStatus.ACTIVE, true);
		addOn(gel, "Nail Art", AddOnStatus.ACTIVE);
		addOn(gel, "Retired Extra", AddOnStatus.ARCHIVED);

		// Hidden for various reasons:
		service(manicure, "Archived Service", "archived-service", ServiceStatus.ARCHIVED, true);
		service(manicure, "Draft Service", "draft-service", ServiceStatus.DRAFT, true);
		service(manicure, "Walk-in Only", "walk-in-only", ServiceStatus.ACTIVE, false);
		service(archivedCat, "Ghost Service", "ghost-service", ServiceStatus.ACTIVE, true);
	}

	@Test
	void businessProfileExposesTransitionNotice() throws Exception {
		mockMvc.perform(get("/api/v1/public/business"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", is("Test Salon")))
				.andExpect(jsonPath("$.appointmentStartWindowMinutes", is(10)))
				.andExpect(jsonPath("$.appointmentStartNotice", is("We may start up to 10 minutes late.")));
	}

	@Test
	void onlyActiveBookableServicesInActiveCategoriesAreVisible() throws Exception {
		mockMvc.perform(get("/api/v1/public/services"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].slug", is("gel-manicure")))
				.andExpect(jsonPath("$[0].price.amountCents", is(5500)))
				.andExpect(jsonPath("$[0].price.currency", is("USD")));
	}

	@Test
	void archivedCategoriesAreExcludedFromPublicList() throws Exception {
		mockMvc.perform(get("/api/v1/public/categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].slug", is("manicure")));
	}

	@Test
	void categoryAndSearchFiltersWork() throws Exception {
		mockMvc.perform(get("/api/v1/public/services").param("category", "manicure").param("q", "gel"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));
		mockMvc.perform(get("/api/v1/public/services").param("q", "no-such-thing"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void serviceDetailShowsOnlyActiveAddOns() throws Exception {
		mockMvc.perform(get("/api/v1/public/services/gel-manicure"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.addOns", hasSize(1)))
				.andExpect(jsonPath("$.addOns[0].name", is("Nail Art")));
	}

	@Test
	void hiddenServiceDetailIs404EvenWithValidSlug() throws Exception {
		mockMvc.perform(get("/api/v1/public/services/draft-service"))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/public/services/ghost-service"))
				.andExpect(status().isNotFound());
	}

	@Test
	void publicCatalogResponsesCarryEtags() throws Exception {
		mockMvc.perform(get("/api/v1/public/services"))
				.andExpect(status().isOk())
				.andExpect(header().string("ETag", notNullValue()));
	}

	// --- fixtures -------------------------------------------------------------------

	private ServiceCategory category(BusinessProfile business, String name, String slug,
			ServiceStatus status, int order) {
		ServiceCategory c = new ServiceCategory();
		c.setBusinessId(business.getId());
		c.setName(name);
		c.setSlug(slug);
		c.setDisplayOrder(order);
		c.setStatus(status);
		return categoryRepository.save(c);
	}

	private SalonService service(ServiceCategory category, String name, String slug,
			ServiceStatus status, boolean bookable) {
		SalonService s = new SalonService();
		s.setCategoryId(category.getId());
		s.setName(name);
		s.setSlug(slug);
		s.setDurationMinutes(60);
		s.setPriceType(PriceType.FIXED);
		s.setPriceCents(5500);
		s.setOnlineBookable(bookable);
		s.setStatus(status);
		return serviceRepository.save(s);
	}

	private void addOn(SalonService service, String name, AddOnStatus status) {
		ServiceAddOn a = new ServiceAddOn();
		a.setServiceId(service.getId());
		a.setName(name);
		a.setPriceCents(500);
		a.setStatus(status);
		addOnRepository.save(a);
	}
}
