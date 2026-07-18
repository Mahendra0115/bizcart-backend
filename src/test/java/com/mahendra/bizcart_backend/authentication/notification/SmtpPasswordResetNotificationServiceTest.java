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
class SmtpPasswordResetNotificationServiceTest {

	@Mock
	private JavaMailSender mailSender;

	@Test
	void sendPasswordResetTokenBuildsEmailWithResetLink() {
		AuthenticationProperties authenticationProperties = new AuthenticationProperties();
		authenticationProperties.getNotification().setPasswordResetFrom("security@bizcart.test");
		authenticationProperties.getNotification().setPasswordResetSubject("Reset your test password");
		authenticationProperties.getNotification().setPasswordResetUrl("https://app.bizcart.test/reset-password");
		SmtpPasswordResetNotificationService notificationService =
				new SmtpPasswordResetNotificationService(mailSender, authenticationProperties);
		notificationService.sendPasswordResetToken("mahendra@example.com", "Mahendra", "reset token+/=");

		ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(messageCaptor.capture());
		SimpleMailMessage message = messageCaptor.getValue();
		assertThat(message.getFrom()).isEqualTo("security@bizcart.test");
		assertThat(message.getTo()).containsExactly("mahendra@example.com");
		assertThat(message.getSubject()).isEqualTo("Reset your test password");
		assertThat(message.getText()).contains("Hi Mahendra");
		assertThat(message.getText())
			.contains("https://app.bizcart.test/reset-password?token=reset+token%2B%2F%3D");
		assertThat(message.getText()).contains("can be used only once");
	}
}
