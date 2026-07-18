package com.mahendra.bizcart_backend.authentication.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({ "local", "test" })
@ConditionalOnProperty(prefix = "bizcart.auth.notification", name = "verification-provider",
		havingValue = "no-op", matchIfMissing = true)
public class NoOpVerificationNotificationService implements VerificationNotificationService {
	@Override public void sendVerification(String email, String firstName, String rawToken) { }
}
