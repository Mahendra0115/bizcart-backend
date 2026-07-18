package com.mahendra.bizcart_backend.authentication.notification;

public interface PasswordResetNotificationService {

	void sendPasswordResetToken(String email, String firstName, String resetToken);
}
