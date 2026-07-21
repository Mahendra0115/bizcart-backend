package com.mahendra.bizcart_backend.authentication.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class VerificationNotificationEventListener {
	private static final Logger log = LoggerFactory.getLogger(VerificationNotificationEventListener.class);
	private final VerificationNotificationService service;
	public VerificationNotificationEventListener(VerificationNotificationService service) { this.service = service; }

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void send(VerificationNotificationEvent event) {
		try {
			service.sendVerification(event.email(), event.firstName(), event.token());
		} catch (RuntimeException ex) {
			log.warn("Verification email delivery failed for userId={}", event.userId(), ex);
		}
	}
}
