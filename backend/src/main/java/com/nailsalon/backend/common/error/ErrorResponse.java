package com.nailsalon.backend.common.error;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
		Instant timestamp,
		int status,
		String code,
		String message,
		String path,
		Map<String, String> fields) {

	public static ErrorResponse of(int status, String code, String message, String path) {
		return new ErrorResponse(Instant.now(), status, code, message, path, null);
	}

	public static ErrorResponse of(int status, String code, String message, String path, Map<String, String> fields) {
		return new ErrorResponse(Instant.now(), status, code, message, path, fields);
	}
}
