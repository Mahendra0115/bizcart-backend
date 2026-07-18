package com.mahendra.bizcart_backend.authentication.notification;

import com.mahendra.bizcart_backend.authentication.config.AuthenticationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@ConditionalOnProperty(prefix = "bizcart.auth.notification", name = "verification-provider", havingValue = "smtp")
public class SmtpVerificationNotificationService implements VerificationNotificationService {
	private final JavaMailSender sender;
	private final AuthenticationProperties properties;
	public SmtpVerificationNotificationService(JavaMailSender sender, AuthenticationProperties properties) {
		this.sender = sender; this.properties = properties;
	}
	@Override
	public void sendVerification(String email, String firstName, String rawToken) {
		String link = UriComponentsBuilder.fromUriString(properties.getNotification().getVerificationUrl())
			.queryParam("token", rawToken).build().encode().toUriString();
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(properties.getNotification().getVerificationFrom());
		message.setTo(email);
		message.setSubject(properties.getNotification().getVerificationSubject());
		message.setText("Hello " + firstName + ",\n\nVerify your BizCart email:\n" + link
				+ "\n\nThis link expires in 24 hours.");
		sender.send(message);
	}
}
