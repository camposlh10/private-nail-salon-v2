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

import com.nailsalon.backend.booking.api.admin.ScheduleDtos.OverrideWrite;
import com.nailsalon.backend.booking.api.admin.ScheduleDtos.WeeklyInterval;
import com.nailsalon.backend.booking.api.admin.ScheduleDtos.WeeklySchedule;
import com.nailsalon.backend.booking.api.publicapi.PublicBookingDtos.AvailabilityResponse;
import com.nailsalon.backend.booking.application.AvailabilityService;
import com.nailsalon.backend.booking.application.ScheduleAdminService;
import com.nailsalon.backend.booking.domain.Appointment;
import com.nailsalon.backend.booking.domain.AppointmentStatus;
import com.nailsalon.backend.booking.domain.BlockedTime;
import com.nailsalon.backend.booking.domain.Client;
import com.nailsalon.backend.booking.domain.SlotHold;
import com.nailsalon.backend.booking.infrastructure.AppointmentEventRepository;
import com.nailsalon.backend.booking.infrastructure.AppointmentItemRepository;
import com.nailsalon.backend.booking.infrastructure.AppointmentRepository;
import com.nailsalon.backend.booking.infrastructure.AvailabilityOverrideRepository;
import com.nailsalon.backend.booking.infrastructure.BlockedTimeRepository;
import com.nailsalon.backend.booking.infrastructure.ClientRepository;
import com.nailsalon.backend.booking.infrastructure.PhoneVerifiedSessionRepository;
import com.nailsalon.backend.booking.infrastructure.SlotHoldRepository;
import com.nailsalon.backend.booking.infrastructure.WeeklyAvailabilityRepository;
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
import com.nailsalon.backend.shared.error.ApiException;

/**
 * Availability engine rules. Time is pinned to 2026-03-02T12:00:00Z (a Monday, 07:00
 * EST) so DST transitions (2026-03-08 spring forward, 2026-11-01 fall back in
 * America/New_York) are deterministic. The horizon is stretched to a year so the
 * November fall-back date stays bookable.
 */
@SpringBootTest(properties = "app.booking.horizon-days=365")
@ActiveProfiles("test")
@Import(AvailabilityEngineTests.FixedClockConfig.class)
class AvailabilityEngineTests {

	static final Instant NOW = Instant.parse("2026-03-02T12:00:00Z");
	static final ZoneId NY = ZoneId.of("America/New_York");

	@TestConfiguration
	static class FixedClockConfig {

		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(NOW, ZoneOffset.UTC);
		}
	}

	@Autowired
	private AvailabilityService availability;
	@Autowired
	private ScheduleAdminService scheduleAdmin;

	@Autowired
	private AppointmentEventRepository eventRepository;
	@Autowired
	private AppointmentItemRepository itemRepository;
	@Autowired
	private AppointmentRepository appointmentRepository;
	@Autowired
	private PhoneVerifiedSessionRepository sessionRepository;
	@Autowired
	private SlotHoldRepository holdRepository;
	@Autowired
	private ClientRepository clientRepository;
	@Autowired
	private WeeklyAvailabilityRepository weeklyRepository;
	@Autowired
	private AvailabilityOverrideRepository overrideRepository;
	@Autowired
	private BlockedTimeRepository blockRepository;
	@Autowired
	private ServiceAddOnRepository addOnRepository;
	@Autowired
	private SalonServiceRepository serviceRepository;
	@Autowired
	private ServiceCategoryRepository categoryRepository;
	@Autowired
	private BusinessProfileRepository businessRepository;

	private SalonService gel60;
	private SalonService spa75;
	private ServiceAddOn addOn30;
	private SalonService archived;

	@BeforeEach
	void seed() {
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

		gel60 = service(category, "Gel Manicure", "gel-manicure", 60, ServiceStatus.ACTIVE, true);
		spa75 = service(category, "Spa Special", "spa-special", 75, ServiceStatus.ACTIVE, true);
		archived = service(category, "Old Service", "old-service", 60, ServiceStatus.ARCHIVED, true);

		addOn30 = new ServiceAddOn();
		addOn30.setServiceId(gel60.getId());
		addOn30.setName("Paraffin");
		addOn30.setAddedDurationMinutes(30);
		addOn30.setPriceCents(1500);
		addOn30.setStatus(AddOnStatus.ACTIVE);
		addOnRepository.save(addOn30);

		// Mon-Sat 09:00-17:00; Sunday closed.
		scheduleAdmin.replaceWeekly(new WeeklySchedule(List.of(
				interval(DayOfWeek.MONDAY), interval(DayOfWeek.TUESDAY), interval(DayOfWeek.WEDNESDAY),
				interval(DayOfWeek.THURSDAY), interval(DayOfWeek.FRIDAY), interval(DayOfWeek.SATURDAY))));
	}

	// --- required scenarios -------------------------------------------------------

	@Test
	void backToBackAppointmentAllowsTheNextSlot() {
		LocalDate date = LocalDate.of(2026, 3, 10); // Tuesday, EDT
		appointment(ny(date, "09:00"), ny(date, "10:00"), AppointmentStatus.CONFIRMED);

		List<Instant> slots = slots(gel60, date);
		assertThat(slots).contains(ny(date, "10:00"));
		assertThat(slots).doesNotContain(ny(date, "09:00"), ny(date, "09:15"), ny(date, "09:30"),
				ny(date, "09:45"));
		// [start, end): a 60-min slot ending exactly at 09:00 would also be fine —
		// but the window opens at 09:00, so just assert 08:xx never appears.
		assertThat(slots).allMatch(s -> !s.isBefore(ny(date, "09:00")));
	}

	@Test
	void cancelledAppointmentsFreeTheSlot() {
		LocalDate date = LocalDate.of(2026, 3, 10);
		appointment(ny(date, "09:00"), ny(date, "10:00"), AppointmentStatus.CANCELLED_BY_CLIENT);

		assertThat(slots(gel60, date)).contains(ny(date, "09:00"));
	}

	@Test
	void serviceLongerThanTheOpenWindowIsRejected() {
		LocalDate date = LocalDate.of(2026, 3, 11);
		scheduleAdmin.createOverride(new OverrideWrite(date, false,
				LocalTime.of(10, 0), LocalTime.of(11, 0), "short day"));

		assertThat(slots(spa75, date)).isEmpty(); // 75 min never fits in 60 open minutes
		assertThat(slots(gel60, date)).containsExactly(ny(date, "10:00")); // 60 min just fits
	}

	@Test
	void addOnsIncreaseTheRequiredDuration() {
		LocalDate date = LocalDate.of(2026, 3, 12);
		scheduleAdmin.createOverride(new OverrideWrite(date, false,
				LocalTime.of(9, 0), LocalTime.of(10, 30), null));

		assertThat(slots(gel60, date))
				.containsExactly(ny(date, "09:00"), ny(date, "09:15"), ny(date, "09:30"));
		assertThat(slotsWithAddOn(gel60, addOn30, date))
				.containsExactly(ny(date, "09:00")); // 90 min only fits at 09:00
	}

	@Test
	void closedDaysReturnNoSlots() {
		assertThat(slots(gel60, LocalDate.of(2026, 3, 15))).isEmpty(); // Sunday, no weekly hours
	}

	@Test
	void closedOverrideBeatsWeeklyHours() {
		LocalDate date = LocalDate.of(2026, 3, 13); // Friday
		scheduleAdmin.createOverride(new OverrideWrite(date, true, null, null, "holiday"));

		assertThat(slots(gel60, date)).isEmpty();
	}

	@Test
	void specialHoursReplaceNormalHours() {
		LocalDate date = LocalDate.of(2026, 3, 13); // Friday, normally 09:00-17:00
		scheduleAdmin.createOverride(new OverrideWrite(date, false,
				LocalTime.of(12, 0), LocalTime.of(14, 0), null));

		List<Instant> slots = slots(gel60, date);
		assertThat(slots).doesNotContain(ny(date, "09:00"), ny(date, "11:45"), ny(date, "14:00"));
		assertThat(slots).containsExactly(ny(date, "12:00"), ny(date, "12:15"), ny(date, "12:30"),
				ny(date, "12:45"), ny(date, "13:00"));
	}

	@Test
	void blockedTimeRemovesCandidates() {
		LocalDate date = LocalDate.of(2026, 3, 17);
		BlockedTime block = new BlockedTime();
		block.setStartAt(ny(date, "09:00"));
		block.setEndAt(ny(date, "12:00"));
		blockRepository.save(block);

		List<Instant> slots = slots(gel60, date);
		assertThat(slots).doesNotContain(ny(date, "09:00"), ny(date, "11:00"), ny(date, "11:15"));
		assertThat(slots).contains(ny(date, "12:00")); // [start, end): block end frees 12:00
	}

	@Test
	void activeHoldBlocksButExpiredHoldDoesNot() {
		LocalDate date = LocalDate.of(2026, 3, 18);

		SlotHold active = hold(gel60, ny(date, "14:00"), ny(date, "15:00"), NOW.plusSeconds(600));
		assertThat(slots(gel60, date)).doesNotContain(ny(date, "14:00"), ny(date, "14:45"));

		active.setExpiresAt(NOW.minusSeconds(1));
		holdRepository.save(active);
		assertThat(slots(gel60, date)).contains(ny(date, "14:00"), ny(date, "14:45"));
	}

	@Test
	void consumedAndReleasedHoldsDoNotBlock() {
		LocalDate date = LocalDate.of(2026, 3, 18);
		SlotHold hold = hold(gel60, ny(date, "14:00"), ny(date, "15:00"), NOW.plusSeconds(600));
		hold.setStatus(SlotHold.Status.RELEASED);
		holdRepository.save(hold);

		assertThat(slots(gel60, date)).contains(ny(date, "14:00"));
	}

	@Test
	void inactiveServicesCannotProduceAvailability() {
		LocalDate date = LocalDate.of(2026, 3, 10);
		assertThatThrownBy(() -> availability.availability(archived.getId(), List.of(), date, date))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getStatus().value()).isEqualTo(404));
	}

	@Test
	void minimumNoticeHidesImminentSlots() {
		LocalDate today = LocalDate.of(2026, 3, 2); // now = 07:00 EST, notice = 120 min
		List<Instant> slots = slots(gel60, today);
		assertThat(slots).isNotEmpty();
		assertThat(slots).allMatch(s -> !s.isBefore(NOW.plusSeconds(120 * 60)));
	}

	@Test
	void datesBeyondTheHorizonReturnNothing() {
		LocalDate farFuture = LocalDate.of(2027, 4, 1); // > 365-day horizon
		assertThat(slots(gel60, farFuture)).isEmpty();
	}

	// --- daylight saving ------------------------------------------------------------

	@Test
	void springForwardGapProducesNoPhantomSlots() {
		LocalDate date = LocalDate.of(2026, 3, 8); // 02:00 -> 03:00 EDT jump
		scheduleAdmin.createOverride(new OverrideWrite(date, false,
				LocalTime.of(1, 0), LocalTime.of(5, 0), "dst test"));

		AvailabilityResponse response = availability.availability(gel60.getId(), List.of(), date, date);
		List<OffsetDateTime> slots = response.days().get(0).slots();

		assertThat(slots).isNotEmpty();
		// 02:00-02:59 does not exist that night.
		assertThat(slots).noneMatch(s -> s.getHour() == 2);
		// Before the jump: EST (-05:00); after: EDT (-04:00).
		assertThat(slots).filteredOn(s -> s.getHour() == 1)
				.allMatch(s -> s.getOffset().equals(ZoneOffset.ofHours(-5)));
		assertThat(slots).filteredOn(s -> s.getHour() >= 3)
				.allMatch(s -> s.getOffset().equals(ZoneOffset.ofHours(-4)));
	}

	@Test
	void fallBackNightKeepsCorrectOffsets() {
		LocalDate date = LocalDate.of(2026, 11, 1); // 02:00 EDT -> 01:00 EST
		scheduleAdmin.createOverride(new OverrideWrite(date, false,
				LocalTime.of(0, 30), LocalTime.of(3, 0), "dst test"));

		AvailabilityResponse response = availability.availability(gel60.getId(), List.of(), date, date);
		List<OffsetDateTime> slots = response.days().get(0).slots();

		assertThat(slots).isNotEmpty();
		// No duplicate instants even though 01:xx wall times happen twice.
		assertThat(slots.stream().map(OffsetDateTime::toInstant).distinct().count())
				.isEqualTo(slots.size());
		// 02:00 is unambiguously EST.
		assertThat(slots).filteredOn(s -> s.getHour() == 2)
				.allMatch(s -> s.getOffset().equals(ZoneOffset.ofHours(-5)));
	}

	@Test
	void slotsCarryTheBusinessTimezoneOffset() {
		LocalDate winter = LocalDate.of(2026, 3, 3); // EST
		LocalDate summer = LocalDate.of(2026, 6, 3); // EDT

		assertThat(availability.availability(gel60.getId(), List.of(), winter, winter)
				.days().get(0).slots())
				.isNotEmpty()
				.allMatch(s -> s.getOffset().equals(ZoneOffset.ofHours(-5)));
		assertThat(availability.availability(gel60.getId(), List.of(), summer, summer)
				.days().get(0).slots())
				.isNotEmpty()
				.allMatch(s -> s.getOffset().equals(ZoneOffset.ofHours(-4)));
	}

	// --- fixtures -------------------------------------------------------------------

	private static WeeklyInterval interval(DayOfWeek day) {
		return new WeeklyInterval(day, LocalTime.of(9, 0), LocalTime.of(17, 0));
	}

	private SalonService service(ServiceCategory category, String name, String slug, int minutes,
			ServiceStatus status, boolean bookable) {
		SalonService service = new SalonService();
		service.setCategoryId(category.getId());
		service.setName(name);
		service.setSlug(slug);
		service.setDurationMinutes(minutes);
		service.setPriceType(PriceType.FIXED);
		service.setPriceCents(5500);
		service.setOnlineBookable(bookable);
		service.setStatus(status);
		return serviceRepository.save(service);
	}

	private void appointment(Instant start, Instant end, AppointmentStatus status) {
		Client client = new Client();
		client.setName("Test Client");
		client.setPhoneE164("+1555010" + (System.nanoTime() % 10000));
		clientRepository.save(client);

		Appointment appointment = new Appointment();
		appointment.setClientId(client.getId());
		appointment.setStatus(status);
		appointment.setStartAt(start);
		appointment.setEndAt(end);
		appointment.setTimezone("America/New_York");
		appointmentRepository.save(appointment);
	}

	private SlotHold hold(SalonService service, Instant start, Instant end, Instant expiresAt) {
		SlotHold hold = new SlotHold();
		hold.setServiceId(service.getId());
		hold.setStartAt(start);
		hold.setEndAt(end);
		hold.setExpiresAt(expiresAt);
		return holdRepository.save(hold);
	}

	private List<Instant> slots(SalonService service, LocalDate date) {
		return availability.availability(service.getId(), List.of(), date, date).days().stream()
				.filter(d -> d.date().equals(date))
				.flatMap(d -> d.slots().stream().map(OffsetDateTime::toInstant))
				.toList();
	}

	private List<Instant> slotsWithAddOn(SalonService service, ServiceAddOn addOn, LocalDate date) {
		return availability.availability(service.getId(), List.of(addOn.getId()), date, date)
				.days().stream()
				.filter(d -> d.date().equals(date))
				.flatMap(d -> d.slots().stream().map(OffsetDateTime::toInstant))
				.toList();
	}

	private static Instant ny(LocalDate date, String time) {
		return ZonedDateTime.of(date, LocalTime.parse(time), NY).toInstant();
	}
}
