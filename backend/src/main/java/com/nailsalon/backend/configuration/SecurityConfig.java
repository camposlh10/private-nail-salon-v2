package com.nailsalon.backend.configuration;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Owner CRM security: server-side HTTP session in a Secure/HttpOnly cookie, SPA-style
 * CSRF (readable XSRF-TOKEN cookie mirrored back in the X-XSRF-TOKEN header), and
 * problem+json responses for authentication/authorization failures.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.cors(Customizer.withDefaults())
				.csrf(csrf -> csrf
						.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
						.csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
						// Provider webhooks authenticate with signatures, not sessions —
						// CSRF does not apply to them. Public booking endpoints are
						// anonymous (no Spring session to bind a CSRF token to); the
						// verified-phone cookie they use is HttpOnly + SameSite=Lax and
						// all mutations are JSON-only, which cross-site forms can't send.
						.ignoringRequestMatchers("/api/v1/webhooks/**", "/api/v1/public/**"))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/api/v1/public/**",
								"/api/v1/admin/auth/login",
								"/api/v1/admin/auth/csrf",
								"/api/v1/admin/auth/password/request-reset",
								"/api/v1/admin/auth/password/reset",
								// Signature-verified inside the controller (fail closed).
								"/api/v1/webhooks/**",
								"/actuator/health",
								// Maps to nothing in production (test-only controllers); kept
								// permitted so unknown-route handling stays uniform in tests.
								"/__test/**")
						.permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(problemEntryPoint())
						.accessDeniedHandler(problemAccessDeniedHandler()))
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable);
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(
			@Value("${app.cors.allowed-origins}") String allowedOrigins) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
		// Required so the browser sends the session cookie cross-origin (Next.js dev servers).
		config.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	private static AuthenticationEntryPoint problemEntryPoint() {
		return (request, response, exception) -> writeProblem(response,
				HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication required");
	}

	private static AccessDeniedHandler problemAccessDeniedHandler() {
		return (request, response, exception) -> writeProblem(response,
				HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied");
	}

	/**
	 * These problems are fixed strings (no user input), so they're rendered directly
	 * instead of pulling a JSON mapper into the security filter chain.
	 */
	private static void writeProblem(HttpServletResponse response, HttpStatus status,
			String code, String detail) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.getWriter().write("""
				{"type":"about:blank","title":"%s","status":%d,"detail":"%s","code":"%s","timestamp":"%s"}"""
				.formatted(status.getReasonPhrase(), status.value(), detail, code, Instant.now()));
	}

	/**
	 * The SPA CSRF handler from the Spring Security reference docs: XOR-encode tokens in
	 * responses (BREACH protection) but resolve plain header values sent by JS clients.
	 * Calling {@code csrfToken.get()} on every request forces the deferred token to load,
	 * which makes the cookie repository write/refresh the XSRF-TOKEN cookie.
	 */
	static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

		private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
		private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

		@Override
		public void handle(HttpServletRequest request, HttpServletResponse response,
				Supplier<CsrfToken> csrfToken) {
			this.xor.handle(request, response, csrfToken);
			csrfToken.get();
		}

		@Override
		public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
			String headerValue = request.getHeader(csrfToken.getHeaderName());
			return (StringUtils.hasText(headerValue) ? this.plain : this.xor)
					.resolveCsrfTokenValue(request, csrfToken);
		}
	}
}
