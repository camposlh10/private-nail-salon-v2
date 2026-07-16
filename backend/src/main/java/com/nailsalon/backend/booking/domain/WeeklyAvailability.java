package com.nailsalon.backend.booking.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.nailsalon.backend.shared.persistence.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * One recurring opening interval; multiple rows per day model split shifts.
 * Stored as the ISO day number (1 = Monday .. 7 = Sunday). Intervals are [start, end).
 */
@Entity
@Table(name = "weekly_availability")
public class WeeklyAvailability extends AuditableEntity {

	@Column(name = "day_of_week", nullable = false)
	private int dayOfWeek;

	@Column(name = "start_time", nullable = false)
	private LocalTime startTime;

	@Column(name = "end_time", nullable = false)
	private LocalTime endTime;

	public DayOfWeek getDayOfWeek() {
		return DayOfWeek.of(dayOfWeek);
	}

	public void setDayOfWeek(DayOfWeek dayOfWeek) {
		this.dayOfWeek = dayOfWeek.getValue();
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalTime endTime) {
		this.endTime = endTime;
	}
}
