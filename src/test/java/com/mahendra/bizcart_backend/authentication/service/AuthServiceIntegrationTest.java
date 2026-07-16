package com.mahendra.bizcart_backend.authentication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mahendra.bizcart_backend.authentication.dto.request.LoginRequestDto;
import com.mahendra.bizcart_backend.authentication.entity.LoginAttempt;
import com.mahendra.bizcart_backend.authentication.repository.LoginAttemptRepository;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import com.mahendra.bizcart_backend.user.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
class AuthServiceIntegrationTest {

	private static final String EMAIL = "failed-login@example.com";

	@Autowired
	private AuthService authService;

	@Autowired
	private LoginAttemptRepository loginAttemptRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	@AfterEach
	void cleanDatabase() {
		loginAttemptRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void failedLoginAttemptIsCommittedWhenLoginTransactionRollsBack() {
		userRepository.save(activeVerifiedUser());
		LocalDateTime from = LocalDateTime.now(Clock.systemUTC()).minusMinutes(1);

		assertThatThrownBy(() -> authService.login(loginRequest(), "127.0.0.1", "JUnit"))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining(AppConstants.Auth.INVALID_CREDENTIALS);

		List<LoginAttempt> attempts = loginAttemptRepository.findByEmailAndAttemptedAtBetween(EMAIL, from,
				LocalDateTime.now(Clock.systemUTC()).plusMinutes(1));
		assertThat(attempts).hasSize(1);
		assertThat(attempts.getFirst().isWasSuccessful()).isFalse();
		assertThat(attempts.getFirst().getFailureReason()).isEqualTo(AppConstants.Auth.INVALID_CREDENTIALS);
	}

	private User activeVerifiedUser() {
		User user = new User();
		user.setFirstName("Failed");
		user.setLastName("Login");
		user.setUsername("failed-login");
		user.setEmail(EMAIL);
		user.setPassword(passwordEncoder.encode("Password@123"));
		user.setUserType(UserType.CUSTOMER);
		user.setStatus(AccountStatus.ACTIVE);
		user.setEmailVerified(true);
		user.setAdminApproved(false);
		user.setTokenVersion(1L);
		return user;
	}

	private LoginRequestDto loginRequest() {
		LoginRequestDto request = new LoginRequestDto();
		request.setEmail(EMAIL);
		request.setPassword("WrongPassword@123");
		return request;
	}
}
