package com.nailsalon.backend.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nailsalon.backend.auth.owner.OwnerUser;
import com.nailsalon.backend.auth.owner.OwnerUserRepository;
import com.nailsalon.backend.business.BusinessProfile;
import com.nailsalon.backend.business.BusinessProfileRepository;
import com.nailsalon.backend.catalog.domain.PriceType;
import com.nailsalon.backend.catalog.domain.SalonService;
import com.nailsalon.backend.catalog.domain.ServiceAddOn;
import com.nailsalon.backend.catalog.domain.ServiceCategory;
import com.nailsalon.backend.catalog.domain.ServiceStatus;
import com.nailsalon.backend.catalog.infrastructure.SalonServiceRepository;
import com.nailsalon.backend.catalog.infrastructure.ServiceAddOnRepository;
import com.nailsalon.backend.catalog.infrastructure.ServiceCategoryRepository;

/**
 * Dev-only demo data (business profile, owner login, a small catalog). Runs only under
 * the {@code dev} profile — seed data is deliberately NOT a Flyway migration so prod
 * schemas stay data-free.
 */
@Component
@Profile("dev")
public class DevDataSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

	static final String DEV_OWNER_EMAIL = "owner@nailsalon.local";
	static final String DEV_OWNER_PASSWORD = "owner-dev-password";

	private final BusinessProfileRepository businesses;
	private final OwnerUserRepository owners;
	private final ServiceCategoryRepository categories;
	private final SalonServiceRepository services;
	private final ServiceAddOnRepository addOns;
	private final com.nailsalon.backend.booking.infrastructure.WeeklyAvailabilityRepository weeklyAvailability;
	private final PasswordEncoder passwordEncoder;

	public DevDataSeeder(BusinessProfileRepository businesses, OwnerUserRepository owners,
			ServiceCategoryRepository categories, SalonServiceRepository services,
			ServiceAddOnRepository addOns,
			com.nailsalon.backend.booking.infrastructure.WeeklyAvailabilityRepository weeklyAvailability,
			PasswordEncoder passwordEncoder) {
		this.businesses = businesses;
		this.owners = owners;
		this.categories = categories;
		this.services = services;
		this.addOns = addOns;
		this.weeklyAvailability = weeklyAvailability;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		BusinessProfile business = businesses.findFirstByOrderByCreatedAtAsc().orElseGet(this::seedBusiness);
		if (owners.count() == 0) {
			seedOwner();
		}
		if (categories.count() == 0) {
			seedCatalog(business);
		}
		if (weeklyAvailability.count() == 0) {
			seedWeeklyHours();
		}
	}

	/** Default opening hours (Tue-Sat 09:00-17:00) so dev booking works immediately. */
	private void seedWeeklyHours() {
		for (java.time.DayOfWeek day : java.util.List.of(java.time.DayOfWeek.TUESDAY,
				java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.FRIDAY,
				java.time.DayOfWeek.SATURDAY)) {
			com.nailsalon.backend.booking.domain.WeeklyAvailability row =
					new com.nailsalon.backend.booking.domain.WeeklyAvailability();
			row.setDayOfWeek(day);
			row.setStartTime(java.time.LocalTime.of(9, 0));
			row.setEndTime(java.time.LocalTime.of(17, 0));
			weeklyAvailability.save(row);
		}
	}

	private BusinessProfile seedBusiness() {
		BusinessProfile business = new BusinessProfile();
		business.setName("Private Nail Studio");
		business.setSlug("private-nail-studio");
		business.setPhone("+1 (555) 010-0100");
		business.setEmail("hello@nailsalon.local");
		business.setTimezone("America/New_York");
		business.setCurrency("USD");
		business.setAddress("123 Main St, Springfield");
		business.setAppointmentStartWindowMinutes(10);
		business.setAppointmentStartNotice(
				"We're committed to giving every guest our full attention, so your appointment may "
						+ "begin up to 10 minutes after its scheduled time.");
		return businesses.save(business);
	}

	private void seedOwner() {
		OwnerUser owner = new OwnerUser();
		owner.setEmail(DEV_OWNER_EMAIL);
		owner.setPasswordHash(passwordEncoder.encode(DEV_OWNER_PASSWORD));
		owners.save(owner);
		log.warn("Seeded dev owner login: {} / {}", DEV_OWNER_EMAIL, DEV_OWNER_PASSWORD);
	}

	private void seedCatalog(BusinessProfile business) {
		ServiceCategory manicure = category(business, "Manicure", "Hand and nail care", 0);
		ServiceCategory pedicure = category(business, "Pedicure", "Foot and nail care", 1);

		SalonService gel = service(manicure, "Gel Manicure",
				"Long-lasting gel polish with cuticle care and shaping.", 60, PriceType.FIXED, 5500, 0);
		service(manicure, "Classic Manicure",
				"Shape, buff, cuticle care and regular polish.", 45, PriceType.FIXED, 3500, 1);
		service(pedicure, "Spa Pedicure",
				"Relaxing soak, exfoliation, massage and polish.", 75, PriceType.STARTING_AT, 6500, 0);

		addOn(gel, "Nail Art (per nail)", "Hand-painted design", 10, 500, 0);
		addOn(gel, "Paraffin Treatment", "Deep moisturizing wax treatment", 15, 1500, 1);
	}

	private ServiceCategory category(BusinessProfile business, String name, String description, int order) {
		ServiceCategory category = new ServiceCategory();
		category.setBusinessId(business.getId());
		category.setName(name);
		category.setSlug(com.nailsalon.backend.shared.Slugs.slugify(name));
		category.setDescription(description);
		category.setDisplayOrder(order);
		category.setStatus(ServiceStatus.ACTIVE);
		return categories.save(category);
	}

	private SalonService service(ServiceCategory category, String name, String description,
			int durationMinutes, PriceType priceType, int priceCents, int order) {
		SalonService service = new SalonService();
		service.setCategoryId(category.getId());
		service.setName(name);
		service.setSlug(com.nailsalon.backend.shared.Slugs.slugify(name));
		service.setDescription(description);
		service.setDurationMinutes(durationMinutes);
		service.setPriceType(priceType);
		service.setPriceCents(priceCents);
		service.setDisplayOrder(order);
		service.setStatus(ServiceStatus.ACTIVE);
		return services.save(service);
	}

	private void addOn(SalonService service, String name, String description,
			int addedMinutes, int priceCents, int order) {
		ServiceAddOn addOn = new ServiceAddOn();
		addOn.setServiceId(service.getId());
		addOn.setName(name);
		addOn.setDescription(description);
		addOn.setAddedDurationMinutes(addedMinutes);
		addOn.setPriceCents(priceCents);
		addOn.setDisplayOrder(order);
		addOns.save(addOn);
	}
}
