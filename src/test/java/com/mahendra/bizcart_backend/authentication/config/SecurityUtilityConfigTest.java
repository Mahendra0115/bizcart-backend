package com.mahendra.bizcart_backend.authentication.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class SecurityUtilityConfigTest {

	@Test
	void passwordEncoderUsesBcrypt() {
		AuthenticationProperties authenticationProperties = new AuthenticationProperties();
		authenticationProperties.getPassword().setBcryptStrength(4);
		SecurityUtilityConfig securityUtilityConfig = new SecurityUtilityConfig();

		PasswordEncoder passwordEncoder = securityUtilityConfig.passwordEncoder(authenticationProperties);
		String encodedPassword = passwordEncoder.encode("Password@123");

		assertThat(encodedPassword).startsWith("$2");
		assertThat(passwordEncoder.matches("Password@123", encodedPassword)).isTrue();
	}
}
