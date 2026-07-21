package com.mahendra.bizcart_backend.authentication.notification;

public record VerificationNotificationEvent(Long userId, String email, String firstName, String token) {
}
