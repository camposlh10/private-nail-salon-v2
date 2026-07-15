package com.nailsalon.backend.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.nailsalon.backend.auth.owner.PasswordResetTokenRepository;
import com.nailsalon.backend.auth.owner.OwnerUserRepository;
import com.nailsalon.backend.business.BusinessProfile;
import com.nailsalon.backend.business.BusinessProfileRepository;
import com.nailsalon.backend.catalog.api.admin.AdminDtos.AdminCategory;
import com.nailsalon.backend.catalog.api.admin.AdminDtos.AdminService;
import com.nailsalon.backend.catalog.api.admin.AdminDtos.CategoryWrite;
import com.nailsalon.backend.catalog.api.admin.AdminDtos.ServiceWrite;
import com.nailsalon.backend.catalog.application.CategoryAdminService;
import com.nailsalon.backend.catalog.application.ServiceAdminService;
import com.nailsalon.backend.catalog.domain.PriceType;
import com.nailsalon.backend.catalog.domain.ServiceStatus;
import com.nailsalon.backend.catalog.infrastructure.SalonServiceRepository;
import com.nailsalon.backend.catalog.infrastructure.ServiceAddOnRepository;
import com.nailsalon.backend.catalog.infrastructure.ServiceCategoryRepository;
import com.nailsalon.backend.shared.audit.AuditEventRepository;
import com.nailsalon.backend.shared.error.ApiException;

@SpringBootTest
@ActiveProfiles("test")
class CatalogAdminRulesTests {

	@Autowired
	private CategoryAdminService categoryAdmin;
	@Autowired
	private ServiceAdminService serviceAdmin;
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
	void cleanAndSeedBusiness() {
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
		businessRepository.save(business);
	}

	@Test
	void categoryNamesAreUniqueCaseInsensitively() {
		categoryAdmin.create(new CategoryWrite("Manicure", null, null));
		assertThatThrownBy(() -> categoryAdmin.create(new CategoryWrite("  MANICURE ", null, null)))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getStatus().value()).isEqualTo(409));
	}

	@Test
	void freeServicesMustHaveZeroPriceAndPaidServicesPositivePrice() {
		AdminCategory category = categoryAdmin.create(new CategoryWrite("Manicure", null, null));

		assertThatThrownBy(() -> serviceAdmin.create(write(category.id(), "Freebie", PriceType.FREE, 500)))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getStatus().value()).isEqualTo(400));

		assertThatThrownBy(() -> serviceAdmin.create(write(category.id(), "Gel", PriceType.FIXED, 0)))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getStatus().value()).isEqualTo(400));
	}

	@Test
	void duplicateServiceNamesGetSuffixedUniqueSlugs() {
		AdminCategory category = categoryAdmin.create(new CategoryWrite("Manicure", null, null));
		AdminService first = serviceAdmin.create(write(category.id(), "Gel Manicure", PriceType.FIXED, 5500));
		AdminService second = serviceAdmin.create(write(category.id(), "Gel Manicure", PriceType.FIXED, 6000));

		assertThat(first.slug()).isEqualTo("gel-manicure");
		assertThat(second.slug()).isEqualTo("gel-manicure-2");
	}

	@Test
	void archivingRetainsDataInsteadOfDeleting() {
		AdminCategory category = categoryAdmin.create(new CategoryWrite("Manicure", null, null));
		AdminService service = serviceAdmin.create(write(category.id(), "Gel", PriceType.FIXED, 5500));

		AdminService archived = serviceAdmin.changeStatus(service.id(), "ARCHIVED");

		assertThat(archived.status()).isEqualTo(ServiceStatus.ARCHIVED);
		assertThat(serviceRepository.findById(service.id())).isPresent();
	}

	@Test
	void editsWithStaleVersionAreRejected() {
		AdminCategory category = categoryAdmin.create(new CategoryWrite("Manicure", null, null));
		AdminService service = serviceAdmin.create(write(category.id(), "Gel", PriceType.FIXED, 5500));

		ServiceWrite stale = new ServiceWrite(category.id(), "Gel (renamed)", null, 60,
				PriceType.FIXED, 5500, null, null, null, service.version() + 5);
		assertThatThrownBy(() -> serviceAdmin.update(service.id(), stale))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getStatus().value()).isEqualTo(409));
	}

	private static ServiceWrite write(UUID categoryId, String name, PriceType priceType, int priceCents) {
		return new ServiceWrite(categoryId, name, null, 60, priceType, priceCents, null, null, null, null);
	}
}
