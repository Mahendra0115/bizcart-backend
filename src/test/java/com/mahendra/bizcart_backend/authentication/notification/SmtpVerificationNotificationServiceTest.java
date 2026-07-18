package com.mahendra.bizcart_backend.authentication.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.mahendra.bizcart_backend.authentication.config.AuthenticationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class SmtpVerificationNotificationServiceTest {

	@Mock
	private JavaMailSender mailSender;

	@Test
	void emailUsesConfiguredVerificationTokenExpiry() {
		AuthenticationProperties properties = new AuthenticationProperties();
		properties.getJwt().setVerificationTokenExpirySeconds(7200);
		properties.getNotification().setVerificationFrom("security@bizcart.test");
		properties.getNotification().setVerificationSubject("Verify your test email");
		properties.getNotification().setVerificationUrl("https://app.bizcart.test/verify-email");

		new SmtpVerificationNotificationService(mailSender, properties)
			.sendVerification("mahendra@example.com", "Mahendra", "verify-token_123");

		ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(captor.capture());
		SimpleMailMessage message = captor.getValue();
		assertThat(message.getFrom()).isEqualTo("security@bizcart.test");
		assertThat(message.getTo()).containsExactly("mahendra@example.com");
		assertThat(message.getSubject()).isEqualTo("Verify your test email");
		assertThat(message.getText()).contains("Hello Mahendra");
		assertThat(message.getText())
			.contains("https://app.bizcart.test/verify-email?token=verify-token_123");
		assertThat(message.getText()).contains("This link expires in 2 hours.");
		assertThat(message.getText()).doesNotContain("24 hours");
	}
}
