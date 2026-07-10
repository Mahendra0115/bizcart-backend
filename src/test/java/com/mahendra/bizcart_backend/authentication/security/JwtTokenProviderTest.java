package com.mahendra.bizcart_backend.authentication.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mahendra.bizcart_backend.authentication.config.AuthenticationProperties;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenProviderTest {

	private static final String TEST_SECRET = "0123456789abcdef0123456789abcdef";
	private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");

	@Test
	void generateAccessTokenIncludesRequiredClaims() {
		JwtTokenProvider jwtTokenProvider = jwtTokenProvider(900);
		User user = user();

		String token = jwtTokenProvider.generateAccessToken(user, List.of("CUSTOMER"));

		assertThat(jwtTokenProvider.validateToken(token)).isTrue();
		assertThat(jwtTokenProvider.extractUserId(token)).isEqualTo(11L);
		assertThat(jwtTokenProvider.extractEmail(token)).isEqualTo("customer@example.com");
		assertThat(jwtTokenProvider.extractRoles(token)).containsExactly("CUSTOMER");
		assertThat(jwtTokenProvider.extractTokenVersion(token)).isEqualTo(3L);
		assertThat(jwtTokenProvider.extractExpiration(token)).isEqualTo(NOW.plusSeconds(900));

		Map<String, Object> claims = jwtTokenProvider.extractClaims(token);
		assertThat(claims).containsEntry("userType", UserType.CUSTOMER.name());
		assertThat(claims).containsKeys("iat", "exp", "jti");
	}

	@Test
	void validateTokenRejectsExpiredToken() {
		JwtTokenProvider jwtTokenProvider = jwtTokenProvider(-1);

		String token = jwtTokenProvider.generateAccessToken(user(), List.of());

		assertThat(jwtTokenProvider.validateToken(token)).isFalse();
	}

	@Test
	void validateTokenRejectsTamperedToken() {
		JwtTokenProvider jwtTokenProvider = jwtTokenProvider(900);
		String token = jwtTokenProvider.generateAccessToken(user(), List.of("CUSTOMER"));
		String tamperedToken = token.substring(0, token.length() - 2) + "xx";

		assertThat(jwtTokenProvider.validateToken(tamperedToken)).isFalse();
	}

	private JwtTokenProvider jwtTokenProvider(long accessTokenExpirySeconds) {
		AuthenticationProperties authenticationProperties = new AuthenticationProperties();
		authenticationProperties.getJwt().setSecret(TEST_SECRET);
		authenticationProperties.getJwt().setAccessTokenExpirySeconds(accessTokenExpirySeconds);
		return new JwtTokenProvider(authenticationProperties, new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private User user() {
		User user = new User();
		ReflectionTestUtils.setField(user, "id", 11L);
		user.setFirstName("Test");
		user.setLastName("Customer");
		user.setEmail("customer@example.com");
		user.setPassword("$2a$12$encodedPasswordPlaceholder");
		user.setUserType(UserType.CUSTOMER);
		user.setStatus(AccountStatus.ACTIVE);
		user.setEmailVerified(true);
		user.setTokenVersion(3L);
		return user;
	}
}
