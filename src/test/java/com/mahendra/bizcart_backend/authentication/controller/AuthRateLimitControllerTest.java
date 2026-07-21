package com.mahendra.bizcart_backend.authentication.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mahendra.bizcart_backend.authentication.config.AuthenticationProperties;
import com.mahendra.bizcart_backend.authentication.service.AuthRateLimiter;
import com.mahendra.bizcart_backend.authentication.service.AuthService;
import com.mahendra.bizcart_backend.authentication.web.ClientIpResolver;
import com.mahendra.bizcart_backend.common.exception.GlobalExceptionHandler;
import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthRateLimitControllerTest {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		AuthService authService = org.mockito.Mockito.mock(AuthService.class);
		AuthenticationProperties properties = new AuthenticationProperties();
		properties.getRateLimit().getForgotPassword().setMaxRequests(1);
		properties.getRateLimit().getForgotPassword().setWindowSeconds(60);
		AuthController controller = new AuthController(authService, properties,
				new AuthRateLimiter(Clock.systemUTC(), properties), new ClientIpResolver(properties));
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	void forgotPasswordReturns429AfterConfiguredLimit() throws Exception {
		String body = "{\"email\":\"customer@example.com\"}";
		mockMvc.perform(post("/api/v1/auth/forgot-password")
				.remoteAddress("127.0.0.1").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/auth/forgot-password")
				.remoteAddress("127.0.0.1").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isTooManyRequests())
			.andExpect(jsonPath("$.code").value("AUTH_TOO_MANY_REQUESTS"))
			.andExpect(jsonPath("$.message").value("Too many requests"));
	}
}
