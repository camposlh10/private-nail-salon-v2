package com.nailsalon.backend.communications.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nailsalon.backend.communications.domain.CommunicationChannel;
import com.nailsalon.backend.communications.domain.CommunicationConsent;
import com.nailsalon.backend.communications.domain.ConsentPurpose;

public interface CommunicationConsentRepository extends JpaRepository<CommunicationConsent, UUID> {

	Optional<CommunicationConsent> findByPhoneAndChannelAndPurpose(String phone,
			CommunicationChannel channel, ConsentPurpose purpose);
}
