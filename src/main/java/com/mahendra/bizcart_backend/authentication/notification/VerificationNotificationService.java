package com.mahendra.bizcart_backend.authentication.notification;

public interface VerificationNotificationService {
	void sendVerification(String email, String firstName, String rawToken);
}
