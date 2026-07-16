package com.nailsalon.backend.booking.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nailsalon.backend.booking.BookingProperties;
import com.nailsalon.backend.booking.api.publicapi.PublicBookingDtos.AvailabilityDay;
import com.nailsalon.backend.booking.api.publicapi.PublicBookingDtos.AvailabilityResponse;
import com.nailsalon.backend.booking.domain.AppointmentStatus;
import com.nailsalon.backend.booking.domain.AvailabilityOverride;
import com.nailsalon.backend.booking.domain.SlotHold;
import com.nailsalon.backend.booking.domain.WeeklyAvailability;
import com.nailsalon.backend.booking.infrastructure.AppointmentRepository;
import com.nailsalon.backend.booking.infrastructure.AvailabilityOverrideRepository;
import com.nailsalon.backend.booking.infrastructure.BlockedTimeRepository;
import com.nailsalon.backend.booking.infrastructure.SlotHoldRepository;
import com.nailsalon.backend.booking.infrastructure.WeeklyAvailabilityRepository;
import com.nailsalon.backend.business.BusinessProfile;
import com.nailsalon.backend.business.BusinessProfileRepository;
import com.nailsalon.backend.catalog.domain.AddOnStatus;
import com.nailsalon.backend.catalog.domain.SalonService;
import com.nailsalon.backend.catalog.domain.ServiceAddOn;
import com.nailsalon.backend.catalog.domain.ServiceStatus;
import com.nailsalon.backend.catalog.infrastructure.SalonServiceRepository;
import com.nailsalon.backend.catalog.infrastructure.ServiceAddOnRepository;
import com.nailsalon.backend.catalog.infrastructure.ServiceCategoryRepository;
import com.nailsalon.backend.shared.error.ApiException;

/**
 * The single source of truth for "when can this service be booked". Candidate starts
 * are generated on a fixed grid inside the day's opening windows (a date override
 * REPLACES the weekly hours), then filtered by minimum notice, the booking horizon,
 * and every occupied interval: blocking-status appointments, blocked time and ACTIVE
 * unexpired slot holds. All interval logic is half-open [start, end), so back-to-back
 * bookings never conflict. Frontends must never re-derive any of this.
 */
@Service
public class AvailabilityService {

	private final BusinessProfileRepository businesses;
	private final SalonServiceRepository services;
	private final ServiceAddOnRepository addOns;
	private final ServiceCategoryRepository categories;
	private final WeeklyAvailabilityRepository weekly;
	private final AvailabilityOverrideRepository overrides;
	private final BlockedTimeRepository blocks;
	private final AppointmentRepository appointments;
	private final SlotHoldRepository holds;
	private final BookingProperties properties;
	private final Clock clock;

	public AvailabilityService(BusinessProfileRepository businesses, SalonServiceRepository services,
			ServiceAddOnRepository addOns, ServiceCategoryRepository categories,
			WeeklyAvailabilityRepository weekly, AvailabilityOverrideRepository overrides,
			BlockedTimeRepository blocks, AppointmentRepository appointments, SlotHoldRepository holds,
			BookingProperties properties, Clock clock) {
		this.businesses = businesses;
		this.services = services;
		this.addOns = addOns;
		this.categories = categories;
		this.weekly = weekly;
		this.overrides = overrides;
		this.blocks = blocks;
		this.appointments = appointments;
		this.holds = holds;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public AvailabilityResponse availability(UUID serviceId, List<UUID> addOnIds, LocalDate from, LocalDate to) {
		if (from == null || to == null || to.isBefore(from)) {
			throw ApiException.badRequest("Invalid date range");
		}
		BookingSelection selection = loadSelection(serviceId, addOnIds);
		ZoneId zone = selection.zone();
		LocalDate today = LocalDate.ofInstant(clock.instant(), zone);
		LocalDate first = from.isBefore(today) ? today : from;
		LocalDate last = min(to, today.plusDays(properties.horizonDays()));

		List<AvailabilityDay> days = new ArrayList<>();
		for (LocalDate date = first; !date.isAfter(last); date = date.plusDays(1)) {
			List<OffsetDateTime> slots = slotsForDate(selection, date, null).stream()
					.map(instant -> OffsetDateTime.ofInstant(instant, zone))
					.toList();
			days.add(new AvailabilityDay(date, slots));
		}
		return new AvailabilityResponse(selection.service().getId(), selection.durationMinutes(),
				zone.getId(), days);
	}

	/**
	 * Validates the service (must be ACTIVE, online-bookable, in an ACTIVE category)
	 * and add-ons (must belong to the service and be ACTIVE), and resolves them for
	 * server-side duration/price computation.
	 */
	@Transactional(readOnly = true)
	public BookingSelection loadSelection(UUID serviceId, List<UUID> addOnIds) {
		BusinessProfile business = businesses.findFirstByOrderByCreatedAtAsc()
				.orElseThrow(() -> ApiException.notFound("Business is not configured"));
		SalonService service = services.findById(serviceId)
				.filter(s -> s.getStatus() == ServiceStatus.ACTIVE && s.isOnlineBookable())
				.orElseThrow(() -> ApiException.notFound("Service is not bookable"));
		categories.findById(service.getCategoryId())
				.filter(c -> c.getStatus() == ServiceStatus.ACTIVE)
				.orElseThrow(() -> ApiException.notFound("Service is not bookable"));

		List<UUID> distinctIds = (addOnIds == null ? List.<UUID>of() : addOnIds).stream().distinct().toList();
		List<ServiceAddOn> selectedAddOns = distinctIds.stream()
				.map(id -> addOns.findById(id)
						.filter(a -> a.getServiceId().equals(service.getId())
								&& a.getStatus() == AddOnStatus.ACTIVE)
						.orElseThrow(() -> ApiException.badRequest("Add-on is not available for this service")))
				.toList();
		return new BookingSelection(business, service, selectedAddOns);
	}

	/**
	 * Bookable start instants for one local date. {@code ignoreHoldId} lets the
	 * booking flow treat the customer's own hold as free while it is being consumed.
	 */
	@Transactional(readOnly = true)
	public List<Instant> slotsForDate(BookingSelection selection, LocalDate date, UUID ignoreHoldId) {
		ZoneId zone = selection.zone();
		Instant now = clock.instant();
		LocalDate today = LocalDate.ofInstant(now, zone);
		if (date.isBefore(today) || date.isAfter(today.plusDays(properties.horizonDays()))) {
			return List.of();
		}

		List<Window> windows = windowsFor(date);
		if (windows.isEmpty()) {
			return List.of();
		}

		int duration = selection.durationMinutes();
		Instant earliestStart = now.plus(Duration.ofMinutes(properties.minNoticeMinutes()));
		// One occupied-interval fetch covering the whole local day (a slot can only
		// start inside a window, so the day span with duration padding is enough).
		Instant dayStart = date.atStartOfDay(zone).toInstant();
		Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().plus(Duration.ofMinutes(duration));
		List<Occupied> occupied = occupiedIntervals(dayStart, dayEnd, ignoreHoldId);

		List<Instant> slots = new ArrayList<>();
		for (Window window : windows) {
			LocalDateTime windowEnd = date.atTime(window.end());
			for (LocalDateTime candidate = date.atTime(window.start());
					!candidate.plusMinutes(duration).isAfter(windowEnd);
					candidate = candidate.plusMinutes(properties.slotGridMinutes())) {
				// Wall-clock times inside a spring-forward DST gap do not exist — skip.
				if (zone.getRules().getValidOffsets(candidate).isEmpty()) {
					continue;
				}
				Instant slotStart = candidate.atZone(zone).toInstant();
				Instant slotEnd = slotStart.plus(Duration.ofMinutes(duration));
				if (slotStart.isBefore(earliestStart)) {
					continue;
				}
				boolean conflict = occupied.stream()
						.anyMatch(o -> slotStart.isBefore(o.end()) && o.start().isBefore(slotEnd));
				if (!conflict) {
					slots.add(slotStart);
				}
			}
		}
		return slots;
	}

	/** Whether the exact start instant is currently a valid, free slot. */
	@Transactional(readOnly = true)
	public boolean isSlotAvailable(BookingSelection selection, Instant start, UUID ignoreHoldId) {
		LocalDate date = LocalDate.ofInstant(start, selection.zone());
		return slotsForDate(selection, date, ignoreHoldId).contains(start);
	}

	private record Window(LocalTime start, LocalTime end) {
	}

	private record Occupied(Instant start, Instant end) {
	}

	/** Opening windows for a date: an override REPLACES the weekly hours entirely. */
	private List<Window> windowsFor(LocalDate date) {
		Optional<AvailabilityOverride> override = overrides.findByDate(date);
		if (override.isPresent()) {
			AvailabilityOverride o = override.get();
			return o.isClosed() ? List.of() : List.of(new Window(o.getStartTime(), o.getEndTime()));
		}
		return weekly.findByDayOfWeekOrderByStartTimeAsc(date.getDayOfWeek().getValue()).stream()
				.map(w -> new Window(w.getStartTime(), w.getEndTime()))
				.toList();
	}

	private List<Occupied> occupiedIntervals(Instant from, Instant to, UUID ignoreHoldId) {
		List<Occupied> occupied = new ArrayList<>();
		appointments.findByStatusInAndStartAtLessThanAndEndAtGreaterThan(AppointmentStatus.BLOCKING, to, from)
				.forEach(a -> occupied.add(new Occupied(a.getStartAt(), a.getEndAt())));
		blocks.findByStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(to, from)
				.forEach(b -> occupied.add(new Occupied(b.getStartAt(), b.getEndAt())));
		holds.findByStatusAndExpiresAtAfterAndStartAtLessThanAndEndAtGreaterThan(
						SlotHold.Status.ACTIVE, clock.instant(), to, from).stream()
				.filter(h -> !h.getId().equals(ignoreHoldId))
				.forEach(h -> occupied.add(new Occupied(h.getStartAt(), h.getEndAt())));
		return occupied;
	}

	private static LocalDate min(LocalDate a, LocalDate b) {
		return a.isBefore(b) ? a : b;
	}
}
