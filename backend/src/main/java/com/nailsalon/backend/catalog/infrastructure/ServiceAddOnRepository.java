package com.nailsalon.backend.catalog.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nailsalon.backend.catalog.domain.AddOnStatus;
import com.nailsalon.backend.catalog.domain.ServiceAddOn;

public interface ServiceAddOnRepository extends JpaRepository<ServiceAddOn, UUID> {

	List<ServiceAddOn> findByServiceIdOrderByDisplayOrderAscNameAsc(UUID serviceId);

	List<ServiceAddOn> findByServiceIdAndStatusOrderByDisplayOrderAscNameAsc(UUID serviceId, AddOnStatus status);
}
