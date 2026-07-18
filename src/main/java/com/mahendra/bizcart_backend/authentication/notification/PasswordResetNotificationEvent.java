package com.mahendra.bizcart_backend.authentication.notification;

public record PasswordResetNotificationEvent(Long userId, String email, String firstName, String resetToken) {
}
