package com.nailsalon.backend.auth.owner.api;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nailsalon.backend.auth.owner.LoginRateLimiter;
import com.nailsalon.backend.auth.owner.OwnerUser;
import com.nailsalon.backend.auth.owner.OwnerUserRepository;
import com.nailsalon.backend.auth.owner.PasswordResetService;
import com.nailsalon.backend.shared.error.ApiException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AuthController {

	public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
	}

	public record RequestResetRequest(@NotBlank @Email String email) {
	}

	public record ResetRequest(@NotBlank String token, @NotBlank @Size(min = 12) String newPassword) {
	}

	public record OwnerMe(UUID id, String email, Instant lastLoginAt) {

		static OwnerMe from(OwnerUser owner) {
			return new OwnerMe(owner.getId(), owner.getEmail(), owner.getLastLoginAt());
		}
	}

	private final AuthenticationManager authenticationManager;
	private final OwnerUserRepository owners;
	private final LoginRateLimiter rateLimiter;
	private final PasswordResetService passwordReset;
	private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

	public AuthController(AuthenticationManager authenticationManager, OwnerUserRepository owners,
			LoginRateLimiter rateLimiter, PasswordResetService passwordReset) {
		this.authenticationManager = authenticationManager;
		this.owners = owners;
		this.rateLimiter = rateLimiter;
		this.passwordReset = passwordReset;
	}

	/**
	 * No-op endpoint for SPA clients: any response passes through the CSRF handler,
	 * which sets/refreshes the XSRF-TOKEN cookie the client must mirror on mutations.
	 */
	@GetMapping("/csrf")
	public ResponseEntity<Void> csrf() {
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/login")
	public OwnerMe login(@Valid @RequestBody LoginRequest body, HttpServletRequest request,
			HttpServletResponse response) {
		String email = body.email().toLowerCase(Locale.ROOT);
		String rateKey = request.getRemoteAddr() + "|" + email;
		rateLimiter.checkAllowed(rateKey);

		Authentication authentication;
		try {
			authentication = authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(email, body.password()));
		} catch (AuthenticationException ex) {
			rateLimiter.recordFailure(rateKey);
			throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
		}
		rateLimiter.reset(rateKey);

		// Session fixation protection: rotate the session id on privilege change,
		// then persist the authenticated context into the (new) session.
		request.getSession(true);
		request.changeSessionId();
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);

		OwnerUser owner = owners.findByEmailIgnoreCase(email).orElseThrow();
		owner.setLastLoginAt(Instant.now());
		owners.save(owner);
		return OwnerMe.from(owner);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		SecurityContextHolder.clearContext();
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/me")
	public OwnerMe me(Authentication authentication) {
		return owners.findByEmailIgnoreCase(authentication.getName())
				.map(OwnerMe::from)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED",
						"Authentication required"));
	}

	@PostMapping("/password/request-reset")
	public ResponseEntity<Void> requestReset(@Valid @RequestBody RequestResetRequest body) {
		passwordReset.requestReset(body.email());
		// Always 202: never reveal whether the email has an account.
		return ResponseEntity.accepted().build();
	}

	@PostMapping("/password/reset")
	public ResponseEntity<Void> reset(@Valid @RequestBody ResetRequest body) {
		passwordReset.reset(body.token(), body.newPassword());
		return ResponseEntity.noContent().build();
	}
}
