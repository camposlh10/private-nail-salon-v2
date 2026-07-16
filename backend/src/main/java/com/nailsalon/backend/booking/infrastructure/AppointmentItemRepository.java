package com.nailsalon.backend.booking.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nailsalon.backend.booking.domain.AppointmentItem;

public interface AppointmentItemRepository extends JpaRepository<AppointmentItem, UUID> {

	List<AppointmentItem> findByAppointmentIdOrderBySortOrderAsc(UUID appointmentId);

	List<AppointmentItem> findByAppointmentIdInOrderBySortOrderAsc(Collection<UUID> appointmentIds);
}
