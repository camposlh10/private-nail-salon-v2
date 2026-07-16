package com.nailsalon.backend.booking.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nailsalon.backend.booking.domain.WeeklyAvailability;

public interface WeeklyAvailabilityRepository extends JpaRepository<WeeklyAvailability, UUID> {

	List<WeeklyAvailability> findAllByOrderByDayOfWeekAscStartTimeAsc();

	List<WeeklyAvailability> findByDayOfWeekOrderByStartTimeAsc(int dayOfWeek);
}
