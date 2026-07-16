package com.nailsalon.backend.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import com.nailsalon.backend.booking.api.admin.AdminAppointmentDtos.AdminAppointmentCreate;
import com.nailsalon.backend.booking.api.admin.AdminAppointmentDtos.AdminAppointmentDetail;
import com.nailsalon.backend.booking.api.admin.AdminAppointmentDtos.AdminAppointmentView;
import com.nailsalon.backend.booking.api.admin.ScheduleDtos.WeeklyInterval;
import com.nailsalon.backend.booking.api.admin.ScheduleDtos.WeeklySchedule;
import com.nailsalon.backend.booking.application.AppointmentAdminService;
import com.nailsalon.backend.booking.application.ScheduleAdminService;
import com.nailsalon.backend.booking.domain.AppointmentStatus;
import com.nailsalon.backend.booking.infrastructure.AppointmentEventRepository;
import com.nailsalon.backend.booking.infrastructure.AppointmentItemRepository;
import com.nailsalon.backend.booking.infrastructure.AppointmentRepository;
import com.nailsalon.backend.booking.infrastructure.ClientRepository;
import com.nailsalon.backend.booking.infrastructure.WeeklyAvailabilityRepository;
import com.nailsalon.backend.business.BusinessProfile;
import com.nailsalon.backend.business.BusinessProfileRepository;
import com.nailsalon.backend.catalog.domain.PriceType;
import com.nailsalon.backend.catalog.domain.SalonService;
import com.nailsalon.backend.catalog.domain.ServiceCategory;
import com.nailsalon.backend.catalog.domain.ServiceStatus;
import com.nailsalon.backend.catalog.infrastructure.SalonServiceRepository;
import com.nailsalon.backend.catalog.infrastructure.ServiceCategoryRepository;
import com.nailsalon.backend.shared.error.ApiException;

@SpringBootTest
@ActiveProfiles("test")
@Import(OwnerCalendarTests.FixedClockConfig.class)
class OwnerCalendarTests {

	static final Instant NOW = Instant.parse("2026-03-02T12:00:00Z");
	static final ZoneId NY = ZoneId.of("America/New_York");
	static final LocalDate DATE = LocalDate.of(2026, 3, 10); // Tuesday

	@TestConfiguration
	static class FixedClockConfig {

		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(NOW, ZoneOffset.UTC);
		}
	}

	@Autowired
	private AppointmentAdminService admin;
	@Autowired
	private ScheduleAdminService scheduleAdmin;

	@Autowired
	private AppointmentEventRepository eventRepository;
	@Autowired
	private AppointmentItemRepository itemRepository;
	@Autowired
	private AppointmentRepository appointmentRepository;
	@Autowired
	private ClientRepository clientRepository;
	@Autowired
	private WeeklyAvailabilityRepository weeklyRepository;
	@Autowired
	private SalonServiceRepository serviceRepository;
	@Autowired
	private ServiceCategoryRepository categoryRepository;
	@Autowired
	private BusinessProfileRepository businessRepository;
	@Autowired
	private com.nailsalon.backend.booking.infrastructure.PhoneVerifiedSessionRepository sessionRepository;
	@Autowired
	private com.nailsalon.backend.booking.infrastructure.SlotHoldRepository holdRepository;
	@Autowired
	private com.nailsalon.backend.booking.infrastructure.AvailabilityOverrideRepository overrideRepository;
	@Autowired
	private com.nailsalon.backend.booking.infrastructure.BlockedTimeRepository blockRepository;
	@Autowired
	private com.nailsalon.backend.catalog.infrastructure.ServiceAddOnRepository addOnRepository;
	@Autowired
	private com.nailsalon.backend.communications.infrastructure.CommunicationOutboxRepository outboxRepository;

	private SalonService gel60;

	@BeforeEach
	void seed() {
		outboxRepository.deleteAll();
		eventRepository.deleteAll();
		itemRepository.deleteAll();
		appointmentRepository.deleteAll();
		sessionRepository.deleteAll();
		holdRepository.deleteAll();
		clientRepository.deleteAll();
		weeklyRepository.deleteAll();
		overrideRepository.deleteAll();
		blockRepository.deleteAll();
		addOnRepository.deleteAll();
		serviceRepository.deleteAll();
		categoryRepository.deleteAll();
		businessRepository.deleteAll();

		BusinessProfile business = new BusinessProfile();
		business.setName("Test Salon");
		business.setSlug("test-salon");
		business.setTimezone("America/New_York");
		businessRepository.save(business);

		ServiceCategory category = new ServiceCategory();
		category.setBusinessId(business.getId());
		category.setName("Manicure");
		category.setSlug("manicure");
		category.setStatus(ServiceStatus.ACTIVE);
		categoryRepository.save(category);

		gel60 = new SalonService();
		gel60.setCategoryId(category.getId());
		gel60.setName("Gel Manicure");
		gel60.setSlug("gel-manicure");
		gel60.setDurationMinutes(60);
		gel60.setPriceType(PriceType.FIXED);
		gel60.setPriceCents(5500);
		gel60.setStatus(ServiceStatus.ACTIVE);
		serviceRepository.save(gel60);

		scheduleAdmin.replaceWeekly(new WeeklySchedule(List.of(
				new WeeklyInterval(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)))));
	}

	@Test
	void ownerCreationFollowsTheSameAvailabilityRules() {
		AdminAppointmentDetail created = admin.create(create("10:00", "+15550100400"));
		assertThat(created.status()).isEqualTo(AppointmentStatus.CONFIRMED);
		assertThat(created.serviceName()).isEqualTo("Gel Manicure");
		assertThat(created.client().phone()).isEqualTo("+15550100400");

		// Overlap with the fresh appointment is refused...
		assertThatThrownBy(() -> admin.create(create("10:30", "+15550100401")))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getStatus().value()).isEqualTo(409));
		// ...outside opening hours is refused...
		assertThatThrownBy(() -> admin.create(create("18:00", "+15550100401")))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getStatus().value()).isEqualTo(409));
		// ...back-to-back is fine.
		assertThat(admin.create(create("11:00", "+15550100401"))).isNotNull();
	}

	@Test
	void ownerCreationMatchesExistingClientsByPhone() {
		admin.create(create("10:00", "+1 (555) 010-0400"));
		admin.create(create("11:00", "+15550100400"));

		assertThat(clientRepository.count()).isEqualTo(1);
	}

	@Test
	void calendarFeedReturnsRangeWithClientAndService() {
		admin.create(create("10:00", "+15550100400"));

		List<AdminAppointmentView> inRange = admin.list(DATE, DATE);
		assertThat(inRange).hasSize(1);
		AdminAppointmentView view = inRange.get(0);
		assertThat(view.serviceName()).isEqualTo("Gel Manicure");
		assertThat(view.client().name()).isEqualTo("Dana Lee");
		assertThat(view.start().toInstant()).isEqualTo(ny("10:00"));
		assertThat(view.end().toInstant()).isEqualTo(ny("11:00"));
		assertThat(view.totalCents()).isEqualTo(5500);

		assertThat(admin.list(DATE.plusDays(1), DATE.plusDays(7))).isEmpty();
	}

	@Test
	void statusTransitionsAreValidatedAndRecorded() {
		UUID id = admin.create(create("10:00", "+15550100400")).id();

		admin.changeStatus(id, AppointmentStatus.CHECKED_IN);
		AdminAppointmentDetail inProgress = admin.changeStatus(id, AppointmentStatus.IN_PROGRESS);
		assertThat(inProgress.actualStart()).isNotNull();

		AdminAppointmentDetail completed = admin.changeStatus(id, AppointmentStatus.COMPLETED);
		assertThat(completed.actualEnd()).isNotNull();
		assertThat(completed.events()).hasSize(4); // created + 3 transitions

		// Terminal states are final.
		assertThatThrownBy(() -> admin.changeStatus(id, AppointmentStatus.CONFIRMED))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getStatus().value()).isEqualTo(409));
	}

	@Test
	void cancellingFreesTheSlotForANewBooking() {
		UUID id = admin.create(create("10:00", "+15550100400")).id();
		admin.changeStatus(id, AppointmentStatus.CANCELLED_BY_OWNER);

		assertThat(admin.create(create("10:00", "+15550100402"))).isNotNull();
	}

	// --- fixtures -------------------------------------------------------------------

	private AdminAppointmentCreate create(String localTime, String phone) {
		return new AdminAppointmentCreate(gel60.getId(), List.of(),
				OffsetDateTime.ofInstant(ny(localTime), NY), "Dana Lee", phone, null, null);
	}

	private static Instant ny(String time) {
		return ZonedDateTime.of(DATE, LocalTime.parse(time), NY).toInstant();
	}
}
