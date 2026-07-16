package com.nailsalon.backend.booking.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nailsalon.backend.booking.domain.Client;

public interface ClientRepository extends JpaRepository<Client, UUID> {

	Optional<Client> findByPhoneE164(String phoneE164);
}
