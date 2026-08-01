package com.mahendra.bizcart_backend.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mahendra.bizcart_backend.authentication.security.AuthenticatedUserDetails;
import com.mahendra.bizcart_backend.authentication.security.CustomUserDetailsService;
import com.mahendra.bizcart_backend.authentication.security.JwtTokenProvider;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.user.dto.request.UpdateAccountStatusRequestDto;
import com.mahendra.bizcart_backend.user.dto.response.AccountStatusResponseDto;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import com.mahendra.bizcart_backend.user.service.AccountStatusService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(AccountStatusController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountStatusControllerTest {

	private static final String PATH = "/api/v1/admin/users/2/status";

	@Autowired MockMvc mockMvc;
	@MockitoBean AccountStatusService accountStatusService;
	@MockBean JwtTokenProvider jwtTokenProvider;
	@MockBean CustomUserDetailsService customUserDetailsService;

	@BeforeEach
	void authenticateAdmin() {
		User admin = new User();
		ReflectionTestUtils.setField(admin, "id", 1L);
		admin.setEmail("admin@example.com");
		admin.setPassword("encoded");
		admin.setUserType(UserType.CUSTOMER);
		admin.setStatus(AccountStatus.ACTIVE);
		admin.setEmailVerified(true);
		AuthenticatedUserDetails principal = new AuthenticatedUserDetails(admin,
				List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
	}

	@AfterEach
	void clearAuthentication() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void validAdminRequestReturnsSuccess() throws Exception {
		when(accountStatusService.updateStatus(eq(1L), eq(2L), any(UpdateAccountStatusRequestDto.class)))
			.thenReturn(new AccountStatusResponseDto(2L, "user@example.com", UserType.CUSTOMER,
					AccountStatus.ACTIVE, AccountStatus.INACTIVE, null));
		mockMvc.perform(patch(PATH).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"INACTIVE\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value(AppConstants.User.STATUS_UPDATE_SUCCESS));
	}

	@Test
	void rejectsUnknownNullAndInvalidStatusFields() throws Exception {
		assertBadRequest("{\"status\":\"INACTIVE\",\"adminApproved\":true}");
		assertBadRequest("{\"status\":null}");
		assertBadRequest("{\"status\":\"SUSPENDED\"}");
		verifyNoInteractions(accountStatusService);
	}

	@Test
	void rejectsReasonOverMaximumLength() throws Exception {
		assertBadRequest("{\"status\":\"BLOCKED\",\"reason\":\"" + "x".repeat(501) + "\"}");
		verifyNoInteractions(accountStatusService);
	}

	@Test
	void blankBlockReasonReturnsDedicatedBadRequest() throws Exception {
		when(accountStatusService.updateStatus(eq(1L), eq(2L), any(UpdateAccountStatusRequestDto.class)))
			.thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, AppConstants.User.BLOCK_REASON_REQUIRED));
		mockMvc.perform(patch(PATH).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"BLOCKED\",\"reason\":\"   \"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(AppConstants.User.BLOCK_REASON_REQUIRED_CODE));
	}

	private void assertBadRequest(String body) throws Exception {
		mockMvc.perform(patch(PATH).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest());
	}
}
