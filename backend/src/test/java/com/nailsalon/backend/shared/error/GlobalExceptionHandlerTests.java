package com.nailsalon.backend.shared.error;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void apiExceptionReturnsProblemJsonWithStatusAndCode() throws Exception {
		mockMvc.perform(get("/__test/errors/api-exception"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status", is(404)))
				.andExpect(jsonPath("$.code", is("NOT_FOUND")))
				.andExpect(jsonPath("$.detail", is("thing missing")))
				.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void validationErrorReturns400WithFieldErrors() throws Exception {
		mockMvc.perform(post("/__test/errors/validate").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
				.andExpect(jsonPath("$.errors.name").exists());
	}

	@Test
	void malformedJsonReturns400() throws Exception {
		mockMvc.perform(post("/__test/errors/validate").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("not json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", is("MALFORMED_REQUEST")));
	}

	@Test
	void unexpectedExceptionReturns500WithoutLeakingItsMessage() throws Exception {
		mockMvc.perform(get("/__test/errors/boom"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code", is("INTERNAL_ERROR")))
				.andExpect(jsonPath("$.detail", is("Something went wrong")));
	}

	@Test
	void unknownRouteReturnsProblemJson404() throws Exception {
		mockMvc.perform(get("/__test/does-not-exist"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").exists());
	}
}
