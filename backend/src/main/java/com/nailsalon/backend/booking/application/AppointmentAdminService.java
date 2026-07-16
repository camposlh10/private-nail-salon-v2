package com.nailsalon.backend.booking.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nailsalon.backend.booking.api.admin.AdminAppointmentDtos.AdminAppointmentCreate;
import com.nailsalon.backend.booking.api.admin.AdminAppointmentDtos.AdminAppointmentDetail;
import com.nailsalon.backend.booking.api.admin.AdminAppointmentDtos.AdminAppointmentItemView;
import com.nailsalon.backend.booking.api.admin.AdminAppointmentDtos.AdminAppointmentView;
import com.nailsalon.backend.booking.api.admin.AdminAppointmentDtos.AdminClientView;
import com.nailsalon.backend.booking.api.admin.AdminAppointmentDtos.AppointmentEventView;
import com.nailsalon.backend.booking.domain.Appointment;
import com.nailsalon.backend.booking.domain.AppointmentEvent;
import com.nailsalon.backend.booking.domain.AppointmentItem;
import com.nailsalon.backend.booking.domain.AppointmentStatus;
import com.nailsalon.backend.booking.domain.Client;
import com.nailsalon.backend.booking.infrastructure.AppointmentEventRepository;
import com.nailsalon.backend.booking.infrastructure.AppointmentItemRepository;
import com.nailsalon.backend.booking.infrastructure.AppointmentRepository;
import com.nailsalon.backend.booking.infrastructure.ClientRepository;
import com.nailsalon.backend.business.BusinessProfileRepository;
import com.nailsalon.backend.shared.error.ApiException;

/**
 * Owner calendar: range feed, detail with history, manual creation (subject to the
 * SAME availability and overlap rules as public bookings) and status transitions.
 */
@Service
public class AppointmentAdminService {

	private static final Map<AppointmentStatus, Set<AppointmentStatus>> ALLOWED_TRANSITIONS = Map.of(
			AppointmentStatus.CONFIRMED, Set.of(AppointmentStatus.CHECKED_IN, AppointmentStatus.IN_PROGRESS,
					AppointmentStatus.CANCELLED_BY_CLIENT, AppointmentStatus.CANCELLED_BY_OWNER,
					AppointmentStatus.NO_SHOW),
			AppointmentStatus.CHECKED_IN, Set.of(AppointmentStatus.IN_PROGRESS, AppointmentStatus.COMPLETED,
					AppointmentStatus.CANCELLED_BY_CLIENT, AppointmentStatus.CANCELLED_BY_OWNER,
					AppointmentStatus.NO_SHOW),
			AppointmentStatus.IN_PROGRESS, Set.of(AppointmentStatus.COMPLETED,
					AppointmentStatus.CANCELLED_BY_OWNER));

	private final BusinessProfileRepository businesses;
	private final AvailabilityService availability;
	private final AppointmentRepository appointments;
	private final AppointmentItemRepository items;
	private final AppointmentEventRepository events;
	private final ClientRepository clients;
	private final Clock clock;

	public AppointmentAdminService(BusinessProfileRepository businesses, AvailabilityService availability,
			AppointmentRepository appointments, AppointmentItemRepository items,
			AppointmentEventRepository events, ClientRepository clients, Clock clock) {
		this.businesses = businesses;
		this.availability = availability;
		this.appointments = appointments;
		this.items = items;
		this.events = events;
		this.clients = clients;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<AdminAppointmentView> list(LocalDate from, LocalDate to) {
		if (from == null || to == null || to.isBefore(from)) {
			throw ApiException.badRequest("Invalid date range");
		}
		ZoneId zone = businessZone();
		List<Appointment> rows = appointments.findByStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(
				to.plusDays(1).atStartOfDay(zone).toInstant(), from.atStartOfDay(zone).toInstant());
		if (rows.isEmpty()) {
			return List.of();
		}

		Map<UUID, Client> clientsById = clients
				.findAllById(rows.stream().map(Appointment::getClientId).distinct().toList()).stream()
				.collect(Collectors.toMap(Client::getId, Function.identity()));
		Map<UUID, List<AppointmentItem>> itemsByAppointment = items
				.findByAppointmentIdInOrderBySortOrderAsc(rows.stream().map(Appointment::getId).toList())
				.stream()
				.collect(Collectors.groupingBy(AppointmentItem::getAppointmentId));

		return rows.stream().map(a -> summary(a, clientsById.get(a.getClientId()),
				itemsByAppointment.getOrDefault(a.getId(), List.of()))).toList();
	}

	@Transactional(readOnly = true)
	public AdminAppointmentDetail get(UUID id) {
		Appointment appointment = appointments.findById(id)
				.orElseThrow(() -> ApiException.notFound("Appointment not found"));
		return detail(appointment);
	}

	@Transactional
	public AdminAppointmentDetail create(AdminAppointmentCreate request) {
		businesses.lockSingleton().orElseThrow(() -> ApiException.notFound("Business is not configured"));
		BookingSelection selection = availability.loadSelection(request.serviceId(), request.addOnIds());
		Instant start = request.start().toInstant();
		if (!availability.isSlotAvailable(selection, start, null)) {
			throw ApiException.conflict("This time is not available");
		}

		String phone = PhoneVerificationService.normalize(request.clientPhone());
		Client client = clients.findByPhoneE164(phone).orElseGet(Client::new);
		client.setPhoneE164(phone);
		client.setName(request.clientName().trim());
		if (request.clientEmail() != null && !request.clientEmail().isBlank()) {
			client.setEmail(request.clientEmail().trim());
		}
		clients.save(client);

		Appointment appointment = new Appointment();
		appointment.setClientId(client.getId());
		appointment.setStartAt(start);
		appointment.setEndAt(start.plus(Duration.ofMinutes(selection.durationMinutes())));
		appointment.setTimezone(selection.business().getTimezone());
		appointment.setSource(Appointment.Source.OWNER);
		appointment.setNotes(request.notes());
		appointments.save(appointment);

		items.saveAll(BookingService.snapshots(appointment.getId(), selection));
		events.save(AppointmentEvent.of(appointment.getId(), AppointmentEvent.CREATED,
				AppointmentEvent.Actor.OWNER, "Created by owner"));
		return detail(appointment);
	}

	@Transactional
	public AdminAppointmentDetail changeStatus(UUID id, AppointmentStatus target) {
		Appointment appointment = appointments.findById(id)
				.orElseThrow(() -> ApiException.notFound("Appointment not found"));
		AppointmentStatus current = appointment.getStatus();
		if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
			throw ApiException.conflict("Cannot change status from " + current + " to " + target);
		}

		Instant now = clock.instant();
		if (target == AppointmentStatus.IN_PROGRESS && appointment.getActualStartAt() == null) {
			appointment.setActualStartAt(now);
		}
		if (target == AppointmentStatus.COMPLETED && appointment.getActualEndAt() == null) {
			appointment.setActualEndAt(now);
		}
		appointment.setStatus(target);
		events.save(AppointmentEvent.of(appointment.getId(), AppointmentEvent.STATUS_CHANGED,
				AppointmentEvent.Actor.OWNER, current + " -> " + target));
		return detail(appointment);
	}

	// --- mapping --------------------------------------------------------------------

	private AdminAppointmentView summary(Appointment a, Client client, List<AppointmentItem> lines) {
		ZoneId zone = ZoneId.of(a.getTimezone());
		return new AdminAppointmentView(a.getId(), a.getStatus(),
				OffsetDateTime.ofInstant(a.getStartAt(), zone), OffsetDateTime.ofInstant(a.getEndAt(), zone),
				a.getTimezone(), serviceName(lines), clientView(client),
				lines.stream().mapToInt(AppointmentItem::getPriceCents).sum());
	}

	private AdminAppointmentDetail detail(Appointment a) {
		ZoneId zone = ZoneId.of(a.getTimezone());
		Client client = clients.findById(a.getClientId()).orElse(null);
		List<AppointmentItem> lines = items.findByAppointmentIdOrderBySortOrderAsc(a.getId());
		List<AppointmentEventView> history = events.findByAppointmentIdOrderByCreatedAtAsc(a.getId()).stream()
				.map(e -> new AppointmentEventView(e.getEventType(), e.getActor().name(), e.getDetail(),
						OffsetDateTime.ofInstant(e.getCreatedAt(), zone)))
				.toList();
		return new AdminAppointmentDetail(a.getId(), a.getStatus(),
				OffsetDateTime.ofInstant(a.getStartAt(), zone), OffsetDateTime.ofInstant(a.getEndAt(), zone),
				a.getTimezone(), serviceName(lines), clientView(client),
				lines.stream().mapToInt(AppointmentItem::getPriceCents).sum(), a.getNotes(),
				a.getActualStartAt() == null ? null : OffsetDateTime.ofInstant(a.getActualStartAt(), zone),
				a.getActualEndAt() == null ? null : OffsetDateTime.ofInstant(a.getActualEndAt(), zone),
				lines.stream().map(i -> new AdminAppointmentItemView(i.getItemType().name(), i.getName(),
						i.getDurationMinutes(), i.getPriceCents())).toList(),
				history);
	}

	private static String serviceName(List<AppointmentItem> lines) {
		return lines.stream()
				.filter(i -> i.getItemType() == AppointmentItem.ItemType.SERVICE)
				.map(AppointmentItem::getName)
				.findFirst().orElse("");
	}

	private static AdminClientView clientView(Client client) {
		return client == null ? null
				: new AdminClientView(client.getId(), client.getName(), client.getPhoneE164(),
						client.getEmail());
	}

	private ZoneId businessZone() {
		return businesses.findFirstByOrderByCreatedAtAsc()
				.map(b -> ZoneId.of(b.getTimezone()))
				.orElse(ZoneId.of("UTC"));
	}
}
