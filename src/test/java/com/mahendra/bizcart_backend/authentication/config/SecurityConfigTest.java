package com.mahendra.bizcart_backend.authentication.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.mahendra.bizcart_backend.authentication.security.AuthenticatedUserDetails;
import com.mahendra.bizcart_backend.authentication.security.CustomUserDetailsService;
import com.mahendra.bizcart_backend.authentication.security.JwtAuthenticationFilter;
import com.mahendra.bizcart_backend.authentication.security.JwtTokenProvider;
import com.mahendra.bizcart_backend.authentication.security.RestAccessDeniedHandler;
import com.mahendra.bizcart_backend.authentication.security.RestAuthenticationEntryPoint;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.user.controller.UserProfileController;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import com.mahendra.bizcart_backend.user.service.UserProfileService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = { SecurityTestController.class, UserProfileController.class })
@Import({
		SecurityConfig.class,
		JwtAuthenticationFilter.class,
		JwtTokenProvider.class,
		RestAuthenticationEntryPoint.class,
		RestAccessDeniedHandler.class,
		SecurityConfigTest.TestSecurityBeans.class
})
@EnableConfigurationProperties(AuthenticationProperties.class)
@ActiveProfiles("security-test")
@TestPropertySource(properties = {
		"bizcart.auth.jwt.secret=0123456789abcdef0123456789abcdef",
		"bizcart.auth.token-hash.secret=test-token-hash-0123456789abcdef0123456789abcdef",
		"bizcart.auth.jwt.access-token-expiry-seconds=900",
		"bizcart.auth.cors.allowed-origins=http://localhost:3000",
		"bizcart.auth.cors.allowed-methods=GET,POST,PATCH,OPTIONS",
		"bizcart.auth.cors.allowed-headers=Authorization,Content-Type,X-XSRF-TOKEN",
		"bizcart.auth.cors.allow-credentials=true"
})
class SecurityConfigTest {

	private static final String BEARER_PREFIX = "Bearer ";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private TestUserDetailsService testUserDetailsService;

	@MockitoBean
	private UserProfileService userProfileService;

	@BeforeEach
	void setUp() {
		testUserDetailsService.setCurrentUser(user(101L, "security@example.com", UserType.CUSTOMER, AccountStatus.ACTIVE,
				true, 1L), List.of("ROLE_CUSTOMER", "ORDER_READ"));
	}

	@Test
	void publicAuthEndpointDoesNotRequireToken() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login").with(csrf()))
			.andExpect(status().isOk());
	}

	@Test
	void refreshTokenEndpointDoesNotRequireAccessToken() throws Exception {
		mockMvc.perform(post("/api/v1/auth/refresh-token").with(csrf()))
			.andExpect(status().isOk());
	}

	@Test
	void csrfEndpointDoesNotRequireAccessToken() throws Exception {
		mockMvc.perform(get("/api/v1/auth/csrf"))
			.andExpect(status().isOk());
	}

	@Test
	void configuredCorsHeadersMatchCsrfHeader() throws Exception {
		mockMvc.perform(options("/api/v1/auth/login")
				.header(HttpHeaders.ORIGIN, "http://localhost:3000")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, AppConstants.Auth.CSRF_HEADER))
			.andExpect(status().isOk())
			.andExpect(result -> assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS))
				.contains(AppConstants.Auth.CSRF_HEADER));
	}

	@Test
	void cookieAuthenticatedEndpointsRequireCsrf() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login"))
			.andExpect(status().isForbidden());
	}

	@Test
	void protectedAuthEndpointRequiresToken() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void userProfileViewRequiresAuthenticationThroughSecurityFilterChain() throws Exception {
		mockMvc.perform(get(AppConstants.User.API_USERS_BASE + AppConstants.User.PROFILE_PATH))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void userProfileUpdateRequiresAuthenticationThroughSecurityFilterChain() throws Exception {
		mockMvc.perform(patch(AppConstants.User.API_USERS_BASE + AppConstants.User.PROFILE_PATH).with(csrf())
				.contentType("application/json")
				.content("""
						{
						  "firstName": "Security",
						  "lastName": "Test"
						}
						"""))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void userProfileViewAcceptsValidJwtThroughSecurityFilterChain() throws Exception {
		mockMvc.perform(get(AppConstants.User.API_USERS_BASE + AppConstants.User.PROFILE_PATH)
				.header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token("CUSTOMER")))
			.andExpect(status().isOk());
	}

	@Test
	void userProfileUpdateAcceptsValidJwtAndCsrfThroughSecurityFilterChain() throws Exception {
		mockMvc.perform(patch(AppConstants.User.API_USERS_BASE + AppConstants.User.PROFILE_PATH).with(csrf())
				.header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token("CUSTOMER"))
				.contentType("application/json")
				.content("""
						{
						  "firstName": "Security"
						}
						"""))
			.andExpect(status().isOk());
	}

	@Test
	void userProfileUpdateRequiresCsrfWithValidJwt() throws Exception {
		mockMvc.perform(patch(AppConstants.User.API_USERS_BASE + AppConstants.User.PROFILE_PATH)
				.header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token("CUSTOMER"))
				.contentType("application/json")
				.content("""
						{
						  "firstName": "Security"
						}
						"""))
			.andExpect(status().isForbidden());
	}

	@Test
	void protectedAuthEndpointAcceptsValidToken() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token("CUSTOMER")))
			.andExpect(status().isOk());
	}

	@Test
	void protectedEndpointRejectsStaleTokenVersion() throws Exception {
		testUserDetailsService.setCurrentUser(user(101L, "security@example.com", UserType.CUSTOMER, AccountStatus.ACTIVE,
				true, 2L), List.of("ROLE_CUSTOMER"));

		mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token("CUSTOMER")))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedEndpointRejectsInactivePendingBlockedOrUnverifiedUsers() throws Exception {
		assertProtectedEndpointRejects(AccountStatus.INACTIVE, true);
		assertProtectedEndpointRejects(AccountStatus.PENDING, true);
		assertProtectedEndpointRejects(AccountStatus.BLOCKED, true);
		assertProtectedEndpointRejects(AccountStatus.ACTIVE, false);
	}

	@Test
	void protectedEndpointRejectsUnapprovedSellerAtJwtFilter() throws Exception {
		testUserDetailsService.setCurrentUser(user(101L, "security@example.com", UserType.SELLER, AccountStatus.ACTIVE,
				true, 1L), List.of("ROLE_SELLER"));

		mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token("SELLER")))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void adminEndpointRequiresAdminRole() throws Exception {
		mockMvc.perform(get("/api/v1/admin/dashboard")
				.header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token("CUSTOMER")))
			.andExpect(status().isForbidden());

		testUserDetailsService.setCurrentUser(user(101L, "security@example.com", UserType.CUSTOMER, AccountStatus.ACTIVE,
				true, 1L), List.of("ROLE_ADMIN"));
		mockMvc.perform(get("/api/v1/admin/dashboard")
				.header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token("ADMIN")))
			.andExpect(status().isOk());
	}

	@Test
	void permissionEndpointUsesCurrentDatabaseAuthorities() throws Exception {
		mockMvc.perform(get("/api/v1/customer/orders")
				.header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token("CUSTOMER")))
			.andExpect(status().isOk());

		testUserDetailsService.setCurrentUser(user(101L, "security@example.com", UserType.CUSTOMER, AccountStatus.ACTIVE,
				true, 1L), List.of("ROLE_CUSTOMER"));

		mockMvc.perform(get("/api/v1/customer/orders")
				.header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token("CUSTOMER")))
			.andExpect(status().isForbidden());
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

	private void assertProtectedEndpointRejects(AccountStatus accountStatus, boolean emailVerified) throws Exception {
		testUserDetailsService.setCurrentUser(user(101L, "security@example.com", UserType.CUSTOMER, accountStatus,
				emailVerified, 1L), List.of("ROLE_CUSTOMER"));

		mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token("CUSTOMER")))
			.andExpect(status().isUnauthorized());
	}

	private String token(String role) {
		return jwtTokenProvider.generateAccessToken(
				user(101L, "security@example.com", UserType.CUSTOMER, AccountStatus.ACTIVE, true, 1L), List.of(role));
	}

	private User user(Long id, String email, UserType userType, AccountStatus status, boolean emailVerified,
			long tokenVersion) {
		User user = new User();
		ReflectionTestUtils.setField(user, "id", id);
		user.setFirstName("Security");
		user.setLastName("User");
		user.setEmail(email);
		user.setPassword("$2a$12$encodedPasswordPlaceholder");
		user.setUserType(userType);
		user.setStatus(status);
		user.setEmailVerified(emailVerified);
		user.setTokenVersion(tokenVersion);
		return user;
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

		@Bean
		TestUserDetailsService testUserDetailsService() {
			return new TestUserDetailsService();
		}
	}

	static class TestUserDetailsService extends CustomUserDetailsService {

		private AuthenticatedUserDetails currentUser;

		TestUserDetailsService() {
			super(null, null, null);
		}

		void setCurrentUser(User user, Collection<String> authorities) {
			this.currentUser = new AuthenticatedUserDetails(user,
					authorities.stream().map(SimpleGrantedAuthority::new).toList());
		}

		@Override
		public UserDetails loadUserByUsername(String email) {
			if (currentUser != null && currentUser.getUsername().equals(email)) {
				return currentUser;
			}
			throw new UsernameNotFoundException("User not found");
		}
	}

}
