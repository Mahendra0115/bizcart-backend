package com.mahendra.bizcart_backend.authentication.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mahendra.bizcart_backend.authentication.config.AuthenticationProperties;
import com.mahendra.bizcart_backend.authentication.dto.response.TokenResponseDto;
import com.mahendra.bizcart_backend.authentication.security.AuthenticatedUserDetails;
import com.mahendra.bizcart_backend.authentication.service.AuthService;
import com.mahendra.bizcart_backend.authentication.service.RefreshTokenResult;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

class AuthControllerTest {

	private FakeAuthService authService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		authService = new FakeAuthService();
		mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, authenticationProperties()))
			.setCustomArgumentResolvers(new TestAuthenticationPrincipalResolver())
			.build();
	}

	@Test
	void refreshAccessTokenReadsRefreshCookieAndSetsRotatedCookie() throws Exception {
		mockMvc.perform(post(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.REFRESH_TOKEN_PATH)
				.cookie(new jakarta.servlet.http.Cookie(AppConstants.Auth.REFRESH_TOKEN_COOKIE, "raw-refresh"))
				.header("X-Forwarded-For", "203.0.113.10")
				.header("User-Agent", "JUnit"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value(AppConstants.Auth.REFRESH_TOKEN_SUCCESS))
			.andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
			.andExpect(jsonPath("$.data.refreshToken").doesNotExist())
			.andExpect(header().string(HttpHeaders.SET_COOKIE,
					org.hamcrest.Matchers.containsString(AppConstants.Auth.REFRESH_TOKEN_COOKIE + "=new-refresh")));

		org.assertj.core.api.Assertions.assertThat(authService.refreshToken).isEqualTo("raw-refresh");
		org.assertj.core.api.Assertions.assertThat(authService.ipAddress).isEqualTo("203.0.113.10");
		org.assertj.core.api.Assertions.assertThat(authService.userAgent).isEqualTo("JUnit");
	}

	@Test
	void logoutRevokesCurrentDeviceAndClearsCookie() throws Exception {
		mockMvc.perform(post(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.LOGOUT_PATH)
				.cookie(new jakarta.servlet.http.Cookie(AppConstants.Auth.REFRESH_TOKEN_COOKIE, "raw-refresh")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value(AppConstants.Auth.LOGOUT_SUCCESS))
			.andExpect(header().string(HttpHeaders.SET_COOKIE,
					org.hamcrest.Matchers.containsString(AppConstants.Auth.REFRESH_TOKEN_COOKIE + "=")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));

		org.assertj.core.api.Assertions.assertThat(authService.logoutToken).isEqualTo("raw-refresh");
	}

	@Test
	void logoutAllRevokesUserTokensAndClearsCookie() throws Exception {
		AuthenticatedUserDetails principal = new AuthenticatedUserDetails(user(), List.of());
		SecurityContextHolder.getContext()
			.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
		try {
			mockMvc.perform(post(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.LOGOUT_ALL_PATH))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value(AppConstants.Auth.LOGOUT_ALL_SUCCESS))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));
		}
		finally {
			SecurityContextHolder.clearContext();
		}

		org.assertj.core.api.Assertions.assertThat(authService.logoutAllUserId).isEqualTo(7L);
	}

	private AuthenticationProperties authenticationProperties() {
		AuthenticationProperties authenticationProperties = new AuthenticationProperties();
		authenticationProperties.getJwt().setRefreshTokenExpirySeconds(604800);
		authenticationProperties.getRefreshCookie().setSecure(false);
		return authenticationProperties;
	}

	private User user() {
		User user = new User();
		ReflectionTestUtils.setField(user, "id", 7L);
		user.setFirstName("Test");
		user.setLastName("User");
		user.setEmail("customer@example.com");
		user.setPassword("hashed-password");
		user.setUserType(UserType.CUSTOMER);
		user.setStatus(AccountStatus.ACTIVE);
		user.setEmailVerified(true);
		user.setTokenVersion(1L);
		return user;
	}

	private static class FakeAuthService extends AuthService {

		private String refreshToken;
		private String ipAddress;
		private String userAgent;
		private String logoutToken;
		private Long logoutAllUserId;

		FakeAuthService() {
			super(null, null, null, null, null);
		}

		@Override
		public RefreshTokenResult refreshAccessToken(String rawRefreshToken, String ipAddress, String userAgent) {
			this.refreshToken = rawRefreshToken;
			this.ipAddress = ipAddress;
			this.userAgent = userAgent;
			return new RefreshTokenResult(new TokenResponseDto("new-access-token", 900, 604800), "new-refresh");
		}

		@Override
		public void logout(String rawRefreshToken) {
			this.logoutToken = rawRefreshToken;
		}

		@Override
		public void logoutAll(Long userId) {
			this.logoutAllUserId = userId;
		}
	}

	private static class TestAuthenticationPrincipalResolver implements HandlerMethodArgumentResolver {

		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
		}

		@Override
		public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
				NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
			return SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		}
	}
}
