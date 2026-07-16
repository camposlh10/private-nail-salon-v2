package com.nailsalon.backend.booking.infrastructure;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nailsalon.backend.booking.domain.AvailabilityOverride;

public interface AvailabilityOverrideRepository extends JpaRepository<AvailabilityOverride, UUID> {

	Optional<AvailabilityOverride> findByDate(LocalDate date);

	List<AvailabilityOverride> findByDateBetweenOrderByDateAsc(LocalDate from, LocalDate to);

	List<AvailabilityOverride> findAllByOrderByDateAsc();
}
