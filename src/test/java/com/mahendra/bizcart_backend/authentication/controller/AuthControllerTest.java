package com.mahendra.bizcart_backend.authentication.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mahendra.bizcart_backend.authentication.dto.request.ForgotPasswordRequestDto;
import com.mahendra.bizcart_backend.authentication.dto.request.LoginRequestDto;
import com.mahendra.bizcart_backend.authentication.dto.request.RegisterRequestDto;
import com.mahendra.bizcart_backend.authentication.dto.request.ResetPasswordRequestDto;
import com.mahendra.bizcart_backend.authentication.dto.request.ChangePasswordRequestDto;
import com.mahendra.bizcart_backend.authentication.dto.response.CurrentUserResponseDto;
import com.mahendra.bizcart_backend.authentication.dto.response.LoginResponseDto;
import com.mahendra.bizcart_backend.authentication.dto.response.RegisterResponseDto;
import com.mahendra.bizcart_backend.authentication.dto.response.TokenResponseDto;
import com.mahendra.bizcart_backend.authentication.config.AuthenticationProperties;
import com.mahendra.bizcart_backend.authentication.security.AuthenticatedUserDetails;
import com.mahendra.bizcart_backend.authentication.security.CustomUserDetailsService;
import com.mahendra.bizcart_backend.authentication.security.JwtTokenProvider;
import com.mahendra.bizcart_backend.authentication.service.LoginResult;
import com.mahendra.bizcart_backend.authentication.service.AuthService;
import com.mahendra.bizcart_backend.authentication.service.RefreshTokenResult;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AuthControllerTest.TestBeans.class)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private AuthService authService;

	@MockBean
	private JwtTokenProvider jwtTokenProvider;

	@MockBean
	private CustomUserDetailsService customUserDetailsService;

	@Test
	void registerReturnsWrappedRegisterResponse() throws Exception {
		when(authService.register(any(RegisterRequestDto.class))).thenReturn(new RegisterResponseDto(44L, "Customer",
				"One", "customer-one", "customer@example.com", "+919999999999", UserType.CUSTOMER,
				AccountStatus.PENDING, false));

		mockMvc.perform(post(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.REGISTER_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(registerRequest())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value(AppConstants.Auth.REGISTER_SUCCESS))
			.andExpect(jsonPath("$.data.username").value("customer-one"))
			.andExpect(jsonPath("$.data.email").value("customer@example.com"))
			.andExpect(jsonPath("$.data.password").doesNotExist());
	}

	@Test
	void registerMapsDuplicateEmailDatabaseRaceToConflictResponse() throws Exception {
		when(authService.register(any(RegisterRequestDto.class)))
			.thenThrow(new DataIntegrityViolationException("Duplicate entry for key 'ux_users_email'"));

		mockMvc.perform(post(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.REGISTER_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(registerRequest())))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value(AppConstants.Auth.AUTH_EMAIL_ALREADY_EXISTS))
			.andExpect(jsonPath("$.message").value(AppConstants.Auth.DUPLICATE_EMAIL))
			.andExpect(jsonPath("$.path").value(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.REGISTER_PATH));
	}

	@Test
	void registerMapsDuplicateUsernameDatabaseRaceToConflictResponse() throws Exception {
		when(authService.register(any(RegisterRequestDto.class)))
			.thenThrow(new DataIntegrityViolationException("Duplicate entry for key 'ux_users_username'"));

		mockMvc.perform(post(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.REGISTER_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(registerRequest())))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value(AppConstants.Auth.AUTH_USERNAME_ALREADY_EXISTS))
			.andExpect(jsonPath("$.message").value(AppConstants.Auth.DUPLICATE_USERNAME));
	}

	@Test
	void validationErrorsUseStandardErrorResponse() throws Exception {
		RegisterRequestDto request = registerRequest();
		request.setEmail("not-an-email");

		mockMvc.perform(post(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.REGISTER_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(AppConstants.Auth.AUTH_VALIDATION_FAILED))
			.andExpect(jsonPath("$.message").value(AppConstants.Auth.VALIDATION_FAILED))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
	}

	@Test
	void loginReturnsAccessAndRefreshTokens() throws Exception {
		CurrentUserResponseDto user = currentUserResponse();
		when(authService.login(any(LoginRequestDto.class), eq("203.0.113.10"), eq("JUnit")))
			.thenReturn(new LoginResult(new LoginResponseDto("access-token", 900, 604800, user), "refresh-token"));

		mockMvc.perform(post(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.LOGIN_PATH)
				.header("X-Forwarded-For", "203.0.113.10")
				.header("User-Agent", "JUnit")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value(AppConstants.Auth.LOGIN_SUCCESS))
			.andExpect(jsonPath("$.data.accessToken").value("access-token"))
			.andExpect(jsonPath("$.data.refreshToken").doesNotExist())
			.andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse()
				.getHeader(HttpHeaders.SET_COOKIE)).contains("BIZCART_REFRESH_TOKEN=refresh-token"))
			.andExpect(jsonPath("$.data.user.password").doesNotExist());
	}

	@Test
	void currentUserUsesAuthenticatedPrincipalId() throws Exception {
		when(authService.currentUser(55L)).thenReturn(currentUserResponse());
		AuthenticatedUserDetails principal = new AuthenticatedUserDetails(user(), List.of());
		SecurityContextHolder.getContext()
			.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

		try {
			mockMvc.perform(get(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.CURRENT_USER_PATH))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value(AppConstants.Auth.CURRENT_USER_SUCCESS))
				.andExpect(jsonPath("$.data.username").value("customer-one"));
		}
		finally {
			SecurityContextHolder.clearContext();
		}

		verify(authService).currentUser(55L);
	}

	@Test
	void refreshAccessTokenRotatesRefreshCookie() throws Exception {
		when(authService.refreshAccessToken(eq("raw-refresh"), eq("203.0.113.10"), eq("JUnit")))
			.thenReturn(new RefreshTokenResult(new TokenResponseDto("new-access-token", 900, 604800), "new-refresh"));

		mockMvc.perform(post(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.REFRESH_TOKEN_PATH)
				.cookie(new Cookie(AppConstants.Auth.REFRESH_TOKEN_COOKIE, "raw-refresh"))
				.header("X-Forwarded-For", "203.0.113.10")
				.header("User-Agent", "JUnit"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value(AppConstants.Auth.REFRESH_TOKEN_SUCCESS))
			.andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
			.andExpect(jsonPath("$.data.refreshToken").doesNotExist())
			.andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse()
				.getHeader(HttpHeaders.SET_COOKIE)).contains("BIZCART_REFRESH_TOKEN=new-refresh"));
	}

	@Test
	void refreshAccessTokenExpiredUsesSpecificErrorCode() throws Exception {
		when(authService.refreshAccessToken(eq("expired-refresh"), any(), any()))
			.thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, AppConstants.Auth.REFRESH_TOKEN_EXPIRED));

		mockMvc.perform(post(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.REFRESH_TOKEN_PATH)
				.cookie(new Cookie(AppConstants.Auth.REFRESH_TOKEN_COOKIE, "expired-refresh")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(AppConstants.Auth.AUTH_REFRESH_TOKEN_EXPIRED));
	}

	@Test
	void refreshAccessTokenRevokedUsesSpecificErrorCode() throws Exception {
		when(authService.refreshAccessToken(eq("revoked-refresh"), any(), any()))
			.thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, AppConstants.Auth.REFRESH_TOKEN_REVOKED));

		mockMvc.perform(post(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.REFRESH_TOKEN_PATH)
				.cookie(new Cookie(AppConstants.Auth.REFRESH_TOKEN_COOKIE, "revoked-refresh")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(AppConstants.Auth.AUTH_REFRESH_TOKEN_REVOKED));
	}

	@Test
	void refreshAccessTokenInvalidUsesInvalidTokenErrorCode() throws Exception {
		when(authService.refreshAccessToken(eq("invalid-refresh"), any(), any()))
			.thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, AppConstants.Auth.INVALID_REFRESH_TOKEN));

		mockMvc.perform(post(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.REFRESH_TOKEN_PATH)
				.cookie(new Cookie(AppConstants.Auth.REFRESH_TOKEN_COOKIE, "invalid-refresh")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(AppConstants.Auth.AUTH_INVALID_TOKEN));
	}

	@Test
	void logoutRevokesRefreshTokenAndClearsCookie() throws Exception {
		AuthenticatedUserDetails principal = new AuthenticatedUserDetails(user(), List.of());
		SecurityContextHolder.getContext()
			.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

		try {
			mockMvc.perform(post(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.LOGOUT_PATH)
					.cookie(new Cookie(AppConstants.Auth.REFRESH_TOKEN_COOKIE, "raw-refresh")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value(AppConstants.Auth.LOGOUT_SUCCESS))
				.andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse()
					.getHeader(HttpHeaders.SET_COOKIE)).contains("BIZCART_REFRESH_TOKEN=").contains("Max-Age=0"));
		}
		finally {
			SecurityContextHolder.clearContext();
		}

		verify(authService).logout(55L, "raw-refresh");
	}

	@Test
	void logoutWithoutRefreshTokenStillClearsCookie() throws Exception {
		AuthenticatedUserDetails principal = new AuthenticatedUserDetails(user(), List.of());
		SecurityContextHolder.getContext()
			.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

		try {
			mockMvc.perform(post(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.LOGOUT_PATH))
				.andExpect(status().isOk())
				.andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse()
					.getHeader(HttpHeaders.SET_COOKIE)).contains("BIZCART_REFRESH_TOKEN=").contains("Max-Age=0"));
		}
		finally {
			SecurityContextHolder.clearContext();
		}

		verify(authService).logout(55L, null);
	}

	@Test
	void logoutAllRevokesAllUserRefreshTokensAndClearsCookie() throws Exception {
		AuthenticatedUserDetails principal = new AuthenticatedUserDetails(user(), List.of());
		SecurityContextHolder.getContext()
			.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

		try {
			mockMvc.perform(post(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.LOGOUT_ALL_PATH))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value(AppConstants.Auth.LOGOUT_ALL_SUCCESS))
				.andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse()
					.getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0"));
		}
		finally {
			SecurityContextHolder.clearContext();
		}

		verify(authService).logoutAll(55L);
	}

	@Test
	void forgotPasswordReturnsGenericSuccessMessage() throws Exception {
		mockMvc.perform(post(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.FORGOT_PASSWORD_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(forgotPasswordRequest())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value(AppConstants.Auth.FORGOT_PASSWORD_SUCCESS))
			.andExpect(jsonPath("$.data.message").value(AppConstants.Auth.FORGOT_PASSWORD_SUCCESS));

		verify(authService).forgotPassword(any(ForgotPasswordRequestDto.class));
	}

	@Test
	void resetPasswordReturnsSuccessMessage() throws Exception {
		mockMvc.perform(post(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.RESET_PASSWORD_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(resetPasswordRequest())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value(AppConstants.Auth.RESET_PASSWORD_SUCCESS))
			.andExpect(jsonPath("$.data.message").value(AppConstants.Auth.RESET_PASSWORD_SUCCESS));

		verify(authService).resetPassword(any(ResetPasswordRequestDto.class));
	}

	@Test
	void resetPasswordInvalidTokenUsesSpecificErrorCode() throws Exception {
		doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, AppConstants.Auth.INVALID_RESET_TOKEN))
			.when(authService)
			.resetPassword(any(ResetPasswordRequestDto.class));

		mockMvc.perform(post(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.RESET_PASSWORD_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(resetPasswordRequest())))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(AppConstants.Auth.AUTH_RESET_TOKEN_INVALID));
	}

	@Test
	void resetPasswordExpiredTokenUsesSpecificErrorCode() throws Exception {
		doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, AppConstants.Auth.RESET_TOKEN_EXPIRED))
			.when(authService)
			.resetPassword(any(ResetPasswordRequestDto.class));

		mockMvc.perform(post(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.RESET_PASSWORD_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(resetPasswordRequest())))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(AppConstants.Auth.AUTH_RESET_TOKEN_EXPIRED));
	}

	@Test
	void verifyEmailDelegatesTokenAndReturnsSuccess() throws Exception {
		mockMvc.perform(post(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.VERIFY_EMAIL_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"raw-verification-token\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value(AppConstants.Auth.VERIFY_EMAIL_SUCCESS));

		verify(authService).verifyEmail("raw-verification-token");
	}

	@Test
	void resendVerificationAlwaysReturnsGenericSuccess() throws Exception {
		mockMvc.perform(post(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.RESEND_VERIFICATION_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"unknown@example.com\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value(AppConstants.Auth.RESEND_VERIFICATION_SUCCESS));

		verify(authService).resendVerification("unknown@example.com");
	}

	@Test
	void changePasswordUsesAuthenticatedUser() throws Exception {
		AuthenticatedUserDetails principal = new AuthenticatedUserDetails(user(), List.of());
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
		try {
			mockMvc.perform(put(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.CHANGE_PASSWORD_PATH)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(changePasswordRequest())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value(AppConstants.Auth.CHANGE_PASSWORD_SUCCESS));
		}
		finally {
			SecurityContextHolder.clearContext();
		}

		verify(authService).changePassword(eq(55L), any(ChangePasswordRequestDto.class));
	}

	private RegisterRequestDto registerRequest() {
		RegisterRequestDto request = new RegisterRequestDto();
		request.setFirstName("Customer");
		request.setLastName("One");
		request.setUsername("customer-one");
		request.setEmail("customer@example.com");
		request.setPhone("+919999999999");
		request.setPassword("Password@123");
		request.setConfirmPassword("Password@123");
		request.setUserType(UserType.CUSTOMER);
		return request;
	}

	private LoginRequestDto loginRequest() {
		LoginRequestDto request = new LoginRequestDto();
		request.setEmail("customer@example.com");
		request.setPassword("Password@123");
		return request;
	}

	private ForgotPasswordRequestDto forgotPasswordRequest() {
		ForgotPasswordRequestDto request = new ForgotPasswordRequestDto();
		request.setEmail("customer@example.com");
		return request;
	}

	private ResetPasswordRequestDto resetPasswordRequest() {
		ResetPasswordRequestDto request = new ResetPasswordRequestDto();
		request.setToken("reset-token");
		request.setNewPassword("NewPassword@123");
		request.setConfirmPassword("NewPassword@123");
		return request;
	}

	private ChangePasswordRequestDto changePasswordRequest() {
		ChangePasswordRequestDto request = new ChangePasswordRequestDto();
		request.setCurrentPassword("Password@123");
		request.setNewPassword("NewPassword@123");
		request.setConfirmPassword("NewPassword@123");
		return request;
	}

	private CurrentUserResponseDto currentUserResponse() {
		return new CurrentUserResponseDto(55L, "Customer", "One", "customer-one", "customer@example.com",
				"+919999999999", null, UserType.CUSTOMER, AccountStatus.ACTIVE, true,
				List.of(AppConstants.RoleNames.CUSTOMER), List.of("ORDER_READ"));
	}

	private User user() {
		User user = new User();
		ReflectionTestUtils.setField(user, "id", 55L);
		user.setFirstName("Customer");
		user.setLastName("One");
		user.setUsername("customer-one");
		user.setEmail("customer@example.com");
		user.setPassword("hashed-password");
		user.setUserType(UserType.CUSTOMER);
		user.setStatus(AccountStatus.ACTIVE);
		user.setEmailVerified(true);
		user.setTokenVersion(1L);
		return user;
	}

	static class TestBeans {

		@Bean
		AuthenticationProperties authenticationProperties() {
			AuthenticationProperties authenticationProperties = new AuthenticationProperties();
			authenticationProperties.getJwt().setRefreshTokenExpirySeconds(604800);
			authenticationProperties.getRefreshCookie().setSecure(false);
			return authenticationProperties;
		}
	}
}
