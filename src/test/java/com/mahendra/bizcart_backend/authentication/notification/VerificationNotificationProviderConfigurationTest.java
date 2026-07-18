package com.mahendra.bizcart_backend.authentication.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.mahendra.bizcart_backend.authentication.config.AuthenticationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

class VerificationNotificationProviderConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(PropertiesConfiguration.class)
		.withBean(JavaMailSender.class, () -> mock(JavaMailSender.class));

	@Test
	void smtpProviderCreatesServiceAndListener() {
		contextRunner
			.withPropertyValues(
					"bizcart.auth.jwt.secret=test-only-0123456789abcdef0123456789abcdef",
					"bizcart.auth.token-hash.secret=test-token-hash-0123456789abcdef",
					"bizcart.auth.notification.verification-provider=smtp",
					"bizcart.auth.notification.verification-from=security@bizcart.test",
					"bizcart.auth.notification.verification-url=https://app.bizcart.test/verify-email")
			.withUserConfiguration(SmtpVerificationNotificationService.class,
					VerificationNotificationEventListener.class)
			.run(context -> {
				assertThat(context).hasSingleBean(VerificationNotificationService.class);
				assertThat(context).hasSingleBean(VerificationNotificationEventListener.class);
			});
	}

	@Test
	void deployedContextFailsFastWhenNoVerificationProviderBeanExists() {
		contextRunner
			.withPropertyValues(
					"bizcart.auth.jwt.secret=test-only-0123456789abcdef0123456789abcdef",
					"bizcart.auth.token-hash.secret=test-token-hash-0123456789abcdef")
			.withUserConfiguration(VerificationNotificationEventListener.class)
			.run(context -> assertThat(context).hasFailed());
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(AuthenticationProperties.class)
	static class PropertiesConfiguration {
	}
}
