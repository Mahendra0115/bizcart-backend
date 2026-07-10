package com.mahendra.bizcart_backend.authentication.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mahendra.bizcart_backend.authentication.config.AuthenticationProperties;
import com.mahendra.bizcart_backend.user.entity.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtTokenProvider {

	private static final String TOKEN_TYPE = "JWT";
	private static final String ALGORITHM = "HS256";
	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final int MIN_SECRET_BYTES = 32;
	private static final String HEADER_ALGORITHM = "alg";
	private static final String HEADER_TYPE = "typ";
	private static final String CLAIM_SUBJECT = "sub";
	private static final String CLAIM_EMAIL = "email";
	private static final String CLAIM_USER_TYPE = "userType";
	private static final String CLAIM_ROLES = "roles";
	private static final String CLAIM_TOKEN_VERSION = "tokenVersion";
	private static final String CLAIM_ISSUED_AT = "iat";
	private static final String CLAIM_EXPIRES_AT = "exp";
	private static final String CLAIM_JWT_ID = "jti";
	private static final String TOKEN_SEPARATOR = ".";
	private static final TypeReference<Map<String, Object>> CLAIM_MAP_TYPE = new TypeReference<>() {
	};

	private final AuthenticationProperties authenticationProperties;
	private final ObjectMapper objectMapper;
	private final Clock clock;
	private final Base64.Encoder base64UrlEncoder = Base64.getUrlEncoder().withoutPadding();
	private final Base64.Decoder base64UrlDecoder = Base64.getUrlDecoder();

	public JwtTokenProvider(AuthenticationProperties authenticationProperties, ObjectMapper objectMapper, Clock clock) {
		this.authenticationProperties = authenticationProperties;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	public String generateAccessToken(User user, List<String> roles) {
		Instant issuedAt = Instant.now(clock);
		Instant expiresAt = issuedAt.plusSeconds(authenticationProperties.getJwt().getAccessTokenExpirySeconds());

		Map<String, Object> claims = new LinkedHashMap<>();
		claims.put(CLAIM_SUBJECT, String.valueOf(user.getId()));
		claims.put(CLAIM_EMAIL, user.getEmail());
		claims.put(CLAIM_USER_TYPE, user.getUserType().name());
		claims.put(CLAIM_ROLES, roles == null ? List.of() : List.copyOf(roles));
		claims.put(CLAIM_TOKEN_VERSION, user.getTokenVersion());
		claims.put(CLAIM_ISSUED_AT, issuedAt.getEpochSecond());
		claims.put(CLAIM_EXPIRES_AT, expiresAt.getEpochSecond());
		claims.put(CLAIM_JWT_ID, UUID.randomUUID().toString());

		return buildToken(claims);
	}

	public boolean validateToken(String token) {
		try {
			Map<String, Object> claims = extractClaims(token);
			return getExpiration(claims).isAfter(Instant.now(clock));
		}
		catch (RuntimeException ex) {
			return false;
		}
	}

	public Map<String, Object> extractClaims(String token) {
		String[] tokenParts = splitToken(token);
		verifySignature(tokenParts);
		return decodeJson(tokenParts[1]);
	}

	public Long extractUserId(String token) {
		return Long.valueOf(String.valueOf(extractClaims(token).get(CLAIM_SUBJECT)));
	}

	public String extractEmail(String token) {
		return String.valueOf(extractClaims(token).get(CLAIM_EMAIL));
	}

	public String extractUserType(String token) {
		return String.valueOf(extractClaims(token).get(CLAIM_USER_TYPE));
	}

	public List<String> extractRoles(String token) {
		Object roles = extractClaims(token).get(CLAIM_ROLES);
		if (roles instanceof List<?> roleList) {
			return roleList.stream().map(String::valueOf).toList();
		}
		return List.of();
	}

	public long extractTokenVersion(String token) {
		Object tokenVersion = extractClaims(token).get(CLAIM_TOKEN_VERSION);
		if (tokenVersion instanceof Number number) {
			return number.longValue();
		}
		return Long.parseLong(String.valueOf(tokenVersion));
	}

	public Instant extractExpiration(String token) {
		return getExpiration(extractClaims(token));
	}

	private String buildToken(Map<String, Object> claims) {
		Map<String, Object> header = Map.of(HEADER_ALGORITHM, ALGORITHM, HEADER_TYPE, TOKEN_TYPE);
		String encodedHeader = encodeJson(header);
		String encodedClaims = encodeJson(claims);
		String unsignedToken = encodedHeader + TOKEN_SEPARATOR + encodedClaims;
		return unsignedToken + TOKEN_SEPARATOR + sign(unsignedToken);
	}

	private void verifySignature(String[] tokenParts) {
		String unsignedToken = tokenParts[0] + TOKEN_SEPARATOR + tokenParts[1];
		String expectedSignature = sign(unsignedToken);
		if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
				tokenParts[2].getBytes(StandardCharsets.UTF_8))) {
			throw new IllegalArgumentException("Invalid JWT signature");
		}
	}

	private String[] splitToken(String token) {
		if (!StringUtils.hasText(token)) {
			throw new IllegalArgumentException("JWT token is required");
		}
		String[] tokenParts = token.split("\\.");
		if (tokenParts.length != 3) {
			throw new IllegalArgumentException("Invalid JWT format");
		}
		return tokenParts;
	}

	private String encodeJson(Map<String, Object> value) {
		try {
			return base64UrlEncoder.encodeToString(objectMapper.writeValueAsBytes(value));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to encode JWT JSON", ex);
		}
	}

	private Map<String, Object> decodeJson(String encodedJson) {
		try {
			byte[] decodedJson = base64UrlDecoder.decode(encodedJson);
			return objectMapper.readValue(decodedJson, CLAIM_MAP_TYPE);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Unable to decode JWT JSON", ex);
		}
	}

	private String sign(String unsignedToken) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(jwtSecret(), HMAC_ALGORITHM));
			return base64UrlEncoder.encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to sign JWT", ex);
		}
	}

	private byte[] jwtSecret() {
		String secret = authenticationProperties.getJwt().getSecret();
		if (!StringUtils.hasText(secret)) {
			throw new IllegalStateException("JWT secret must be configured");
		}
		byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (secretBytes.length < MIN_SECRET_BYTES) {
			throw new IllegalStateException("JWT secret must be at least 256 bits");
		}
		return secretBytes;
	}

	private Instant getExpiration(Map<String, Object> claims) {
		Object expiresAt = claims.get(CLAIM_EXPIRES_AT);
		if (expiresAt instanceof Number number) {
			return Instant.ofEpochSecond(number.longValue());
		}
		return Instant.ofEpochSecond(Long.parseLong(String.valueOf(expiresAt)));
	}
}
