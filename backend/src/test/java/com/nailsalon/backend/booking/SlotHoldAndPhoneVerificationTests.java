package com.nailsalon.backend.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.nailsalon.backend.booking.api.admin.ScheduleDtos.WeeklyInterval;
import com.nailsalon.backend.booking.api.admin.ScheduleDtos.WeeklySchedule;
import com.nailsalon.backend.booking.api.publicapi.PublicBookingDtos.SlotHoldCreate;
import com.nailsalon.backend.booking.api.publicapi.PublicBookingDtos.SlotHoldView;
import com.nailsalon.backend.booking.application.PhoneVerificationService;
import com.nailsalon.backend.booking.application.PhoneVerificationService.MintedSession;
import com.nailsalon.backend.booking.application.ScheduleAdminService;
import com.nailsalon.backend.booking.application.SlotHoldService;
import com.nailsalon.backend.booking.domain.PhoneVerifiedSession;
import com.nailsalon.backend.booking.domain.SlotHold;
import com.nailsalon.backend.booking.infrastructure.PhoneVerifiedSessionRepository;
import com.nailsalon.backend.booking.infrastructure.SlotHoldRepository;
import com.nailsalon.backend.booking.infrastructure.WeeklyAvailabilityRepository;
import com.nailsalon.backend.business.BusinessProfile;
import com.nailsalon.backend.business.BusinessProfileRepository;
import com.nailsalon.backend.catalog.domain.PriceType;
import com.nailsalon.backend.catalog.domain.SalonService;
import com.nailsalon.backend.catalog.domain.ServiceCategory;
import com.nailsalon.backend.catalog.domain.ServiceStatus;
import com.nailsalon.backend.catalog.infrastructure.SalonServiceRepository;
import com.nailsalon.backend.catalog.infrastructure.ServiceCategoryRepository;
import com.nailsalon.backend.communications.providers.LoggingPhoneVerificationGateway;
import com.nailsalon.backend.shared.error.ApiException;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SlotHoldAndPhoneVerificationTests.FixedClockConfig.class)
class SlotHoldAndPhoneVerificationTests {

	static final Instant NOW = Instant.parse("2026-03-02T12:00:00Z");
	static final ZoneId NY = ZoneId.of("America/New_York");
	static final LocalDate DATE = LocalDate.of(2026, 3, 10);

	@TestConfiguration
	static class FixedClockConfig {

		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(NOW, ZoneOffset.UTC);
		}
	}

	@Autowired
	private SlotHoldService slotHolds;
	@Autowired
	private PhoneVerificationService phoneVerification;
	@Autowired
	private ScheduleAdminService scheduleAdmin;
	@Autowired
	private LoggingPhoneVerificationGateway otpGateway;
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PhoneVerifiedSessionRepository sessionRepository;
	@Autowired
	private SlotHoldRepository holdRepository;
	@Autowired
	private WeeklyAvailabilityRepository weeklyRepository;
	@Autowired
	private SalonServiceRepository serviceRepository;
	@Autowired
	private ServiceCategoryRepository categoryRepository;
	@Autowired
	private BusinessProfileRepository businessRepository;
	@Autowired
	private com.nailsalon.backend.booking.infrastructure.AppointmentEventRepository eventRepository;
	@Autowired
	private com.nailsalon.backend.booking.infrastructure.AppointmentItemRepository itemRepository;
	@Autowired
	private com.nailsalon.backend.booking.infrastructure.AppointmentRepository appointmentRepository;
	@Autowired
	private com.nailsalon.backend.booking.infrastructure.ClientRepository clientRepository;
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

	// --- holds ----------------------------------------------------------------------

	@Test
	void holdReservesTheSlotAndExpiresInTenMinutes() {
		SlotHoldView hold = slotHolds.create(holdRequest("10:00"));

		assertThat(hold.expiresAt().toInstant()).isEqualTo(NOW.plusSeconds(600));
		assertThat(hold.end().toInstant()).isEqualTo(ny("11:00"));
		assertThatThrownBy(() -> slotHolds.create(holdRequest("10:00")))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getStatus().value()).isEqualTo(409));
		// Overlapping (not just identical) starts are also refused.
		assertThatThrownBy(() -> slotHolds.create(holdRequest("10:30")))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getStatus().value()).isEqualTo(409));
		// Back-to-back after the hold is fine.
		assertThat(slotHolds.create(holdRequest("11:00"))).isNotNull();
	}

	@Test
	void offGridOrOutOfHoursStartsAreRejected() {
		assertThatThrownBy(() -> slotHolds.create(holdRequest("10:05")))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getStatus().value()).isEqualTo(409));
		assertThatThrownBy(() -> slotHolds.create(holdRequest("07:00")))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getStatus().value()).isEqualTo(409));
	}

	@Test
	void releasingAHoldFreesTheSlot() {
		SlotHoldView hold = slotHolds.create(holdRequest("10:00"));
		slotHolds.release(hold.id());

		assertThat(slotHolds.create(holdRequest("10:00"))).isNotNull();
		slotHolds.release(hold.id()); // idempotent, no error
	}

	@Test
	void twoConcurrentAttemptsForTheSameSlotYieldExactlyOneHold() throws Exception {
		int attempts = 2;
		ExecutorService executor = Executors.newFixedThreadPool(attempts);
		CountDownLatch ready = new CountDownLatch(attempts);
		CountDownLatch go = new CountDownLatch(1);
		Callable<Object> attempt = () -> {
			ready.countDown();
			go.await();
			try {
				return slotHolds.create(holdRequest("13:00"));
			}
			catch (ApiException ex) {
				return ex;
			}
		};
		List<Future<Object>> futures = List.of(executor.submit(attempt), executor.submit(attempt));
		ready.await();
		go.countDown(); // both threads hit the serialization lock at the same moment

		long successes = 0;
		long conflicts = 0;
		for (Future<Object> future : futures) {
			Object result = future.get();
			if (result instanceof SlotHoldView) {
				successes++;
			}
			else if (result instanceof ApiException ex && ex.getStatus().value() == 409) {
				conflicts++;
			}
		}
		executor.shutdown();

		assertThat(successes).isEqualTo(1);
		assertThat(conflicts).isEqualTo(1);
		assertThat(holdRepository.findAll())
				.filteredOn(h -> h.getStatus() == SlotHold.Status.ACTIVE)
				.hasSize(1);
	}

	// --- phone verification -----------------------------------------------------------

	@Test
	void wrongOtpCodeIsRejected() {
		phoneVerification.start("+1 (555) 010-0200");
		assertThatThrownBy(() -> phoneVerification.check("+15550100200", "000000", null))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getStatus().value()).isEqualTo(400));
	}

	@Test
	void successfulOtpMintsAHashedSessionBoundToPhoneAndHold() {
		SlotHoldView hold = slotHolds.create(holdRequest("10:00"));
		phoneVerification.start("+15550100200");
		String code = otpGateway.pendingCodeFor("+15550100200");

		MintedSession minted = phoneVerification.check("+1 555 010 0200", code, hold.id());

		assertThat(minted.expiresAt()).isEqualTo(NOW.plusSeconds(30 * 60));
		PhoneVerifiedSession stored = sessionRepository.findAll().get(0);
		assertThat(stored.getPhoneE164()).isEqualTo("+15550100200");
		assertThat(stored.getSlotHoldId()).isEqualTo(hold.id());
		assertThat(stored.getTokenHash()).isNotEqualTo(minted.token());
		assertThat(stored.getTokenHash()).isEqualTo(PhoneVerificationService.sha256(minted.token()));
		assertThat(stored.getConsumedAt()).isNull();
	}

	@Test
	void malformedPhoneNumbersAreRejected() {
		assertThatThrownBy(() -> phoneVerification.start("555-0200"))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getStatus().value()).isEqualTo(400));
	}

	@Test
	void checkEndpointSetsHttpOnlySessionCookie() throws Exception {
		mockMvc.perform(post("/api/v1/public/auth/phone/start")
						.contentType("application/json")
						.content("{\"phone\":\"+15550100201\"}"))
				.andExpect(status().isAccepted());
		String code = otpGateway.pendingCodeFor("+15550100201");

		var result = mockMvc.perform(post("/api/v1/public/auth/phone/check")
						.contentType("application/json")
						.content("{\"phone\":\"+15550100201\",\"code\":\"" + code + "\"}"))
				.andExpect(status().isOk())
				.andReturn();

		String sessionCookie = result.getResponse().getHeaders("Set-Cookie").stream()
				.filter(h -> h.startsWith("BOOKING_SESSION="))
				.findFirst().orElseThrow();
		assertThat(sessionCookie).contains("HttpOnly").contains("SameSite=Lax");
	}

	// --- fixtures -------------------------------------------------------------------

	private SlotHoldCreate holdRequest(String localTime) {
		return new SlotHoldCreate(gel60.getId(), List.of(),
				OffsetDateTime.ofInstant(ny(localTime), NY));
	}

	private static Instant ny(String time) {
		return ZonedDateTime.of(DATE, LocalTime.parse(time), NY).toInstant();
	}
}
