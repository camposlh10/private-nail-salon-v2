package com.nailsalon.backend.shared.audit;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Records audit events, attributing them to the authenticated principal when present. */
@Component
public class AuditRecorder {

	private final AuditEventRepository repository;

	public AuditRecorder(AuditEventRepository repository) {
		this.repository = repository;
	}

	public void record(String action, String resourceType, UUID resourceId, String changeSummary) {
		repository.save(new AuditEvent(currentActor(), action, resourceType,
				resourceId != null ? resourceId.toString() : null, changeSummary));
	}

	private String currentActor() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
			return null;
		}
		return auth.getName();
	}
}
