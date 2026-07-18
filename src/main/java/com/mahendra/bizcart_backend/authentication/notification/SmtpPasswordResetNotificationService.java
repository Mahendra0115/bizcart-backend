package com.mahendra.bizcart_backend.authentication.notification;

import com.mahendra.bizcart_backend.authentication.config.AuthenticationProperties;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "bizcart.auth.notification", name = "password-reset-provider",
		havingValue = AppConstants.Auth.NOTIFICATION_PROVIDER_SMTP)
public class SmtpPasswordResetNotificationService implements PasswordResetNotificationService {

	private final JavaMailSender mailSender;
	private final AuthenticationProperties authenticationProperties;

	public SmtpPasswordResetNotificationService(JavaMailSender mailSender,
			AuthenticationProperties authenticationProperties) {
		this.mailSender = mailSender;
		this.authenticationProperties = authenticationProperties;
	}

	@Override
	public void sendPasswordResetToken(String email, String firstName, String resetToken) {
		AuthenticationProperties.Notification notification = authenticationProperties.getNotification();
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(notification.getPasswordResetFrom());
		message.setTo(email);
		message.setSubject(notification.getPasswordResetSubject());
		message.setText(passwordResetMessage(firstName, passwordResetLink(notification.getPasswordResetUrl(), resetToken)));
		mailSender.send(message);
	}

	private String passwordResetLink(String baseUrl, String resetToken) {
		String separator = baseUrl.contains("?") ? "&" : "?";
		return baseUrl + separator + "token=" + URLEncoder.encode(resetToken, StandardCharsets.UTF_8);
	}

	private String passwordResetMessage(String firstName, String resetLink) {
		return """
				Hi %s,

				We received a request to reset your BizCart password.

				Use this secure link to set a new password:
				%s

				This link expires soon and can be used only once. If you did not request a password reset, you can ignore this email.

				Thanks,
				BizCart Team
				""".formatted(firstName, resetLink);
	}
}
