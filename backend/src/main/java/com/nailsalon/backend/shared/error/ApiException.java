package com.nailsalon.backend.shared.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Application-level exception carrying an HTTP status and a stable machine-readable
 * {@code code}. Surfaced by {@link GlobalExceptionHandler} as RFC 7807
 * {@code application/problem+json}.
 */
public class ApiException extends RuntimeException {

	private final HttpStatusCode status;
	private final String code;

	public ApiException(HttpStatusCode status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	public static ApiException notFound(String message) {
		return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
	}

	public static ApiException badRequest(String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
	}

	public static ApiException conflict(String message) {
		return new ApiException(HttpStatus.CONFLICT, "CONFLICT", message);
	}

	public HttpStatusCode getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}
}
