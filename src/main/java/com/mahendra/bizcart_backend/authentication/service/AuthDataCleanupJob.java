package com.mahendra.bizcart_backend.authentication.service;

import com.mahendra.bizcart_backend.authentication.config.AuthenticationProperties;
import com.mahendra.bizcart_backend.authentication.repository.LoginAttemptRepository;
import com.mahendra.bizcart_backend.authentication.repository.PasswordResetTokenRepository;
import com.mahendra.bizcart_backend.authentication.repository.RefreshTokenRepository;
import com.mahendra.bizcart_backend.authentication.repository.VerificationTokenRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuthDataCleanupJob {
	private static final Logger log = LoggerFactory.getLogger(AuthDataCleanupJob.class);
	private final RefreshTokenRepository refreshTokens;
	private final PasswordResetTokenRepository resetTokens;
	private final VerificationTokenRepository verificationTokens;
	private final LoginAttemptRepository loginAttempts;
	private final AuthenticationProperties properties;
	private final Clock clock;

	public AuthDataCleanupJob(RefreshTokenRepository refreshTokens, PasswordResetTokenRepository resetTokens,
			VerificationTokenRepository verificationTokens, LoginAttemptRepository loginAttempts,
			AuthenticationProperties properties, Clock clock) {
		this.refreshTokens = refreshTokens; this.resetTokens = resetTokens;
		this.verificationTokens = verificationTokens; this.loginAttempts = loginAttempts;
		this.properties = properties; this.clock = clock;
	}

	@Scheduled(cron = "${bizcart.auth.cleanup.cron:0 0 3 * * *}")
	@Transactional
	public void cleanup() {
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime inactiveCutoff = now.minusDays(properties.getCleanup().getInactiveTokenRetentionDays());
		int refresh = refreshTokens.deleteExpiredTokens(now);
		int reset = resetTokens.deleteExpiredOrInactiveTokens(inactiveCutoff);
		int verification = verificationTokens.deleteExpiredTokens(inactiveCutoff);
		int attempts = loginAttempts.deleteOldAttempts(
				now.minusDays(properties.getCleanup().getLoginAttemptRetentionDays()));
		log.info("Authentication cleanup removed refreshTokens={}, resetTokens={}, verificationTokens={}, loginAttempts={}",
				refresh, reset, verification, attempts);
	}
}
