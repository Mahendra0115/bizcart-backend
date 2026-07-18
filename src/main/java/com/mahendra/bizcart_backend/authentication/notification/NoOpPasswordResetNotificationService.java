package com.mahendra.bizcart_backend.authentication.notification;

import com.mahendra.bizcart_backend.common.constants.AppConstants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({ "local", "test" })
@ConditionalOnProperty(prefix = "bizcart.auth.notification", name = "password-reset-provider",
		havingValue = AppConstants.Auth.NOTIFICATION_PROVIDER_NO_OP, matchIfMissing = true)
public class NoOpPasswordResetNotificationService implements PasswordResetNotificationService {

	@Override
	public void sendPasswordResetToken(String email, String firstName, String resetToken) {
		// Email provider integration belongs behind this abstraction.
	}
}
