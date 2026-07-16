package com.mahendra.bizcart_backend.authentication.notification;

import com.mahendra.bizcart_backend.user.entity.User;

public interface PasswordResetNotificationService {

	void sendPasswordResetToken(User user, String resetToken);
}
