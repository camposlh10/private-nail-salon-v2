package com.nailsalon.backend.booking.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nailsalon.backend.booking.domain.AppointmentEvent;

public interface AppointmentEventRepository extends JpaRepository<AppointmentEvent, UUID> {

	List<AppointmentEvent> findByAppointmentIdOrderByCreatedAtAsc(UUID appointmentId);
}
