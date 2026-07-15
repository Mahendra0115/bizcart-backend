package com.mahendra.bizcart_backend.authentication.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties(AuthenticationProperties.class)
public class SecurityUtilityConfig {

	@Bean
	public PasswordEncoder passwordEncoder(AuthenticationProperties authenticationProperties) {
		return new BCryptPasswordEncoder(authenticationProperties.getPassword().getBcryptStrength());
	}

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}
