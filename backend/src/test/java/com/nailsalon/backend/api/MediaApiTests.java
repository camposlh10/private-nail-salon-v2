package com.nailsalon.backend.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
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
import com.nailsalon.backend.catalog.domain.PriceType;
import com.nailsalon.backend.catalog.domain.SalonService;
import com.nailsalon.backend.catalog.domain.ServiceCategory;
import com.nailsalon.backend.catalog.domain.ServiceStatus;
import com.nailsalon.backend.catalog.infrastructure.SalonServiceRepository;
import com.nailsalon.backend.catalog.infrastructure.ServiceAddOnRepository;
import com.nailsalon.backend.catalog.infrastructure.ServiceCategoryRepository;
import com.nailsalon.backend.media.MediaAssetRepository;
import com.nailsalon.backend.shared.audit.AuditEventRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MediaApiTests {

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
	private MediaAssetRepository mediaRepository;
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
		mediaRepository.deleteAll();
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
	void uploadServeAndDeleteRoundTrip() throws Exception {
		MockHttpSession session = login();

		MvcResult uploaded = mockMvc.perform(multipart("/api/v1/admin/media")
						.file(new MockMultipartFile("file", "photo.png", "image/png", tinyPng()))
						.param("altText", "Gel manicure result")
						.session(session).with(csrf()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.contentType", is("image/png")))
				.andExpect(jsonPath("$.width", is(2)))
				.andExpect(jsonPath("$.url", containsString("/api/v1/public/media/")))
				.andReturn();
		JsonNode media = objectMapper.readTree(uploaded.getResponse().getContentAsString());
		String mediaId = media.get("id").asText();

		// Served publicly with long-lived cache headers
		mockMvc.perform(get("/api/v1/public/media/" + mediaId))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "image/png"))
				.andExpect(header().string("Cache-Control", containsString("immutable")));

		// Unreferenced delete works
		mockMvc.perform(delete("/api/v1/admin/media/" + mediaId).session(session).with(csrf()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/public/media/" + mediaId))
				.andExpect(status().isNotFound());
	}

	@Test
	void mismatchedSignatureIsRejected() throws Exception {
		MockHttpSession session = login();
		mockMvc.perform(multipart("/api/v1/admin/media")
						.file(new MockMultipartFile("file", "fake.png", "image/png",
								"this is not an image".getBytes()))
						.session(session).with(csrf()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", is("BAD_REQUEST")));
	}

	@Test
	void deletingReferencedImageIs409() throws Exception {
		MockHttpSession session = login();

		MvcResult uploaded = mockMvc.perform(multipart("/api/v1/admin/media")
						.file(new MockMultipartFile("file", "photo.png", "image/png", tinyPng()))
						.session(session).with(csrf()))
				.andExpect(status().isCreated())
				.andReturn();
		String mediaId = objectMapper.readTree(uploaded.getResponse().getContentAsString())
				.get("id").asText();

		ServiceCategory category = new ServiceCategory();
		category.setBusinessId(businessRepository.findFirstByOrderByCreatedAtAsc().orElseThrow().getId());
		category.setName("Manicure");
		category.setSlug("manicure");
		category.setStatus(ServiceStatus.ACTIVE);
		categoryRepository.save(category);

		SalonService service = new SalonService();
		service.setCategoryId(category.getId());
		service.setName("Gel");
		service.setSlug("gel");
		service.setDurationMinutes(60);
		service.setPriceType(PriceType.FIXED);
		service.setPriceCents(5500);
		service.setImageId(java.util.UUID.fromString(mediaId));
		service.setStatus(ServiceStatus.ACTIVE);
		serviceRepository.save(service);

		mockMvc.perform(delete("/api/v1/admin/media/" + mediaId).session(session).with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", is("CONFLICT")));
	}

	@Test
	void uploadRequiresAuthentication() throws Exception {
		mockMvc.perform(multipart("/api/v1/admin/media")
						.file(new MockMultipartFile("file", "photo.png", "image/png", tinyPng()))
						.with(csrf()))
				.andExpect(status().isUnauthorized());
	}

	private MockHttpSession login() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/admin/auth/login").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + OWNER_EMAIL + "\",\"password\":\"" + OWNER_PASSWORD + "\"}"))
				.andExpect(status().isOk())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private static byte[] tinyPng() throws Exception {
		BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "png", out);
		return out.toByteArray();
	}
}
