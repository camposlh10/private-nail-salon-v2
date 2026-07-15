package com.nailsalon.backend.catalog.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nailsalon.backend.catalog.domain.ServiceCategory;
import com.nailsalon.backend.catalog.domain.ServiceStatus;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, UUID> {

	List<ServiceCategory> findAllByOrderByDisplayOrderAscNameAsc();

	List<ServiceCategory> findByStatusOrderByDisplayOrderAscNameAsc(ServiceStatus status);

	Optional<ServiceCategory> findByBusinessIdAndNameNormalized(UUID businessId, String nameNormalized);

	boolean existsByBusinessIdAndNameNormalized(UUID businessId, String nameNormalized);

	boolean existsBySlug(String slug);
}
