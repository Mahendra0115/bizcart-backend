package com.mahendra.bizcart_backend.authentication.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PasswordResetNotificationEventListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetNotificationEventListener.class);

	private final PasswordResetNotificationService passwordResetNotificationService;

	public PasswordResetNotificationEventListener(PasswordResetNotificationService passwordResetNotificationService) {
		this.passwordResetNotificationService = passwordResetNotificationService;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void sendPasswordResetNotification(PasswordResetNotificationEvent event) {
		try {
			passwordResetNotificationService.sendPasswordResetToken(event.email(), event.firstName(), event.resetToken());
		}
		catch (RuntimeException ex) {
			LOGGER.warn("Password reset email delivery failed for userId={}", event.userId(), ex);
		}
	}
}
