package com.mahendra.bizcart_backend.authentication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.mahendra.bizcart_backend.authentication.dto.request.ForgotPasswordRequestDto;
import com.mahendra.bizcart_backend.authentication.dto.request.LoginRequestDto;
import com.mahendra.bizcart_backend.authentication.dto.request.ResetPasswordRequestDto;
import com.mahendra.bizcart_backend.authentication.entity.LoginAttempt;
import com.mahendra.bizcart_backend.authentication.entity.PasswordResetToken;
import com.mahendra.bizcart_backend.authentication.entity.RefreshToken;
import com.mahendra.bizcart_backend.authentication.entity.RefreshTokenRevocationReason;
import com.mahendra.bizcart_backend.authentication.notification.PasswordResetNotificationService;
import com.mahendra.bizcart_backend.authentication.repository.LoginAttemptRepository;
import com.mahendra.bizcart_backend.authentication.repository.PasswordResetTokenRepository;
import com.mahendra.bizcart_backend.authentication.repository.RefreshTokenRepository;
import com.mahendra.bizcart_backend.authentication.config.AuthenticationProperties;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import com.mahendra.bizcart_backend.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.ArgumentCaptor;

@SpringBootTest
class AuthServiceIntegrationTest {

	private static final String EMAIL = "failed-login@example.com";

	@Autowired
	private AuthService authService;

	@Autowired
	private LoginAttemptRepository loginAttemptRepository;

	@Autowired
	private PasswordResetTokenRepository passwordResetTokenRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AuthenticationProperties authenticationProperties;

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

	@Test
	void reusedRevokedRefreshTokenRevokesFamilyEvenAfterUnauthorizedResponse() {
		User user = userRepository.save(activeVerifiedUser());
		String familyId = "family-reuse";
		refreshTokenRepository.save(refreshToken(user, "reused-refresh-token", familyId,
				LocalDateTime.now(Clock.systemUTC()).plusDays(1),
				LocalDateTime.now(Clock.systemUTC()).minusMinutes(1), RefreshTokenRevocationReason.ROTATED));
		RefreshToken activeSibling = refreshTokenRepository.save(refreshToken(user, "active-refresh-token", familyId,
				LocalDateTime.now(Clock.systemUTC()).plusDays(1), null, null));
		RefreshToken secondActiveSibling = refreshTokenRepository.save(refreshToken(user, "second-active-refresh-token",
				familyId, LocalDateTime.now(Clock.systemUTC()).plusDays(1), null, null));

		assertThatThrownBy(() -> authService.refreshAccessToken("reused-refresh-token", "127.0.0.1", "JUnit"))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining(AppConstants.Auth.REFRESH_TOKEN_REVOKED);

		RefreshToken refreshedSibling = refreshTokenRepository.findById(activeSibling.getId()).orElseThrow();
		assertThat(refreshedSibling.getRevokedAt()).isNotNull();
		assertThat(refreshedSibling.getRevocationReason())
			.isEqualTo(RefreshTokenRevocationReason.TOKEN_REUSE_DETECTED);
		RefreshToken secondRefreshedSibling = refreshTokenRepository.findById(secondActiveSibling.getId()).orElseThrow();
		assertThat(secondRefreshedSibling.getRevokedAt()).isNotNull();
		assertThat(secondRefreshedSibling.getRevocationReason())
			.isEqualTo(RefreshTokenRevocationReason.TOKEN_REUSE_DETECTED);
		assertThat(refreshTokenRepository.findActiveByTokenFamilyId(familyId, LocalDateTime.now(Clock.systemUTC())))
			.isEmpty();
	}

	@Test
	void onlyOneSimultaneousRefreshRequestSucceeds() throws Exception {
		User user = userRepository.save(activeVerifiedUser());
		String familyId = "family-concurrent";
		refreshTokenRepository.save(refreshToken(user, "concurrent-refresh-token", familyId,
				LocalDateTime.now(Clock.systemUTC()).plusDays(1), null, null));
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		CountDownLatch start = new CountDownLatch(1);
		try {
			Future<Boolean> first = executorService.submit(() -> refreshAfterStart(start, "concurrent-refresh-token"));
			Future<Boolean> second = executorService.submit(() -> refreshAfterStart(start, "concurrent-refresh-token"));
			start.countDown();

			long successCount = List.of(first.get(), second.get()).stream().filter(Boolean::booleanValue).count();

			assertThat(successCount).isEqualTo(1);
			List<RefreshToken> activeFamilyTokens = refreshTokenRepository.findActiveByTokenFamilyId(familyId,
					LocalDateTime.now(Clock.systemUTC()));
			assertThat(activeFamilyTokens).hasSize(1);
			assertThat(activeFamilyTokens.getFirst().getParentToken()).isNotNull();
		}
		finally {
			executorService.shutdownNow();
		}
	}

	@Test
	void reusedOldTokenAfterRotationGraceWindowRevokesActiveReplacementFamily() {
		User user = userRepository.save(activeVerifiedUser());
		String familyId = "family-rotated-reuse";
		RefreshToken originalToken = refreshTokenRepository.save(refreshToken(user, "rotated-refresh-token", familyId,
				LocalDateTime.now(Clock.systemUTC()).plusDays(1), null, null));

		authService.refreshAccessToken("rotated-refresh-token", "127.0.0.1", "JUnit");

		RefreshToken rotatedOriginal = refreshTokenRepository.findById(originalToken.getId()).orElseThrow();
		List<RefreshToken> activeFamilyTokens = refreshTokenRepository.findActiveByTokenFamilyId(familyId,
				LocalDateTime.now(Clock.systemUTC()));
		assertThat(activeFamilyTokens).hasSize(1);
		RefreshToken activeReplacement = activeFamilyTokens.getFirst();
		assertThat(activeReplacement.getParentToken().getId()).isEqualTo(rotatedOriginal.getId());
		rotatedOriginal.setRevokedAt(LocalDateTime.now(Clock.systemUTC())
			.minusSeconds(authenticationProperties.getJwt().getRefreshTokenReuseGraceSeconds() + 1));
		refreshTokenRepository.saveAndFlush(rotatedOriginal);

		assertThatThrownBy(() -> authService.refreshAccessToken("rotated-refresh-token", "127.0.0.1", "JUnit"))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining(AppConstants.Auth.REFRESH_TOKEN_REVOKED);

		RefreshToken revokedReplacement = refreshTokenRepository.findById(activeReplacement.getId()).orElseThrow();
		assertThat(revokedReplacement.getRevokedAt()).isNotNull();
		assertThat(revokedReplacement.getRevocationReason())
			.isEqualTo(RefreshTokenRevocationReason.TOKEN_REUSE_DETECTED);
		assertThat(refreshTokenRepository.findActiveByTokenFamilyId(familyId, LocalDateTime.now(Clock.systemUTC())))
			.isEmpty();
	}

	@Test
	void logoutRevokesOnlyAuthenticatedUsersMatchingRefreshToken() {
		User owner = userRepository.save(activeVerifiedUser("logout-owner@example.com", "logout-owner"));
		User otherUser = userRepository.save(activeVerifiedUser("logout-other@example.com", "logout-other"));
		RefreshToken ownerToken = refreshTokenRepository.save(refreshToken(owner, "owned-refresh-token", "family-logout",
				LocalDateTime.now(Clock.systemUTC()).plusDays(1), null, null));

		authService.logout(otherUser.getId(), "owned-refresh-token");

		RefreshToken afterWrongUserLogout = refreshTokenRepository.findById(ownerToken.getId()).orElseThrow();
		assertThat(afterWrongUserLogout.getRevokedAt()).isNull();

		authService.logout(owner.getId(), "owned-refresh-token");

		RefreshToken afterOwnerLogout = refreshTokenRepository.findById(ownerToken.getId()).orElseThrow();
		assertThat(afterOwnerLogout.getRevokedAt()).isNotNull();
		assertThat(afterOwnerLogout.getRevocationReason()).isEqualTo(RefreshTokenRevocationReason.LOGOUT);
	}

	@Test
	void forgotPasswordAndValidResetUseSingleHashedTokenAndRevokeRefreshTokens() {
		User user = userRepository.save(activeVerifiedUser("reset-valid@example.com", "reset-valid"));
		RefreshToken refreshToken = refreshTokenRepository.save(refreshToken(user, "reset-refresh-token",
				"family-reset-valid", LocalDateTime.now(Clock.systemUTC()).plusDays(1), null, null));

		authService.forgotPassword(forgotPasswordRequest(" Reset-Valid@Example.COM "));

		ArgumentCaptor<String> rawTokenCaptor = ArgumentCaptor.forClass(String.class);
		verify(passwordResetNotificationService).sendPasswordResetToken(any(User.class), rawTokenCaptor.capture());
		String rawResetToken = rawTokenCaptor.getValue();
		assertThat(rawResetToken).isNotBlank();
		List<PasswordResetToken> resetTokens = passwordResetTokenRepository.findAll();
		assertThat(resetTokens).hasSize(1);
		assertThat(resetTokens.getFirst().getTokenHash()).isEqualTo(hashToken(rawResetToken));
		assertThat(resetTokens.getFirst().getTokenHash()).isNotEqualTo(rawResetToken);

		authService.resetPassword(resetPasswordRequest(rawResetToken, "NewPassword@123"));

		PasswordResetToken usedToken = passwordResetTokenRepository.findById(resetTokens.getFirst().getId()).orElseThrow();
		assertThat(usedToken.getUsedAt()).isNotNull();
		RefreshToken revokedRefreshToken = refreshTokenRepository.findById(refreshToken.getId()).orElseThrow();
		assertThat(revokedRefreshToken.getRevokedAt()).isNotNull();
		assertThat(revokedRefreshToken.getRevocationReason()).isEqualTo(RefreshTokenRevocationReason.PASSWORD_RESET);
	}

	@Test
	void resetPasswordRejectsInvalidToken() {
		assertThatThrownBy(() -> authService.resetPassword(resetPasswordRequest("missing-token", "NewPassword@123")))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining(AppConstants.Auth.INVALID_RESET_TOKEN);
	}

	@Test
	void resetPasswordRejectsExpiredToken() {
		User user = userRepository.save(activeVerifiedUser("reset-expired@example.com", "reset-expired"));
		passwordResetTokenRepository.save(passwordResetToken(user, "expired-reset-token",
				LocalDateTime.now(Clock.systemUTC()).minusMinutes(1), null, null));

		assertThatThrownBy(() -> authService.resetPassword(resetPasswordRequest("expired-reset-token", "NewPassword@123")))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining(AppConstants.Auth.RESET_TOKEN_EXPIRED);
	}

	@Test
	void resetPasswordRejectsReusedToken() {
		User user = userRepository.save(activeVerifiedUser("reset-reused@example.com", "reset-reused"));
		passwordResetTokenRepository.save(passwordResetToken(user, "reused-reset-token",
				LocalDateTime.now(Clock.systemUTC()).plusMinutes(15), LocalDateTime.now(Clock.systemUTC()), null));

		assertThatThrownBy(() -> authService.resetPassword(resetPasswordRequest("reused-reset-token", "NewPassword@123")))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining(AppConstants.Auth.INVALID_RESET_TOKEN);
	}

	private User activeVerifiedUser() {
		return activeVerifiedUser(EMAIL, "failed-login");
	}

	private User activeVerifiedUser(String email, String username) {
		User user = new User();
		user.setFirstName("Failed");
		user.setLastName("Login");
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

	private RefreshToken refreshToken(User user, String rawToken, String familyId, LocalDateTime expiresAt,
			LocalDateTime revokedAt, RefreshTokenRevocationReason revocationReason) {
		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setUser(user);
		refreshToken.setTokenHash(hashToken(rawToken));
		refreshToken.setTokenFamilyId(familyId);
		refreshToken.setExpiresAt(expiresAt);
		refreshToken.setRevokedAt(revokedAt);
		refreshToken.setRevocationReason(revocationReason);
		return refreshToken;
	}

	private PasswordResetToken passwordResetToken(User user, String rawToken, LocalDateTime expiresAt,
			LocalDateTime usedAt, LocalDateTime invalidatedAt) {
		PasswordResetToken passwordResetToken = new PasswordResetToken();
		passwordResetToken.setUser(user);
		passwordResetToken.setTokenHash(hashToken(rawToken));
		passwordResetToken.setExpiresAt(expiresAt);
		passwordResetToken.setUsedAt(usedAt);
		passwordResetToken.setInvalidatedAt(invalidatedAt);
		return passwordResetToken;
	}

	private boolean refreshAfterStart(CountDownLatch start, String refreshToken) throws Exception {
		start.await();
		try {
			authService.refreshAccessToken(refreshToken, "127.0.0.1", "JUnit");
			return true;
		}
		catch (ResponseStatusException ex) {
			return false;
		}
	}

	private String hashToken(String token) {
		try {
			Mac mac = Mac.getInstance(AppConstants.Auth.HMAC_SHA_256);
			mac.init(new SecretKeySpec(authenticationProperties.getTokenHash()
				.getSecret()
				.getBytes(StandardCharsets.UTF_8), AppConstants.Auth.HMAC_SHA_256));
			return HexFormat.of().formatHex(mac.doFinal(token.getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to hash token for test", ex);
		}
	}

	private LoginRequestDto loginRequest() {
		LoginRequestDto request = new LoginRequestDto();
		request.setEmail(EMAIL);
		request.setPassword("WrongPassword@123");
		return request;
	}

	private ForgotPasswordRequestDto forgotPasswordRequest(String email) {
		ForgotPasswordRequestDto request = new ForgotPasswordRequestDto();
		request.setEmail(email);
		return request;
	}

	private ResetPasswordRequestDto resetPasswordRequest(String token, String newPassword) {
		ResetPasswordRequestDto request = new ResetPasswordRequestDto();
		request.setToken(token);
		request.setNewPassword(newPassword);
		request.setConfirmPassword(newPassword);
		return request;
	}
}
