package com.mahendra.bizcart_backend.authentication.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mahendra.bizcart_backend.authentication.config.AuthenticationProperties;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenProviderTest {

	private static final String TEST_SECRET = "0123456789abcdef0123456789abcdef";
	private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

	@Test
	void validateTokenRejectsMalformedToken() {
		JwtTokenProvider jwtTokenProvider = jwtTokenProvider(900);

		assertThat(jwtTokenProvider.validateToken("not-a-jwt")).isFalse();
	}

	@Test
	void validateTokenRejectsWrongAlgorithm() {
		JwtTokenProvider jwtTokenProvider = jwtTokenProvider(900);

		assertThat(jwtTokenProvider.validateToken(signedToken(Map.of("alg", "none", "typ", "JWT"), validClaims())))
			.isFalse();
	}

	@Test
	void validateTokenRejectsMissingRequiredClaims() {
		JwtTokenProvider jwtTokenProvider = jwtTokenProvider(900);
		Map<String, Object> claims = validClaims();
		claims.remove("email");

		assertThat(jwtTokenProvider.validateToken(signedToken(validHeader(), claims))).isFalse();
	}

	@Test
	void validateTokenRejectsInvalidRolesClaim() {
		JwtTokenProvider jwtTokenProvider = jwtTokenProvider(900);
		Map<String, Object> claims = validClaims();
		claims.put("roles", "CUSTOMER");

		assertThat(jwtTokenProvider.validateToken(signedToken(validHeader(), claims))).isFalse();
	}

	@Test
	void validateTokenRejectsInvalidUserTypeClaim() {
		JwtTokenProvider jwtTokenProvider = jwtTokenProvider(900);
		Map<String, Object> claims = validClaims();
		claims.put("userType", "STORE_STAFF");

		assertThat(jwtTokenProvider.validateToken(signedToken(validHeader(), claims))).isFalse();
	}

	@Test
	void validateTokenRejectsExpiryBeforeIssuedAt() {
		JwtTokenProvider jwtTokenProvider = jwtTokenProvider(900);
		Map<String, Object> claims = validClaims();
		claims.put("exp", NOW.minusSeconds(1).getEpochSecond());

		assertThat(jwtTokenProvider.validateToken(signedToken(validHeader(), claims))).isFalse();
	}

	private JwtTokenProvider jwtTokenProvider(long accessTokenExpirySeconds) {
		AuthenticationProperties authenticationProperties = new AuthenticationProperties();
		authenticationProperties.getJwt().setSecret(TEST_SECRET);
		authenticationProperties.getJwt().setAccessTokenExpirySeconds(accessTokenExpirySeconds);
		return new JwtTokenProvider(authenticationProperties, OBJECT_MAPPER, Clock.fixed(NOW, ZoneOffset.UTC));
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

	private Map<String, Object> validHeader() {
		return Map.of("alg", "HS256", "typ", "JWT");
	}

	private Map<String, Object> validClaims() {
		Map<String, Object> claims = new LinkedHashMap<>();
		claims.put("sub", "11");
		claims.put("email", "customer@example.com");
		claims.put("userType", "CUSTOMER");
		claims.put("roles", List.of("CUSTOMER"));
		claims.put("tokenVersion", 3L);
		claims.put("iat", NOW.getEpochSecond());
		claims.put("exp", NOW.plusSeconds(900).getEpochSecond());
		claims.put("jti", "test-jwt-id");
		return claims;
	}

	private String signedToken(Map<String, Object> header, Map<String, Object> claims) {
		try {
			Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
			String encodedHeader = encoder.encodeToString(OBJECT_MAPPER.writeValueAsBytes(header));
			String encodedClaims = encoder.encodeToString(OBJECT_MAPPER.writeValueAsBytes(claims));
			String unsignedToken = encodedHeader + "." + encodedClaims;
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return unsignedToken + "." + encoder.encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
}
