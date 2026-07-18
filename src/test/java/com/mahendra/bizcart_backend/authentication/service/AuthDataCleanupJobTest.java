package com.mahendra.bizcart_backend.authentication.service;

import static org.mockito.Mockito.verify;

import com.mahendra.bizcart_backend.authentication.config.AuthenticationProperties;
import com.mahendra.bizcart_backend.authentication.repository.LoginAttemptRepository;
import com.mahendra.bizcart_backend.authentication.repository.PasswordResetTokenRepository;
import com.mahendra.bizcart_backend.authentication.repository.RefreshTokenRepository;
import com.mahendra.bizcart_backend.authentication.repository.VerificationTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthDataCleanupJobTest {

	@Mock RefreshTokenRepository refreshTokens;
	@Mock PasswordResetTokenRepository resetTokens;
	@Mock VerificationTokenRepository verificationTokens;
	@Mock LoginAttemptRepository loginAttempts;

	@Test
	void cleanupUsesConfiguredRetentionCutoffsForEveryAuthStore() {
		Instant instant = Instant.parse("2026-07-18T03:00:00Z");
		LocalDateTime now = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
		AuthenticationProperties properties = new AuthenticationProperties();
		properties.getCleanup().setInactiveTokenRetentionDays(7);
		properties.getCleanup().setLoginAttemptRetentionDays(90);
		AuthDataCleanupJob job = new AuthDataCleanupJob(refreshTokens, resetTokens, verificationTokens,
				loginAttempts, properties, Clock.fixed(instant, ZoneOffset.UTC));

		job.cleanup();

		verify(refreshTokens).deleteExpiredTokens(now);
		verify(resetTokens).deleteExpiredOrInactiveTokens(now.minusDays(7));
		verify(verificationTokens).deleteExpiredTokens(now.minusDays(7));
		verify(loginAttempts).deleteOldAttempts(now.minusDays(90));
	}
}
