package com.nailsalon.backend.catalog.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nailsalon.backend.catalog.domain.SalonService;
import com.nailsalon.backend.catalog.domain.ServiceStatus;

public interface SalonServiceRepository extends JpaRepository<SalonService, UUID> {

	Page<SalonService> findByStatus(ServiceStatus status, Pageable pageable);

	Page<SalonService> findByCategoryId(UUID categoryId, Pageable pageable);

	Page<SalonService> findByCategoryIdAndStatus(UUID categoryId, ServiceStatus status, Pageable pageable);

	Optional<SalonService> findBySlug(String slug);

	boolean existsBySlug(String slug);

	boolean existsByImageId(UUID imageId);

	long countByCategoryId(UUID categoryId);

	java.util.List<SalonService> findByStatusAndOnlineBookableTrueOrderByDisplayOrderAscNameAsc(ServiceStatus status);
}
