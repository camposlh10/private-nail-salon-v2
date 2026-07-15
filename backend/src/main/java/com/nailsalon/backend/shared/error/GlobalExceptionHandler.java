package com.nailsalon.backend.shared.error;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.validation.ConstraintViolationException;

/**
 * Renders every error as RFC 7807 {@code application/problem+json}. Each problem carries a
 * stable {@code code} extension (machine-readable) alongside the standard fields, plus a
 * {@code timestamp}. Field-level validation failures are attached under {@code errors}.
 *
 * <p>Extends {@link ResponseEntityExceptionHandler} so Spring MVC's own exceptions (unreadable
 * body, unsupported method, missing params, etc.) also flow through problem+json.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	public ProblemDetail handleApi(ApiException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
		decorate(problem, ex.getCode());
		return problem;
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
		Map<String, String> errors = new LinkedHashMap<>();
		ex.getConstraintViolations()
				.forEach(v -> errors.put(v.getPropertyPath().toString(), v.getMessage()));
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
		decorate(problem, "VALIDATION_ERROR");
		problem.setProperty("errors", errors);
		return problem;
	}

	/** Bad JSON / wrong-typed query param that Spring surfaces outside the MVC hooks below. */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed or invalid request");
		decorate(problem, "MALFORMED_REQUEST");
		return problem;
	}

	/**
	 * Optimistic-lock or unique-constraint races that slip past application-level checks
	 * (e.g. two concurrent edits, or duplicate slugs inserted simultaneously).
	 */
	@ExceptionHandler({ OptimisticLockingFailureException.class, DataIntegrityViolationException.class })
	public ProblemDetail handleDataConflict(Exception ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"The request conflicts with the current state of the data");
		decorate(problem, "CONFLICT");
		return problem;
	}

	// Note: MaxUploadSizeExceededException (multipart too large) is handled by
	// ResponseEntityExceptionHandler itself and flows through handleExceptionInternal below.

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception ex) {
		log.error("Unhandled exception", ex);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong");
		decorate(problem, "INTERNAL_ERROR");
		return problem;
	}

	// --- Spring MVC framework exceptions routed through problem+json ---------------------

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
		decorate(problem, "VALIDATION_ERROR");
		problem.setProperty("errors", errors);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed or invalid request");
		decorate(problem, "MALFORMED_REQUEST");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
	}

	/**
	 * Fallback for every other Spring MVC exception (404 no-handler, 405 method not supported,
	 * 415, missing params...). Re-uses the ProblemDetail Spring already built and stamps our
	 * {@code code} extension onto it so the response shape stays uniform.
	 */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
		Object responseBody = response != null ? response.getBody() : null;
		if (responseBody instanceof ProblemDetail problem) {
			String code = ex instanceof ErrorResponseException
					? HttpStatus.valueOf(problem.getStatus()).name()
					: "REQUEST_ERROR";
			decorate(problem, code);
		}
		return response;
	}

	private void decorate(ProblemDetail problem, String code) {
		problem.setType(URI.create("about:blank"));
		problem.setProperty("code", code);
		problem.setProperty("timestamp", Instant.now());
	}
}
