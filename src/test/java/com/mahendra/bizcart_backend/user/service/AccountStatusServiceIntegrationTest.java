package com.mahendra.bizcart_backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.mahendra.bizcart_backend.authentication.entity.RefreshToken;
import com.mahendra.bizcart_backend.authentication.entity.RefreshTokenRevocationReason;
import com.mahendra.bizcart_backend.authentication.repository.RefreshTokenRepository;
import com.mahendra.bizcart_backend.user.dto.request.UpdateAccountStatusRequestDto;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.entity.UserStatusHistory;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import com.mahendra.bizcart_backend.user.repository.UserRepository;
import com.mahendra.bizcart_backend.user.repository.UserStatusHistoryRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccountStatusServiceIntegrationTest {

	@Autowired AccountStatusService accountStatusService;
	@Autowired UserRepository userRepository;
	@Autowired RefreshTokenRepository refreshTokenRepository;
	@Autowired UserStatusHistoryRepository userStatusHistoryRepository;

	@Test
	void blockingUserRevokesDatabaseRefreshTokensAndPersistsAuditHistory() {
		User admin = user("audit-admin", AccountStatus.ACTIVE);
		User target = user("audit-target", AccountStatus.ACTIVE);
		admin = userRepository.saveAndFlush(admin);
		target = userRepository.saveAndFlush(target);
		Long adminUserId = admin.getId();
		Long targetUserId = target.getId();
		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setUser(target);
		refreshToken.setTokenHash("audit-token-" + UUID.randomUUID());
		refreshToken.setTokenFamilyId(UUID.randomUUID().toString());
		refreshToken.setExpiresAt(LocalDateTime.now().plusDays(1));
		refreshToken = refreshTokenRepository.saveAndFlush(refreshToken);
		Long refreshTokenId = refreshToken.getId();

		accountStatusService.updateStatus(adminUserId, targetUserId,
				new UpdateAccountStatusRequestDto(AccountStatus.BLOCKED, "Confirmed abuse"));

		User updatedTarget = userRepository.findById(targetUserId).orElseThrow();
		RefreshToken revokedToken = refreshTokenRepository.findById(refreshTokenId).orElseThrow();
		UserStatusHistory history = userStatusHistoryRepository.findAll().stream()
			.filter(item -> item.getTargetUser().getId().equals(targetUserId))
			.findFirst().orElseThrow();
		assertThat(updatedTarget.getStatus()).isEqualTo(AccountStatus.BLOCKED);
		assertThat(updatedTarget.getTokenVersion()).isEqualTo(2L);
		assertThat(revokedToken.getRevokedAt()).isNotNull();
		assertThat(revokedToken.getRevocationReason()).isEqualTo(RefreshTokenRevocationReason.ADMIN_REVOKED);
		assertThat(history.getAdminUser().getId()).isEqualTo(adminUserId);
		assertThat(history.getOldStatus()).isEqualTo(AccountStatus.ACTIVE);
		assertThat(history.getNewStatus()).isEqualTo(AccountStatus.BLOCKED);
		assertThat(history.getReason()).isEqualTo("Confirmed abuse");
	}

	private User user(String identity, AccountStatus status) {
		User user = new User();
		user.setFirstName("Audit");
		user.setLastName("User");
		user.setUsername(identity);
		user.setEmail(identity + "@example.com");
		user.setPassword("encoded-password");
		user.setUserType(UserType.CUSTOMER);
		user.setStatus(status);
		user.setEmailVerified(true);
		user.setAdminApproved(true);
		user.setTokenVersion(1L);
		return user;
	}
}
