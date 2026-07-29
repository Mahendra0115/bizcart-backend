package com.mahendra.bizcart_backend.user.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mahendra.bizcart_backend.authentication.security.AuthenticatedUserDetails;
import com.mahendra.bizcart_backend.authentication.security.CustomUserDetailsService;
import com.mahendra.bizcart_backend.authentication.security.JwtTokenProvider;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.user.dto.request.UpdateProfileRequestDto;
import com.mahendra.bizcart_backend.user.dto.response.UserProfileResponseDto;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import com.mahendra.bizcart_backend.user.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserProfileControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserProfileService userProfileService;

	@MockBean
	private JwtTokenProvider jwtTokenProvider;

	@MockBean
	private CustomUserDetailsService customUserDetailsService;

	@BeforeEach
	void authenticate() {
		AuthenticatedUserDetails principal = principal();
		SecurityContextHolder.getContext()
			.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
	}

	@AfterEach
	void clearAuthentication() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void getProfileReturnsAuthenticatedUsersProfile() throws Exception {
		when(userProfileService.getProfile(1L)).thenReturn(profile());

		mockMvc.perform(get(AppConstants.User.API_USERS_BASE + AppConstants.User.PROFILE_PATH))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value(AppConstants.User.PROFILE_FETCH_SUCCESS))
			.andExpect(jsonPath("$.data.id").value(1))
			.andExpect(jsonPath("$.data.password").doesNotExist())
			.andExpect(jsonPath("$.data.tokenVersion").doesNotExist());
	}

	@Test
	void updateProfileValidatesAndReturnsUpdatedProfile() throws Exception {
		UpdateProfileRequestDto request = new UpdateProfileRequestDto("Updated", "User", "+919876543210",
				"https://img.test/me.png");
		when(userProfileService.updateProfile(1L, request)).thenReturn(profile());

		mockMvc.perform(put(AppConstants.User.API_USERS_BASE + AppConstants.User.PROFILE_PATH)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "firstName": "Updated",
								  "lastName": "User",
								  "phone": "+919876543210",
								  "profileImage": "https://img.test/me.png"
								}
								"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value(AppConstants.User.PROFILE_UPDATE_SUCCESS))
			.andExpect(jsonPath("$.data.email").value("user@example.com"));
	}

	@Test
	void updateProfileRejectsRestrictedFieldsByIgnoringThem() throws Exception {
		UpdateProfileRequestDto request = new UpdateProfileRequestDto("Updated", "User", null, null);
		when(userProfileService.updateProfile(1L, request)).thenReturn(profile());

		mockMvc.perform(put(AppConstants.User.API_USERS_BASE + AppConstants.User.PROFILE_PATH)
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "firstName": "Updated",
								  "lastName": "User",
								  "email": "attacker@example.com",
								  "status": "BLOCKED"
								}
								"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.email").value("user@example.com"))
			.andExpect(jsonPath("$.data.status").value("ACTIVE"));
	}

	private AuthenticatedUserDetails principal() {
		com.mahendra.bizcart_backend.user.entity.User user = new com.mahendra.bizcart_backend.user.entity.User();
		ReflectionTestUtils.setField(user, "id", 1L);
		user.setEmail("user@example.com");
		user.setPassword("encoded");
		user.setUserType(UserType.CUSTOMER);
		user.setStatus(AccountStatus.ACTIVE);
		user.setEmailVerified(true);
		return new AuthenticatedUserDetails(user, java.util.List.of());
	}

	private UserProfileResponseDto profile() {
		return new UserProfileResponseDto(1L, "Updated", "User", "bizcart-user", "user@example.com",
				"+919876543210", "https://img.test/me.png", UserType.CUSTOMER, AccountStatus.ACTIVE, true, false);
	}
}
