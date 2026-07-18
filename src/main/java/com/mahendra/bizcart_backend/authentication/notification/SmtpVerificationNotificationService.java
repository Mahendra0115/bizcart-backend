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
		String expiry = formatExpiry(properties.getJwt().getVerificationTokenExpirySeconds());
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(properties.getNotification().getVerificationFrom());
		message.setTo(email);
		message.setSubject(properties.getNotification().getVerificationSubject());
		message.setText("Hello " + firstName + ",\n\nVerify your BizCart email:\n" + link
				+ "\n\nThis link expires in " + expiry + ".");
		sender.send(message);
	}

	private String formatExpiry(long seconds) {
		if (seconds % 86400 == 0) return quantity(seconds / 86400, "day");
		if (seconds % 3600 == 0) return quantity(seconds / 3600, "hour");
		if (seconds % 60 == 0) return quantity(seconds / 60, "minute");
		return quantity(seconds, "second");
	}

	private String quantity(long value, String unit) {
		return value + " " + unit + (value == 1 ? "" : "s");
	}
}
