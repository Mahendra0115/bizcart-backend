package com.mahendra.bizcart_backend.authentication.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mahendra.bizcart_backend.authentication.dto.request.ForgotPasswordRequestDto;
import com.mahendra.bizcart_backend.authentication.notification.PasswordResetNotificationService;
import com.mahendra.bizcart_backend.authentication.repository.LoginAttemptRepository;
import com.mahendra.bizcart_backend.authentication.repository.PasswordResetTokenRepository;
import com.mahendra.bizcart_backend.authentication.repository.RefreshTokenRepository;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import com.mahendra.bizcart_backend.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthPasswordResetControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private LoginAttemptRepository loginAttemptRepository;

	@Autowired
	private PasswordResetTokenRepository passwordResetTokenRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockBean
	private PasswordResetNotificationService passwordResetNotificationService;

	@BeforeEach
	@AfterEach
	void cleanDatabase() {
		loginAttemptRepository.deleteAll();
		passwordResetTokenRepository.deleteAll();
		refreshTokenRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void forgotPasswordUnknownEmailReturnsGenericSuccess() throws Exception {
		mockMvc.perform(post(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.FORGOT_PASSWORD_PATH)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(forgotPasswordRequest("unknown@example.com"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value(AppConstants.Auth.FORGOT_PASSWORD_SUCCESS))
			.andExpect(jsonPath("$.data.message").value(AppConstants.Auth.FORGOT_PASSWORD_SUCCESS));

		assertThat(passwordResetTokenRepository.findAll()).isEmpty();
		verify(passwordResetNotificationService, never()).sendPasswordResetToken(any(), any(), any());
	}

	@Test
	void forgotPasswordExistingEmailReturnsGenericSuccessAndSendsAfterTokenCommit() throws Exception {
		userRepository.save(activeVerifiedUser("reset-existing@example.com", "reset-existing"));
		doAnswer(invocation -> {
			assertThat(passwordResetTokenRepository.count()).isEqualTo(1);
			return null;
		}).when(passwordResetNotificationService).sendPasswordResetToken(any(), any(), any());

		mockMvc.perform(post(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.FORGOT_PASSWORD_PATH)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(forgotPasswordRequest("reset-existing@example.com"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value(AppConstants.Auth.FORGOT_PASSWORD_SUCCESS))
			.andExpect(jsonPath("$.data.message").value(AppConstants.Auth.FORGOT_PASSWORD_SUCCESS));

		assertThat(passwordResetTokenRepository.findAll()).hasSize(1);
		verify(passwordResetNotificationService).sendPasswordResetToken(any(), any(), any());
	}

	@Test
	void forgotPasswordSmtpFailureReturnsGenericSuccessAndKeepsToken() throws Exception {
		userRepository.save(activeVerifiedUser("reset-smtp-failure@example.com", "reset-smtp-failure"));
		doThrow(new IllegalStateException("SMTP unavailable")).when(passwordResetNotificationService)
			.sendPasswordResetToken(any(), any(), any());

		mockMvc.perform(post(AppConstants.Auth.API_AUTH_BASE + AppConstants.Auth.FORGOT_PASSWORD_PATH)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(forgotPasswordRequest("reset-smtp-failure@example.com"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value(AppConstants.Auth.FORGOT_PASSWORD_SUCCESS))
			.andExpect(jsonPath("$.data.message").value(AppConstants.Auth.FORGOT_PASSWORD_SUCCESS));

		assertThat(passwordResetTokenRepository.findAll()).hasSize(1);
	}

	private ForgotPasswordRequestDto forgotPasswordRequest(String email) {
		ForgotPasswordRequestDto request = new ForgotPasswordRequestDto();
		request.setEmail(email);
		return request;
	}

	private User activeVerifiedUser(String email, String username) {
		User user = new User();
		user.setFirstName("Reset");
		user.setLastName("User");
		user.setUsername(username);
		user.setEmail(email);
		user.setPassword(passwordEncoder.encode("Password@123"));
		user.setUserType(UserType.CUSTOMER);
		user.setStatus(AccountStatus.ACTIVE);
		user.setEmailVerified(true);
		user.setAdminApproved(false);
		user.setTokenVersion(1L);
		return user;
	}
}
