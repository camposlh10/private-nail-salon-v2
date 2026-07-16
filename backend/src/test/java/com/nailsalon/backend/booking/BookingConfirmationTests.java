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

import com.nailsalon.backend.booking.api.admin.ScheduleDtos.WeeklyInterval;
import com.nailsalon.backend.booking.api.admin.ScheduleDtos.WeeklySchedule;
import com.nailsalon.backend.booking.api.publicapi.PublicBookingDtos.AppointmentCreate;
import com.nailsalon.backend.booking.api.publicapi.PublicBookingDtos.SlotHoldCreate;
import com.nailsalon.backend.booking.api.publicapi.PublicBookingDtos.SlotHoldView;
import com.nailsalon.backend.booking.application.BookingService;
import com.nailsalon.backend.booking.application.BookingService.ConfirmResult;
import com.nailsalon.backend.booking.application.PhoneVerificationService;
import com.nailsalon.backend.booking.application.ScheduleAdminService;
import com.nailsalon.backend.booking.application.SlotHoldService;
import com.nailsalon.backend.booking.domain.AppointmentEvent;
import com.nailsalon.backend.booking.domain.AppointmentStatus;
import com.nailsalon.backend.booking.domain.SlotHold;
import com.nailsalon.backend.booking.infrastructure.AppointmentEventRepository;
import com.nailsalon.backend.booking.infrastructure.AppointmentItemRepository;
import com.nailsalon.backend.booking.infrastructure.AppointmentRepository;
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
import com.nailsalon.backend.communications.domain.CommunicationChannel;
import com.nailsalon.backend.communications.domain.CommunicationOutbox;
import com.nailsalon.backend.communications.infrastructure.CommunicationOutboxRepository;
import com.nailsalon.backend.communications.outbox.OutboxWorker;
import com.nailsalon.backend.communications.providers.LoggingPhoneVerificationGateway;
import com.nailsalon.backend.shared.error.ApiException;

@SpringBootTest
@ActiveProfiles("test")
@Import(BookingConfirmationTests.FixedClockConfig.class)
class BookingConfirmationTests {

	static final Instant NOW = Instant.parse("2026-03-02T12:00:00Z");
	static final ZoneId NY = ZoneId.of("America/New_York");
	static final LocalDate DATE = LocalDate.of(2026, 3, 10); // Tuesday
	static final String PHONE = "+15550100300";

	@TestConfiguration
	static class FixedClockConfig {

		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(NOW, ZoneOffset.UTC);
		}
	}

	@Autowired
	private BookingService booking;
	@Autowired
	private SlotHoldService slotHolds;
	@Autowired
	private PhoneVerificationService phoneVerification;
	@Autowired
	private ScheduleAdminService scheduleAdmin;
	@Autowired
	private LoggingPhoneVerificationGateway otpGateway;
	@Autowired
	private OutboxWorker outboxWorker;

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
	private ServiceAddOnRepository addOnRepository;
	@Autowired
	private SalonServiceRepository serviceRepository;
	@Autowired
	private ServiceCategoryRepository categoryRepository;
	@Autowired
	private BusinessProfileRepository businessRepository;
	@Autowired
	private CommunicationOutboxRepository outboxRepository;

	private SalonService gel60;
	private ServiceAddOn addOn30;

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

		addOn30 = new ServiceAddOn();
		addOn30.setServiceId(gel60.getId());
		addOn30.setName("Paraffin");
		addOn30.setAddedDurationMinutes(30);
		addOn30.setPriceCents(1500);
		addOn30.setStatus(AddOnStatus.ACTIVE);
		addOnRepository.save(addOn30);

		scheduleAdmin.replaceWeekly(new WeeklySchedule(List.of(
				new WeeklyInterval(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)))));
	}

	@Test
	void confirmationPersistsEverythingAtomically() {
		SlotHoldView hold = hold("10:00", List.of(addOn30.getId()));
		String token = verifiedToken(hold.id());

		ConfirmResult result = booking.confirm(token,
				new AppointmentCreate(hold.id(), "Dana Lee", "dana@example.com", "first visit"),
				"idem-key-1");

		assertThat(result.created()).isTrue();
		var appointment = appointmentRepository.findById(result.appointment().id()).orElseThrow();
		assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
		assertThat(appointment.getStartAt()).isEqualTo(ny("10:00"));
		assertThat(appointment.getEndAt()).isEqualTo(ny("11:30")); // 60 + 30 add-on
		assertThat(appointment.getTimezone()).isEqualTo("America/New_York");

		var client = clientRepository.findByPhoneE164(PHONE).orElseThrow();
		assertThat(client.getName()).isEqualTo("Dana Lee");
		assertThat(client.getEmail()).isEqualTo("dana@example.com");

		var lines = itemRepository.findByAppointmentIdOrderBySortOrderAsc(appointment.getId());
		assertThat(lines).hasSize(2);
		assertThat(lines.get(0).getName()).isEqualTo("Gel Manicure");
		assertThat(lines.get(0).getPriceCents()).isEqualTo(5500);
		assertThat(lines.get(1).getName()).isEqualTo("Paraffin");
		assertThat(lines.get(1).getPriceCents()).isEqualTo(1500);
		assertThat(result.appointment().totalCents()).isEqualTo(7000);

		assertThat(eventRepository.findByAppointmentIdOrderByCreatedAtAsc(appointment.getId()))
				.extracting(AppointmentEvent::getEventType)
				.containsExactly(AppointmentEvent.CREATED);

		assertThat(holdRepository.findById(hold.id()).orElseThrow().getStatus())
				.isEqualTo(SlotHold.Status.CONSUMED);
		assertThat(sessionRepository.findAll().get(0).getConsumedAt()).isNotNull();

		var outboxRows = outboxRepository.findAll();
		assertThat(outboxRows).hasSize(2);
		assertThat(outboxRows).extracting(CommunicationOutbox::getChannel)
				.containsExactlyInAnyOrder(CommunicationChannel.SMS, CommunicationChannel.EMAIL);
		assertThat(outboxRows).allMatch(r -> r.getStatus() == CommunicationOutbox.Status.PENDING);
	}

	@Test
	void snapshotsAreImmuneToLaterCatalogEdits() {
		SlotHoldView hold = hold("10:00", List.of());
		ConfirmResult result = booking.confirm(verifiedToken(hold.id()),
				new AppointmentCreate(hold.id(), "Dana Lee", null, null), null);

		gel60.setName("Renamed Service");
		gel60.setPriceCents(9900);
		serviceRepository.save(gel60);

		var lines = itemRepository.findByAppointmentIdOrderBySortOrderAsc(result.appointment().id());
		assertThat(lines.get(0).getName()).isEqualTo("Gel Manicure");
		assertThat(lines.get(0).getPriceCents()).isEqualTo(5500);
	}

	@Test
	void sameIdempotencyKeyReturnsTheOriginalAppointment() {
		SlotHoldView hold = hold("10:00", List.of());
		String token = verifiedToken(hold.id());
		ConfirmResult first = booking.confirm(token,
				new AppointmentCreate(hold.id(), "Dana Lee", null, null), "retry-key");

		// Browser retry: hold consumed, session consumed — the key alone must answer.
		ConfirmResult replay = booking.confirm(token,
				new AppointmentCreate(hold.id(), "Dana Lee", null, null), "retry-key");

		assertThat(replay.created()).isFalse();
		assertThat(replay.appointment().id()).isEqualTo(first.appointment().id());
		assertThat(appointmentRepository.count()).isEqualTo(1);
		assertThat(outboxRepository.count()).isEqualTo(1); // just the original SMS
	}

	@Test
	void verifiedSessionIsSingleUse() {
		SlotHoldView firstHold = hold("10:00", List.of());
		String token = verifiedToken(null); // unbound session
		booking.confirm(token, new AppointmentCreate(firstHold.id(), "Dana Lee", null, null), null);

		SlotHoldView secondHold = hold("13:00", List.of());
		assertThatThrownBy(() -> booking.confirm(token,
				new AppointmentCreate(secondHold.id(), "Dana Lee", null, null), null))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getStatus().value()).isEqualTo(401));
	}

	@Test
	void missingOrForeignSessionIsRejected() {
		SlotHoldView hold = hold("10:00", List.of());
		assertThatThrownBy(() -> booking.confirm(null,
				new AppointmentCreate(hold.id(), "Dana Lee", null, null), null))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getStatus().value()).isEqualTo(401));

		SlotHoldView otherHold = hold("13:00", List.of());
		String boundToOther = verifiedToken(otherHold.id());
		assertThatThrownBy(() -> booking.confirm(boundToOther,
				new AppointmentCreate(hold.id(), "Dana Lee", null, null), null))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getStatus().value()).isEqualTo(401));
	}

	@Test
	void expiredHoldCannotBeConsumed() {
		SlotHoldView hold = hold("10:00", List.of());
		String token = verifiedToken(hold.id());
		var stored = holdRepository.findById(hold.id()).orElseThrow();
		stored.setExpiresAt(NOW.minusSeconds(1));
		holdRepository.save(stored);

		assertThatThrownBy(() -> booking.confirm(token,
				new AppointmentCreate(hold.id(), "Dana Lee", null, null), null))
				.isInstanceOfSatisfying(ApiException.class,
						ex -> assertThat(ex.getStatus().value()).isEqualTo(409));
	}

	@Test
	void failureLateInTheFlowPersistsNothing() {
		SlotHoldView hold = hold("10:00", List.of(addOn30.getId()));
		String token = verifiedToken(hold.id());

		// The add-on is archived between hold and confirmation — eligibility recheck fails.
		addOn30.setStatus(AddOnStatus.ARCHIVED);
		addOnRepository.save(addOn30);

		assertThatThrownBy(() -> booking.confirm(token,
				new AppointmentCreate(hold.id(), "Dana Lee", "dana@example.com", null), "atomic-key"))
				.isInstanceOf(ApiException.class);

		assertThat(appointmentRepository.count()).isZero();
		assertThat(clientRepository.count()).isZero();
		assertThat(outboxRepository.count()).isZero();
		assertThat(holdRepository.findById(hold.id()).orElseThrow().getStatus())
				.isEqualTo(SlotHold.Status.ACTIVE); // not consumed
		assertThat(sessionRepository.findAll().get(0).getConsumedAt()).isNull();
	}

	// --- outbox worker ---------------------------------------------------------------

	@Test
	void outboxWorkerSendsPendingRowsThroughTheFakes() {
		SlotHoldView hold = hold("10:00", List.of());
		booking.confirm(verifiedToken(hold.id()),
				new AppointmentCreate(hold.id(), "Dana Lee", "dana@example.com", null), null);

		int processed = outboxWorker.processDue();

		assertThat(processed).isEqualTo(2);
		assertThat(outboxRepository.findAll())
				.allMatch(r -> r.getStatus() == CommunicationOutbox.Status.SENT)
				.allMatch(r -> r.getProviderMessageId() != null);

		// A second pass finds nothing — SENT rows are never re-sent.
		assertThat(outboxWorker.processDue()).isZero();
	}

	@Test
	void outboxFailuresRetryWithBackoffThenFailPermanently() {
		CommunicationOutbox broken = new CommunicationOutbox();
		broken.setChannel(CommunicationChannel.SMS);
		broken.setRecipient(PHONE);
		broken.setMessageType("APPOINTMENT_CONFIRMED");
		broken.setPayload("this is not json {{{");
		broken.setNextAttemptAt(NOW);
		outboxRepository.save(broken);

		outboxWorker.processDue();
		var afterFirst = outboxRepository.findById(broken.getId()).orElseThrow();
		assertThat(afterFirst.getStatus()).isEqualTo(CommunicationOutbox.Status.PENDING);
		assertThat(afterFirst.getAttempts()).isEqualTo(1);
		assertThat(afterFirst.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(30));
		assertThat(afterFirst.getLastError()).isNotBlank();

		// Backed off into the future: an immediate pass must not pick it up again.
		assertThat(outboxWorker.processDue()).isZero();

		// On its final allowed attempt it flips to FAILED for good.
		afterFirst.setAttempts(4); // max-attempts is 5
		afterFirst.setNextAttemptAt(NOW);
		outboxRepository.save(afterFirst);
		outboxWorker.processDue();
		var afterLast = outboxRepository.findById(broken.getId()).orElseThrow();
		assertThat(afterLast.getStatus()).isEqualTo(CommunicationOutbox.Status.FAILED);
		assertThat(afterLast.getAttempts()).isEqualTo(5);
	}

	// --- fixtures -------------------------------------------------------------------

	private SlotHoldView hold(String localTime, List<UUID> addOnIds) {
		return slotHolds.create(new SlotHoldCreate(gel60.getId(), addOnIds,
				OffsetDateTime.ofInstant(ny(localTime), NY)));
	}

	private String verifiedToken(UUID holdId) {
		phoneVerification.start(PHONE);
		String code = otpGateway.pendingCodeFor(PHONE);
		return phoneVerification.check(PHONE, code, holdId).token();
	}

	private static Instant ny(String time) {
		return ZonedDateTime.of(DATE, LocalTime.parse(time), NY).toInstant();
	}
}
