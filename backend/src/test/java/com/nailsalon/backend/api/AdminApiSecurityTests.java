package com.nailsalon.backend.api;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.nailsalon.backend.auth.owner.OwnerUser;
import com.nailsalon.backend.auth.owner.OwnerUserRepository;
import com.nailsalon.backend.auth.owner.PasswordResetTokenRepository;
import com.nailsalon.backend.business.BusinessProfile;
import com.nailsalon.backend.business.BusinessProfileRepository;
import com.nailsalon.backend.catalog.infrastructure.SalonServiceRepository;
import com.nailsalon.backend.catalog.infrastructure.ServiceAddOnRepository;
import com.nailsalon.backend.catalog.infrastructure.ServiceCategoryRepository;
import com.nailsalon.backend.shared.audit.AuditEventRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminApiSecurityTests {

	private static final String OWNER_EMAIL = "owner@test.local";
	private static final String OWNER_PASSWORD = "correct-horse-battery";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ServiceAddOnRepository addOnRepository;
	@Autowired
	private SalonServiceRepository serviceRepository;
	@Autowired
	private ServiceCategoryRepository categoryRepository;
	@Autowired
	private PasswordResetTokenRepository resetTokenRepository;
	@Autowired
	private OwnerUserRepository ownerRepository;
	@Autowired
	private AuditEventRepository auditRepository;
	@Autowired
	private BusinessProfileRepository businessRepository;

	@BeforeEach
	void cleanAndSeed() {
		addOnRepository.deleteAll();
		serviceRepository.deleteAll();
		categoryRepository.deleteAll();
		resetTokenRepository.deleteAll();
		ownerRepository.deleteAll();
		auditRepository.deleteAll();
		businessRepository.deleteAll();

		BusinessProfile business = new BusinessProfile();
		business.setName("Test Salon");
		business.setSlug("test-salon");
		business.setTimezone("America/New_York");
		businessRepository.save(business);

		OwnerUser owner = new OwnerUser();
		owner.setEmail(OWNER_EMAIL);
		owner.setPasswordHash(passwordEncoder.encode(OWNER_PASSWORD));
		ownerRepository.save(owner);
	}

	@Test
	void adminRoutesRejectUnauthenticatedRequestsWithProblemJson() throws Exception {
		mockMvc.perform(get("/api/v1/admin/categories"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", is("UNAUTHENTICATED")));
	}

	@Test
	void loginWithWrongPasswordIs401() throws Exception {
		mockMvc.perform(post("/api/v1/admin/auth/login").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + OWNER_EMAIL + "\",\"password\":\"wrong\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")));
	}

	@Test
	void mutationsWithSessionButWithoutCsrfTokenAre403() throws Exception {
		MockHttpSession session = login();
		mockMvc.perform(post("/api/v1/admin/categories").session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Manicure\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", is("FORBIDDEN")));
	}

	@Test
	void fullCatalogCrudFlowWorksWithSessionAndCsrf() throws Exception {
		MockHttpSession session = login();

		mockMvc.perform(get("/api/v1/admin/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email", is(OWNER_EMAIL)));

		// Create category → 201 + Location
		MvcResult categoryResult = mockMvc.perform(post("/api/v1/admin/categories").session(session).with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Manicure\",\"description\":\"Hand care\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", notNullValue()))
				.andExpect(jsonPath("$.slug", is("manicure")))
				.andExpect(jsonPath("$.status", is("ACTIVE")))
				.andReturn();
		JsonNode category = objectMapper.readTree(categoryResult.getResponse().getContentAsString());

		// Duplicate category name → 409 problem+json
		mockMvc.perform(post("/api/v1/admin/categories").session(session).with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"manicure\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", is("CONFLICT")));

		// Create service → 201
		String serviceJson = """
				{"categoryId":"%s","name":"Gel Manicure","durationMinutes":60,
				 "priceType":"FIXED","priceCents":5500}
				""".formatted(category.get("id").asText());
		MvcResult serviceResult = mockMvc.perform(post("/api/v1/admin/services").session(session).with(csrf())
						.contentType(MediaType.APPLICATION_JSON).content(serviceJson))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.slug", is("gel-manicure")))
				.andReturn();
		JsonNode service = objectMapper.readTree(serviceResult.getResponse().getContentAsString());
		String serviceId = service.get("id").asText();

		// Invalid pricing (FREE with amount) → 400
		mockMvc.perform(post("/api/v1/admin/services").session(session).with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"categoryId":"%s","name":"Freebie","durationMinutes":30,
								 "priceType":"FREE","priceCents":100}
								""".formatted(category.get("id").asText())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", is("BAD_REQUEST")));

		// Add-on → 201
		mockMvc.perform(post("/api/v1/admin/services/" + serviceId + "/addons").session(session).with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Nail Art\",\"addedDurationMinutes\":15,\"priceCents\":500}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status", is("ACTIVE")));

		// Archive service → retained but ARCHIVED
		mockMvc.perform(patch("/api/v1/admin/services/" + serviceId + "/status").session(session).with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"ARCHIVED\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("ARCHIVED")));

		// Logout → session gone
		mockMvc.perform(post("/api/v1/admin/auth/logout").session(session).with(csrf()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/admin/auth/me").session(session))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void repeatedFailedLoginsAreRateLimited() throws Exception {
		OwnerUser other = new OwnerUser();
		other.setEmail("ratelimit@test.local");
		other.setPasswordHash(passwordEncoder.encode("some-password-123"));
		ownerRepository.save(other);

		String body = "{\"email\":\"ratelimit@test.local\",\"password\":\"wrong\"}";
		for (int i = 0; i < 10; i++) {
			mockMvc.perform(post("/api/v1/admin/auth/login").with(csrf())
							.contentType(MediaType.APPLICATION_JSON).content(body))
					.andExpect(status().isUnauthorized());
		}
		mockMvc.perform(post("/api/v1/admin/auth/login").with(csrf())
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code", is("RATE_LIMITED")));
	}

	private MockHttpSession login() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/admin/auth/login").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + OWNER_EMAIL + "\",\"password\":\"" + OWNER_PASSWORD + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email", is(OWNER_EMAIL)))
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}
}
