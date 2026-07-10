package com.mahendra.bizcart_backend.authentication.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mahendra.bizcart_backend.authentication.security.JwtAuthenticationFilter;
import com.mahendra.bizcart_backend.authentication.security.JwtTokenProvider;
import com.mahendra.bizcart_backend.authentication.security.RestAccessDeniedHandler;
import com.mahendra.bizcart_backend.authentication.security.RestAuthenticationEntryPoint;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SecurityTestController.class)
@Import({
		SecurityConfig.class,
		JwtAuthenticationFilter.class,
		JwtTokenProvider.class,
		RestAuthenticationEntryPoint.class,
		RestAccessDeniedHandler.class,
		SecurityConfigTest.TestSecurityBeans.class
})
@EnableConfigurationProperties(AuthenticationProperties.class)
@TestPropertySource(properties = {
		"bizcart.auth.jwt.secret=0123456789abcdef0123456789abcdef",
		"bizcart.auth.jwt.access-token-expiry-seconds=900",
		"bizcart.auth.cors.allowed-origins=http://localhost:3000",
		"bizcart.auth.cors.allowed-methods=GET,POST,OPTIONS",
		"bizcart.auth.cors.allowed-headers=Authorization,Content-Type,X-CSRF-TOKEN",
		"bizcart.auth.cors.allow-credentials=true"
})
class SecurityConfigTest {

	private static final String BEARER_PREFIX = "Bearer ";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Test
	void publicAuthEndpointDoesNotRequireToken() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login"))
			.andExpect(status().isOk());
	}

	@Test
	void protectedAuthEndpointRequiresToken() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedAuthEndpointAcceptsValidToken() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token("CUSTOMER")))
			.andExpect(status().isOk());
	}

	@Test
	void adminEndpointRequiresAdminRole() throws Exception {
		mockMvc.perform(get("/api/v1/admin/dashboard")
				.header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token("CUSTOMER")))
			.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/admin/dashboard")
				.header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token("ADMIN")))
			.andExpect(status().isOk());
	}

	@Test
	void corsAllowsConfiguredOrigin() throws Exception {
		mockMvc.perform(options("/api/v1/auth/login")
				.header(HttpHeaders.ORIGIN, "http://localhost:3000")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name()))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
	}

	@Test
	void statelessSecurityDoesNotCreateSessionForProtectedRequests() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token("CUSTOMER")))
			.andExpect(status().isOk())
			.andExpect(result -> assertThat(result.getRequest().getSession(false)).isNull());
	}

	private String token(String role) {
		User user = new User();
		ReflectionTestUtils.setField(user, "id", 101L);
		user.setFirstName("Security");
		user.setLastName("User");
		user.setEmail("security@example.com");
		user.setPassword("$2a$12$encodedPasswordPlaceholder");
		user.setUserType(UserType.CUSTOMER);
		user.setStatus(AccountStatus.ACTIVE);
		user.setEmailVerified(true);
		user.setTokenVersion(1L);
		return jwtTokenProvider.generateAccessToken(user, List.of(role));
	}

	static class TestSecurityBeans {

		@Bean
		Clock clock() {
			return Clock.fixed(Instant.parse("2026-07-11T00:00:00Z"), ZoneOffset.UTC);
		}

		@Bean
		PasswordEncoder passwordEncoder() {
			return NoOpPasswordEncoder.getInstance();
		}
	}

}
