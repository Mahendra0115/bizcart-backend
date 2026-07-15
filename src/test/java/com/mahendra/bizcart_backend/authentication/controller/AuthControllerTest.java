package com.mahendra.bizcart_backend.authentication.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mahendra.bizcart_backend.authentication.dto.request.LoginRequestDto;
import com.mahendra.bizcart_backend.authentication.dto.request.RegisterRequestDto;
import com.mahendra.bizcart_backend.authentication.dto.response.CurrentUserResponseDto;
import com.mahendra.bizcart_backend.authentication.dto.response.LoginResponseDto;
import com.mahendra.bizcart_backend.authentication.dto.response.RegisterResponseDto;
import com.mahendra.bizcart_backend.authentication.config.AuthenticationProperties;
import com.mahendra.bizcart_backend.authentication.security.AuthenticatedUserDetails;
import com.mahendra.bizcart_backend.authentication.security.CustomUserDetailsService;
import com.mahendra.bizcart_backend.authentication.security.JwtTokenProvider;
import com.mahendra.bizcart_backend.authentication.service.LoginResult;
import com.mahendra.bizcart_backend.authentication.service.AuthService;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

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
