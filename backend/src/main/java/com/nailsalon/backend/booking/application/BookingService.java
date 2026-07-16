package com.nailsalon.backend.booking.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nailsalon.backend.booking.api.publicapi.PublicBookingDtos.AppointmentCreate;
import com.nailsalon.backend.booking.api.publicapi.PublicBookingDtos.AppointmentItemView;
import com.nailsalon.backend.booking.api.publicapi.PublicBookingDtos.PublicAppointment;
import com.nailsalon.backend.booking.domain.Appointment;
import com.nailsalon.backend.booking.domain.AppointmentEvent;
import com.nailsalon.backend.booking.domain.AppointmentItem;
import com.nailsalon.backend.booking.domain.Client;
import com.nailsalon.backend.booking.domain.PhoneVerifiedSession;
import com.nailsalon.backend.booking.domain.SlotHold;
import com.nailsalon.backend.booking.infrastructure.AppointmentEventRepository;
import com.nailsalon.backend.booking.infrastructure.AppointmentItemRepository;
import com.nailsalon.backend.booking.infrastructure.AppointmentRepository;
import com.nailsalon.backend.booking.infrastructure.ClientRepository;
import com.nailsalon.backend.booking.infrastructure.PhoneVerifiedSessionRepository;
import com.nailsalon.backend.booking.infrastructure.SlotHoldRepository;
import com.nailsalon.backend.business.BusinessProfileRepository;
import com.nailsalon.backend.catalog.domain.ServiceAddOn;
import com.nailsalon.backend.communications.outbox.OutboxMessages;
import com.nailsalon.backend.shared.error.ApiException;

/**
 * Public booking confirmation: one transaction that validates the verified-phone
 * session and slot hold, recomputes duration/price server-side, rechecks availability
 * under the serialization lock, creates-or-matches the client by verified phone, and
 * writes the appointment + immutable item snapshots + APPOINTMENT_CREATED event +
 * email/SMS outbox rows while consuming the hold and the session. Any failure rolls
 * the whole thing back. An idempotency key makes browser retries return the original
 * appointment instead of double-booking.
 */
@Service
public class BookingService {

	/** {@code created} is false for an idempotent replay (controller returns 200, not 201). */
	public record ConfirmResult(PublicAppointment appointment, boolean created) {
	}

	private static final DateTimeFormatter FRIENDLY =
			DateTimeFormatter.ofPattern("EEE, MMM d 'at' h:mm a", Locale.US);

	private final BusinessProfileRepository businesses;
	private final AvailabilityService availability;
	private final PhoneVerifiedSessionRepository sessions;
	private final SlotHoldRepository holds;
	private final ClientRepository clients;
	private final AppointmentRepository appointments;
	private final AppointmentItemRepository items;
	private final AppointmentEventRepository events;
	private final OutboxMessages outbox;
	private final Clock clock;

	public BookingService(BusinessProfileRepository businesses, AvailabilityService availability,
			PhoneVerifiedSessionRepository sessions, SlotHoldRepository holds, ClientRepository clients,
			AppointmentRepository appointments, AppointmentItemRepository items,
			AppointmentEventRepository events, OutboxMessages outbox, Clock clock) {
		this.businesses = businesses;
		this.availability = availability;
		this.sessions = sessions;
		this.holds = holds;
		this.clients = clients;
		this.appointments = appointments;
		this.items = items;
		this.events = events;
		this.outbox = outbox;
		this.clock = clock;
	}

	@Transactional
	public ConfirmResult confirm(String sessionToken, AppointmentCreate request, String idempotencyKey) {
		businesses.lockSingleton().orElseThrow(() -> ApiException.notFound("Business is not configured"));
		Instant now = clock.instant();

		// Idempotent replay — checked under the lock so a concurrent duplicate
		// request serializes behind the original insert.
		if (idempotencyKey != null && !idempotencyKey.isBlank()) {
			var existing = appointments.findByIdempotencyKey(idempotencyKey);
			if (existing.isPresent()) {
				return new ConfirmResult(view(existing.get()), false);
			}
		}

		PhoneVerifiedSession session = requireSession(sessionToken, now);
		SlotHold hold = holds.findById(request.slotHoldId())
				.filter(h -> h.isUsable(now))
				.orElseThrow(() -> ApiException.conflict("Your slot hold has expired — pick a time again"));
		if (session.getSlotHoldId() != null && !session.getSlotHoldId().equals(hold.getId())) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "SESSION_HOLD_MISMATCH",
					"This verification does not belong to that hold");
		}

		// Re-validate eligibility and recompute duration/price from the catalog —
		// never from anything the client sent.
		BookingSelection selection = availability.loadSelection(hold.getServiceId(), hold.getAddOnIdList());
		Instant start = hold.getStartAt();
		Instant end = start.plus(Duration.ofMinutes(selection.durationMinutes()));
		if (!availability.isSlotAvailable(selection, start, hold.getId())) {
			throw ApiException.conflict("This time is no longer available");
		}

		Client client = clients.findByPhoneE164(session.getPhoneE164()).orElseGet(Client::new);
		client.setPhoneE164(session.getPhoneE164());
		client.setName(request.clientName().trim());
		if (request.clientEmail() != null && !request.clientEmail().isBlank()) {
			client.setEmail(request.clientEmail().trim());
		}
		clients.save(client);

		Appointment appointment = new Appointment();
		appointment.setClientId(client.getId());
		appointment.setStartAt(start);
		appointment.setEndAt(end);
		appointment.setTimezone(selection.business().getTimezone());
		appointment.setSource(Appointment.Source.PUBLIC);
		appointment.setNotes(request.notes());
		appointment.setIdempotencyKey(
				(idempotencyKey == null || idempotencyKey.isBlank()) ? null : idempotencyKey);
		appointments.save(appointment);

		items.saveAll(snapshots(appointment.getId(), selection));
		events.save(AppointmentEvent.of(appointment.getId(), AppointmentEvent.CREATED,
				AppointmentEvent.Actor.CLIENT, "Booked online by " + client.getName()));

		hold.setStatus(SlotHold.Status.CONSUMED);
		session.setConsumedAt(now);

		enqueueConfirmations(selection, client, appointment);
		return new ConfirmResult(view(appointment), true);
	}

	static List<AppointmentItem> snapshots(UUID appointmentId, BookingSelection selection) {
		String currency = selection.business().getCurrency();
		List<AppointmentItem> snapshots = new ArrayList<>();
		AppointmentItem serviceItem = new AppointmentItem();
		serviceItem.setAppointmentId(appointmentId);
		serviceItem.setItemType(AppointmentItem.ItemType.SERVICE);
		serviceItem.setServiceId(selection.service().getId());
		serviceItem.setName(selection.service().getName());
		serviceItem.setDurationMinutes(selection.service().getDurationMinutes());
		serviceItem.setPriceCents(selection.service().getPriceCents());
		serviceItem.setCurrency(currency);
		serviceItem.setSortOrder(0);
		snapshots.add(serviceItem);

		int order = 1;
		for (ServiceAddOn addOn : selection.addOns()) {
			AppointmentItem item = new AppointmentItem();
			item.setAppointmentId(appointmentId);
			item.setItemType(AppointmentItem.ItemType.ADD_ON);
			item.setServiceId(selection.service().getId());
			item.setAddOnId(addOn.getId());
			item.setName(addOn.getName());
			item.setDurationMinutes(addOn.getAddedDurationMinutes());
			item.setPriceCents(addOn.getPriceCents());
			item.setCurrency(currency);
			item.setSortOrder(order++);
			snapshots.add(item);
		}
		return snapshots;
	}

	private void enqueueConfirmations(BookingSelection selection, Client client, Appointment appointment) {
		ZoneId zone = ZoneId.of(appointment.getTimezone());
		String when = FRIENDLY.format(OffsetDateTime.ofInstant(appointment.getStartAt(), zone));
		String salon = selection.business().getName();
		String service = selection.service().getName();

		outbox.enqueueSms(client.getPhoneE164(), "APPOINTMENT_CONFIRMED",
				"%s: your %s appointment on %s is confirmed. Reply to this number if you need to reschedule."
						.formatted(salon, service, when));
		if (client.getEmail() != null && !client.getEmail().isBlank()) {
			outbox.enqueueEmail(client.getEmail(), "APPOINTMENT_CONFIRMED",
					"Appointment confirmed — " + when,
					"Hi %s,%n%nYour %s appointment at %s on %s is confirmed.%n%nSee you soon!"
							.formatted(client.getName(), service, salon, when));
		}
	}

	private PhoneVerifiedSession requireSession(String sessionToken, Instant now) {
		if (sessionToken == null || sessionToken.isBlank()) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "PHONE_NOT_VERIFIED",
					"Verify your phone before booking");
		}
		return sessions.findByTokenHash(PhoneVerificationService.sha256(sessionToken))
				.filter(s -> s.isUsable(now))
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "PHONE_NOT_VERIFIED",
						"Your verification expired — verify your phone again"));
	}

	/** Also used for idempotent replays, so it rebuilds the view from persisted state only. */
	private PublicAppointment view(Appointment appointment) {
		ZoneId zone = ZoneId.of(appointment.getTimezone());
		List<AppointmentItem> lines = items.findByAppointmentIdOrderBySortOrderAsc(appointment.getId());
		String serviceName = lines.stream()
				.filter(i -> i.getItemType() == AppointmentItem.ItemType.SERVICE)
				.map(AppointmentItem::getName)
				.findFirst().orElse("");
		String currency = lines.isEmpty() ? "USD" : lines.get(0).getCurrency();
		int totalCents = lines.stream().mapToInt(AppointmentItem::getPriceCents).sum();
		return new PublicAppointment(appointment.getId(), appointment.getStatus().name(),
				OffsetDateTime.ofInstant(appointment.getStartAt(), zone),
				OffsetDateTime.ofInstant(appointment.getEndAt(), zone),
				appointment.getTimezone(), serviceName,
				lines.stream().map(i -> new AppointmentItemView(i.getItemType().name(), i.getName(),
						i.getDurationMinutes(), i.getPriceCents())).toList(),
				totalCents, currency);
	}
}
