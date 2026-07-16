package com.nailsalon.backend.booking.application;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nailsalon.backend.booking.api.admin.ScheduleDtos.AdminBlock;
import com.nailsalon.backend.booking.api.admin.ScheduleDtos.AdminOverride;
import com.nailsalon.backend.booking.api.admin.ScheduleDtos.BlockWrite;
import com.nailsalon.backend.booking.api.admin.ScheduleDtos.OverrideWrite;
import com.nailsalon.backend.booking.api.admin.ScheduleDtos.WeeklyInterval;
import com.nailsalon.backend.booking.api.admin.ScheduleDtos.WeeklySchedule;
import com.nailsalon.backend.booking.domain.AvailabilityOverride;
import com.nailsalon.backend.booking.domain.BlockedTime;
import com.nailsalon.backend.booking.domain.WeeklyAvailability;
import com.nailsalon.backend.booking.infrastructure.AvailabilityOverrideRepository;
import com.nailsalon.backend.booking.infrastructure.BlockedTimeRepository;
import com.nailsalon.backend.booking.infrastructure.WeeklyAvailabilityRepository;
import com.nailsalon.backend.business.BusinessProfileRepository;
import com.nailsalon.backend.shared.error.ApiException;

/** Owner-facing schedule management: weekly hours, date overrides and blocked time. */
@Service
public class ScheduleAdminService {

	private final WeeklyAvailabilityRepository weekly;
	private final AvailabilityOverrideRepository overrides;
	private final BlockedTimeRepository blocks;
	private final BusinessProfileRepository businesses;

	public ScheduleAdminService(WeeklyAvailabilityRepository weekly, AvailabilityOverrideRepository overrides,
			BlockedTimeRepository blocks, BusinessProfileRepository businesses) {
		this.weekly = weekly;
		this.overrides = overrides;
		this.blocks = blocks;
		this.businesses = businesses;
	}

	@Transactional(readOnly = true)
	public WeeklySchedule weekly() {
		return new WeeklySchedule(weekly.findAllByOrderByDayOfWeekAscStartTimeAsc().stream()
				.map(w -> new WeeklyInterval(w.getDayOfWeek(), w.getStartTime(), w.getEndTime()))
				.toList());
	}

	@Transactional
	public WeeklySchedule replaceWeekly(WeeklySchedule schedule) {
		validateWeekly(schedule.days());
		weekly.deleteAll();
		weekly.saveAll(schedule.days().stream().map(interval -> {
			WeeklyAvailability row = new WeeklyAvailability();
			row.setDayOfWeek(interval.dayOfWeek());
			row.setStartTime(interval.startTime());
			row.setEndTime(interval.endTime());
			return row;
		}).toList());
		return weekly();
	}

	@Transactional(readOnly = true)
	public List<AdminOverride> overrides(LocalDate from, LocalDate to) {
		List<AvailabilityOverride> rows = (from != null && to != null)
				? overrides.findByDateBetweenOrderByDateAsc(from, to)
				: overrides.findAllByOrderByDateAsc();
		return rows.stream().map(ScheduleAdminService::toDto).toList();
	}

	@Transactional
	public AdminOverride createOverride(OverrideWrite request) {
		boolean closed = Boolean.TRUE.equals(request.closed());
		if (closed && (request.startTime() != null || request.endTime() != null)) {
			throw ApiException.badRequest("A closed day cannot have hours");
		}
		if (!closed) {
			if (request.startTime() == null || request.endTime() == null
					|| !request.startTime().isBefore(request.endTime())) {
				throw ApiException.badRequest("Special hours need startTime < endTime");
			}
		}
		if (overrides.findByDate(request.date()).isPresent()) {
			throw ApiException.conflict("This date already has an override");
		}
		AvailabilityOverride row = new AvailabilityOverride();
		row.setDate(request.date());
		row.setClosed(closed);
		row.setStartTime(request.startTime());
		row.setEndTime(request.endTime());
		row.setReason(request.reason());
		return toDto(overrides.save(row));
	}

	@Transactional
	public void deleteOverride(UUID id) {
		AvailabilityOverride row = overrides.findById(id)
				.orElseThrow(() -> ApiException.notFound("Override not found"));
		overrides.delete(row);
	}

	@Transactional(readOnly = true)
	public List<AdminBlock> blocks(LocalDate from, LocalDate to) {
		ZoneId zone = businessZone();
		List<BlockedTime> rows;
		if (from != null && to != null) {
			rows = blocks.findByStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(
					to.plusDays(1).atStartOfDay(zone).toInstant(), from.atStartOfDay(zone).toInstant());
		}
		else {
			rows = blocks.findAll().stream()
					.sorted(Comparator.comparing(BlockedTime::getStartAt)).toList();
		}
		return rows.stream()
				.map(b -> new AdminBlock(b.getId(), OffsetDateTime.ofInstant(b.getStartAt(), zone),
						OffsetDateTime.ofInstant(b.getEndAt(), zone), b.getReason()))
				.toList();
	}

	@Transactional
	public AdminBlock createBlock(BlockWrite request) {
		if (!request.start().isBefore(request.end())) {
			throw ApiException.badRequest("Block needs start < end");
		}
		BlockedTime row = new BlockedTime();
		row.setStartAt(request.start().toInstant());
		row.setEndAt(request.end().toInstant());
		row.setReason(request.reason());
		BlockedTime saved = blocks.save(row);
		ZoneId zone = businessZone();
		return new AdminBlock(saved.getId(), OffsetDateTime.ofInstant(saved.getStartAt(), zone),
				OffsetDateTime.ofInstant(saved.getEndAt(), zone), saved.getReason());
	}

	@Transactional
	public void deleteBlock(UUID id) {
		BlockedTime row = blocks.findById(id).orElseThrow(() -> ApiException.notFound("Block not found"));
		blocks.delete(row);
	}

	private static void validateWeekly(List<WeeklyInterval> intervals) {
		for (WeeklyInterval interval : intervals) {
			if (!interval.startTime().isBefore(interval.endTime())) {
				throw ApiException.badRequest("Weekly hours need startTime < endTime");
			}
		}
		Map<DayOfWeek, List<WeeklyInterval>> byDay = intervals.stream()
				.collect(Collectors.groupingBy(WeeklyInterval::dayOfWeek));
		for (List<WeeklyInterval> day : byDay.values()) {
			List<WeeklyInterval> sorted = day.stream()
					.sorted(Comparator.comparing(WeeklyInterval::startTime)).toList();
			for (int i = 1; i < sorted.size(); i++) {
				// [start, end): touching intervals (12:00 end, 12:00 start) are fine.
				if (sorted.get(i).startTime().isBefore(sorted.get(i - 1).endTime())) {
					throw ApiException.badRequest("Weekly hours overlap on " + sorted.get(i).dayOfWeek());
				}
			}
		}
	}

	private static AdminOverride toDto(AvailabilityOverride o) {
		return new AdminOverride(o.getId(), o.getDate(), o.isClosed(), o.getStartTime(), o.getEndTime(),
				o.getReason());
	}

	private ZoneId businessZone() {
		return businesses.findFirstByOrderByCreatedAtAsc()
				.map(b -> ZoneId.of(b.getTimezone()))
				.orElse(ZoneId.of("UTC"));
	}
}
