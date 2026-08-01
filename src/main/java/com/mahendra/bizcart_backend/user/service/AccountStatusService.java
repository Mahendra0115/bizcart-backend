package com.mahendra.bizcart_backend.user.service;

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
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountStatusService {

	private static final Logger log = LoggerFactory.getLogger(AccountStatusService.class);
	private static final Map<AccountStatus, EnumSet<AccountStatus>> ALLOWED_TRANSITIONS = Map.of(
			AccountStatus.PENDING, EnumSet.of(AccountStatus.ACTIVE),
			AccountStatus.ACTIVE, EnumSet.of(AccountStatus.INACTIVE, AccountStatus.BLOCKED),
			AccountStatus.INACTIVE, EnumSet.of(AccountStatus.ACTIVE, AccountStatus.BLOCKED),
			AccountStatus.BLOCKED, EnumSet.of(AccountStatus.ACTIVE, AccountStatus.INACTIVE));

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final UserStatusHistoryRepository userStatusHistoryRepository;
	private final Clock clock;

	public AccountStatusService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
			UserStatusHistoryRepository userStatusHistoryRepository, Clock clock) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.userStatusHistoryRepository = userStatusHistoryRepository;
		this.clock = clock;
	}

	@Transactional
	public AccountStatusResponseDto updateStatus(Long adminUserId, Long targetUserId,
			UpdateAccountStatusRequestDto request) {
		User target = userRepository.findByIdForUpdate(targetUserId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, AppConstants.User.USER_NOT_FOUND));
		AccountStatus oldStatus = target.getStatus();
		AccountStatus newStatus = request.status();
		validateSelfAction(adminUserId, targetUserId, newStatus);
		validateTransition(oldStatus, newStatus);
		validateActivationPrerequisites(target, newStatus);
		String reason = normalizeReason(request.reason());
		if (newStatus == AccountStatus.BLOCKED && reason == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, AppConstants.User.BLOCK_REASON_REQUIRED);
		}
		User admin = userRepository.getReferenceById(adminUserId);
		LocalDateTime changedAt = LocalDateTime.now(clock);

		target.setStatus(newStatus);
		if (newStatus == AccountStatus.BLOCKED || newStatus == AccountStatus.INACTIVE) {
			target.setTokenVersion(target.getTokenVersion() + 1);
			userRepository.saveAndFlush(target);
			refreshTokenRepository.revokeActiveTokensByUserId(targetUserId, changedAt,
					RefreshTokenRevocationReason.ADMIN_REVOKED, changedAt);
		} else {
			userRepository.save(target);
		}
		userStatusHistoryRepository.save(createHistory(admin, target, oldStatus, newStatus, reason, changedAt));
		log.info("Admin user {} changed user {} status from {} to {} reasonPresent={}", adminUserId, targetUserId,
				oldStatus, newStatus, reason != null);
		return new AccountStatusResponseDto(target.getId(), target.getEmail(), target.getUserType(), oldStatus,
				newStatus, reason);
	}

	private UserStatusHistory createHistory(User admin, User target, AccountStatus oldStatus, AccountStatus newStatus,
			String reason, LocalDateTime changedAt) {
		UserStatusHistory history = new UserStatusHistory();
		history.setAdminUser(admin);
		history.setTargetUser(target);
		history.setOldStatus(oldStatus);
		history.setNewStatus(newStatus);
		history.setReason(reason);
		history.setChangedAt(changedAt);
		return history;
	}

	private void validateSelfAction(Long adminUserId, Long targetUserId, AccountStatus newStatus) {
		if (adminUserId.equals(targetUserId)
				&& (newStatus == AccountStatus.BLOCKED || newStatus == AccountStatus.INACTIVE)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					AppConstants.User.SELF_STATUS_CHANGE_FORBIDDEN);
		}
	}

	private void validateTransition(AccountStatus oldStatus, AccountStatus newStatus) {
		if (!ALLOWED_TRANSITIONS.getOrDefault(oldStatus, EnumSet.noneOf(AccountStatus.class)).contains(newStatus)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, AppConstants.User.INVALID_STATUS_TRANSITION);
		}
	}

	private void validateActivationPrerequisites(User target, AccountStatus newStatus) {
		if (newStatus != AccountStatus.ACTIVE) {
			return;
		}
		if (!target.isEmailVerified()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					AppConstants.User.EMAIL_VERIFICATION_REQUIRED);
		}
		if (target.getUserType() == UserType.SELLER && !target.isAdminApproved()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					AppConstants.User.SELLER_APPROVAL_REQUIRED);
		}
	}

	private String normalizeReason(String reason) {
		return StringUtils.hasText(reason) ? reason.trim() : null;
	}
}
