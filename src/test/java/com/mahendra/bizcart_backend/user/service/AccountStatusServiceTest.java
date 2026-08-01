package com.mahendra.bizcart_backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import com.mahendra.bizcart_backend.authentication.entity.RefreshTokenRevocationReason;
import com.mahendra.bizcart_backend.authentication.repository.RefreshTokenRepository;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.user.dto.request.UpdateAccountStatusRequestDto;
import com.mahendra.bizcart_backend.user.dto.response.AccountStatusResponseDto;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.entity.UserStatusHistory;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import com.mahendra.bizcart_backend.user.repository.UserRepository;
import com.mahendra.bizcart_backend.user.repository.UserStatusHistoryRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AccountStatusServiceTest {

	@Mock UserRepository userRepository;
	@Mock RefreshTokenRepository refreshTokenRepository;
	@Mock UserStatusHistoryRepository userStatusHistoryRepository;

	private AccountStatusService service;
	private final Clock clock = Clock.fixed(Instant.parse("2026-08-01T10:00:00Z"), ZoneOffset.UTC);

	@BeforeEach
	void setUp() {
		service = new AccountStatusService(userRepository, refreshTokenRepository, userStatusHistoryRepository, clock);
	}

	@ParameterizedTest(name = "allows {0} to {1}")
	@CsvSource({
			"PENDING, ACTIVE",
			"ACTIVE, INACTIVE",
			"ACTIVE, BLOCKED",
			"INACTIVE, ACTIVE",
			"INACTIVE, BLOCKED",
			"BLOCKED, ACTIVE",
			"BLOCKED, INACTIVE"
	})
	void allowsEveryDocumentedStatusTransition(AccountStatus oldStatus, AccountStatus newStatus) {
		User target = user(2L, oldStatus);
		when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(target));
		when(userRepository.getReferenceById(1L)).thenReturn(user(1L, AccountStatus.ACTIVE));
		String reason = newStatus == AccountStatus.BLOCKED ? "Policy violation" : null;

		AccountStatusResponseDto response = service.updateStatus(1L, 2L,
				new UpdateAccountStatusRequestDto(newStatus, reason));

		assertThat(target.getStatus()).isEqualTo(newStatus);
		assertThat(response.oldStatus()).isEqualTo(oldStatus);
		assertThat(response.newStatus()).isEqualTo(newStatus);
		verify(userStatusHistoryRepository).save(org.mockito.ArgumentMatchers.argThat(history ->
				history.getOldStatus() == oldStatus && history.getNewStatus() == newStatus));
	}

	@ParameterizedTest(name = "rejects {0} to {1}")
	@CsvSource({
			"ACTIVE, PENDING",
			"INACTIVE, PENDING",
			"BLOCKED, PENDING",
			"ACTIVE, ACTIVE",
			"PENDING, BLOCKED",
			"PENDING, INACTIVE"
	})
	void rejectsEveryDocumentedInvalidStatusTransition(AccountStatus oldStatus, AccountStatus newStatus) {
		User target = user(2L, oldStatus);
		long originalTokenVersion = target.getTokenVersion();
		when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(target));

		assertFailure(() -> service.updateStatus(1L, 2L,
				new UpdateAccountStatusRequestDto(newStatus, "Policy violation")), HttpStatus.BAD_REQUEST,
				AppConstants.User.INVALID_STATUS_TRANSITION);

		assertThat(target.getStatus()).isEqualTo(oldStatus);
		assertThat(target.getTokenVersion()).isEqualTo(originalTokenVersion);
		verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
		verify(userRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any(User.class));
		verify(userStatusHistoryRepository, never()).save(org.mockito.ArgumentMatchers.any(UserStatusHistory.class));
		verify(refreshTokenRepository, never()).revokeActiveTokensByUserId(org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any());
	}

	@Test
	void blockUpdatesStatusAndRevokesAllSessions() {
		User target = user(2L, AccountStatus.ACTIVE);
		User admin = user(1L, AccountStatus.ACTIVE);
		when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(target));
		when(userRepository.getReferenceById(1L)).thenReturn(admin);

		AccountStatusResponseDto response = service.updateStatus(1L, 2L,
				new UpdateAccountStatusRequestDto(AccountStatus.BLOCKED, " Suspicious activity "));

		assertThat(target.getStatus()).isEqualTo(AccountStatus.BLOCKED);
		assertThat(target.getTokenVersion()).isEqualTo(4L);
		assertThat(response.oldStatus()).isEqualTo(AccountStatus.ACTIVE);
		assertThat(response.newStatus()).isEqualTo(AccountStatus.BLOCKED);
		assertThat(response.reason()).isEqualTo("Suspicious activity");
		verify(userRepository).saveAndFlush(target);
		LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
		verify(refreshTokenRepository).revokeActiveTokensByUserId(2L, now,
				RefreshTokenRevocationReason.ADMIN_REVOKED, now);
		verify(userStatusHistoryRepository).save(org.mockito.ArgumentMatchers.argThat(history ->
				history.getAdminUser() == admin && history.getTargetUser() == target
						&& history.getOldStatus() == AccountStatus.ACTIVE
						&& history.getNewStatus() == AccountStatus.BLOCKED
						&& "Suspicious activity".equals(history.getReason())
						&& now.equals(history.getChangedAt())));
	}

	@Test
	void deactivateRevokesSessionsAndActivateDoesNot() {
		User active = user(2L, AccountStatus.ACTIVE);
		when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(active));
		when(userRepository.getReferenceById(1L)).thenReturn(user(1L, AccountStatus.ACTIVE));
		service.updateStatus(1L, 2L, new UpdateAccountStatusRequestDto(AccountStatus.INACTIVE, null));
		verify(refreshTokenRepository).revokeActiveTokensByUserId(org.mockito.ArgumentMatchers.eq(2L),
				org.mockito.ArgumentMatchers.any(LocalDateTime.class),
				org.mockito.ArgumentMatchers.eq(RefreshTokenRevocationReason.ADMIN_REVOKED),
				org.mockito.ArgumentMatchers.any(LocalDateTime.class));

		User inactive = user(3L, AccountStatus.INACTIVE);
		when(userRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(inactive));
		service.updateStatus(1L, 3L, new UpdateAccountStatusRequestDto(AccountStatus.ACTIVE, null));
		verify(userRepository).save(inactive);
		verify(refreshTokenRepository, never()).revokeActiveTokensByUserId(org.mockito.ArgumentMatchers.eq(3L),
				org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any());
		verify(userStatusHistoryRepository, org.mockito.Mockito.times(2)).save(any(UserStatusHistory.class));
	}

	@Test
	void blockRequiresReason() {
		when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(user(2L, AccountStatus.ACTIVE)));
		assertFailure(() -> service.updateStatus(1L, 2L,
				new UpdateAccountStatusRequestDto(AccountStatus.BLOCKED, "  ")), HttpStatus.BAD_REQUEST,
				AppConstants.User.BLOCK_REASON_REQUIRED);
		verify(refreshTokenRepository, never()).revokeActiveTokensByUserId(org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any());
	}

	@Test
	void rejectsInvalidTransitionWithoutMutation() {
		User pending = user(2L, AccountStatus.PENDING);
		when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(pending));
		assertFailure(() -> service.updateStatus(1L, 2L,
				new UpdateAccountStatusRequestDto(AccountStatus.BLOCKED, "reason")), HttpStatus.BAD_REQUEST,
				AppConstants.User.INVALID_STATUS_TRANSITION);
		assertThat(pending.getStatus()).isEqualTo(AccountStatus.PENDING);
		verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
	}

	@Test
	void rejectsAdminSelfBlockAndDeactivate() {
		when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user(1L, AccountStatus.ACTIVE)));
		assertFailure(() -> service.updateStatus(1L, 1L,
				new UpdateAccountStatusRequestDto(AccountStatus.BLOCKED, "reason")), HttpStatus.FORBIDDEN,
				AppConstants.User.SELF_STATUS_CHANGE_FORBIDDEN);
		assertFailure(() -> service.updateStatus(1L, 1L,
				new UpdateAccountStatusRequestDto(AccountStatus.INACTIVE, null)), HttpStatus.FORBIDDEN,
				AppConstants.User.SELF_STATUS_CHANGE_FORBIDDEN);
	}

	@Test
	void returnsNotFoundForMissingTarget() {
		when(userRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());
		assertFailure(() -> service.updateStatus(1L, 99L,
				new UpdateAccountStatusRequestDto(AccountStatus.ACTIVE, null)), HttpStatus.NOT_FOUND,
				AppConstants.User.USER_NOT_FOUND);
	}

	@Test
	void rejectsActivationWhenEmailIsNotVerified() {
		User target = user(2L, AccountStatus.PENDING);
		target.setEmailVerified(false);
		when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(target));

		assertFailure(() -> service.updateStatus(1L, 2L,
				new UpdateAccountStatusRequestDto(AccountStatus.ACTIVE, null)), HttpStatus.BAD_REQUEST,
				AppConstants.User.EMAIL_VERIFICATION_REQUIRED);

		assertThat(target.getStatus()).isEqualTo(AccountStatus.PENDING);
		verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
	}

	@Test
	void rejectsActivationWhenSellerIsNotApproved() {
		User target = user(2L, AccountStatus.PENDING);
		target.setUserType(UserType.SELLER);
		target.setAdminApproved(false);
		when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(target));

		assertFailure(() -> service.updateStatus(1L, 2L,
				new UpdateAccountStatusRequestDto(AccountStatus.ACTIVE, null)), HttpStatus.BAD_REQUEST,
				AppConstants.User.SELLER_APPROVAL_REQUIRED);

		assertThat(target.getStatus()).isEqualTo(AccountStatus.PENDING);
		verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
	}

	private void assertFailure(Runnable action, HttpStatus status, String reason) {
		assertThatThrownBy(action::run).isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
			assertThat(exception.getStatusCode()).isEqualTo(status);
			assertThat(exception.getReason()).isEqualTo(reason);
		});
	}

	private User user(Long id, AccountStatus status) {
		User user = new User();
		ReflectionTestUtils.setField(user, "id", id);
		user.setEmail("user" + id + "@example.com");
		user.setUserType(UserType.CUSTOMER);
		user.setStatus(status);
		user.setEmailVerified(true);
		user.setAdminApproved(true);
		user.setTokenVersion(3L);
		return user;
	}
}
