package com.nailsalon.backend.common.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Test-only endpoints (this class lives in src/test, not the production jar) used to
 * exercise every branch of {@link GlobalExceptionHandler} without needing real domain
 * controllers, which don't exist yet at this foundation milestone.
 */
@RestController
@RequestMapping("/__test/errors")
class TestErrorSupportController {

	record Body(@NotBlank String name) {
	}

	@PostMapping("/validate")
	ResponseEntity<Void> validate(@Valid @RequestBody Body body) {
		return ResponseEntity.ok().build();
	}

	@GetMapping("/api-exception")
	void apiException() {
		throw ApiException.notFound("thing missing");
	}

	@GetMapping("/boom")
	void boom() {
		throw new RuntimeException("boom");
	}
}
