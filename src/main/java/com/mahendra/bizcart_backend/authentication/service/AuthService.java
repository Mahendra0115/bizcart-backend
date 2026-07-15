package com.mahendra.bizcart_backend.authentication.service;

import com.mahendra.bizcart_backend.authentication.config.AuthenticationProperties;
import com.mahendra.bizcart_backend.authentication.dto.response.TokenResponseDto;
import com.mahendra.bizcart_backend.authentication.entity.RefreshToken;
import com.mahendra.bizcart_backend.authentication.entity.RefreshTokenRevocationReason;
import com.mahendra.bizcart_backend.authentication.repository.RefreshTokenRepository;
import com.mahendra.bizcart_backend.authentication.security.JwtTokenProvider;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.user.entity.Role;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import com.mahendra.bizcart_backend.user.repository.RoleRepository;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

	private static final int REFRESH_TOKEN_BYTES = 64;

	private final RefreshTokenRepository refreshTokenRepository;
	private final RoleRepository roleRepository;
	private final JwtTokenProvider jwtTokenProvider;
	private final AuthenticationProperties authenticationProperties;
	private final Clock clock;
	private final SecureRandom secureRandom = new SecureRandom();

	public AuthService(RefreshTokenRepository refreshTokenRepository, RoleRepository roleRepository,
			JwtTokenProvider jwtTokenProvider, AuthenticationProperties authenticationProperties, Clock clock) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.roleRepository = roleRepository;
		this.jwtTokenProvider = jwtTokenProvider;
		this.authenticationProperties = authenticationProperties;
		this.clock = clock;
	}

	@Transactional
	public RefreshTokenResult refreshAccessToken(String rawRefreshToken, String ipAddress, String userAgent) {
		RefreshToken currentToken = findRefreshTokenForUpdate(rawRefreshToken);
		LocalDateTime now = now();
		validateRefreshToken(currentToken, now);

		User user = currentToken.getUser();
		validateRefreshAllowed(user);

		String newRawRefreshToken = generateRefreshToken();
		RefreshToken newRefreshToken = createRefreshToken(user, newRawRefreshToken, currentToken.getTokenFamilyId(),
				currentToken, ipAddress, userAgent);
		refreshTokenRepository.save(newRefreshToken);

		currentToken.setRevokedAt(now);
		currentToken.setRevocationReason(RefreshTokenRevocationReason.ROTATED);
		currentToken.setReplacedByToken(newRefreshToken);

		List<String> roles = roleRepository.findByUserId(user.getId()).stream().map(Role::getName).toList();
		String accessToken = jwtTokenProvider.generateAccessToken(user, roles);
		TokenResponseDto response = new TokenResponseDto(accessToken,
				authenticationProperties.getJwt().getAccessTokenExpirySeconds(),
				authenticationProperties.getJwt().getRefreshTokenExpirySeconds());
		return new RefreshTokenResult(response, newRawRefreshToken);
	}

	@Transactional
	public void logout(String rawRefreshToken) {
		String tokenHash = hashToken(rawRefreshToken);
		LocalDateTime now = now();
		int revokedTokens = refreshTokenRepository.revokeActiveTokenByHash(tokenHash, now,
				RefreshTokenRevocationReason.LOGOUT, now);
		if (revokedTokens == 0) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, AppConstants.Auth.INVALID_REFRESH_TOKEN);
		}
	}

	@Transactional
	public void logoutAll(Long userId) {
		refreshTokenRepository.revokeActiveTokensByUserId(userId, now(), RefreshTokenRevocationReason.LOGOUT_ALL, now());
	}

	private RefreshToken findRefreshTokenForUpdate(String rawRefreshToken) {
		return refreshTokenRepository.findByTokenHashForUpdate(hashToken(rawRefreshToken))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
					AppConstants.Auth.INVALID_REFRESH_TOKEN));
	}

	private void validateRefreshToken(RefreshToken refreshToken, LocalDateTime now) {
		if (refreshToken.getRevokedAt() != null) {
			refreshTokenRepository.revokeActiveTokensByFamilyId(refreshToken.getTokenFamilyId(), now,
					RefreshTokenRevocationReason.TOKEN_REUSE_DETECTED, now);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, AppConstants.Auth.REFRESH_TOKEN_REVOKED);
		}
		if (!refreshToken.getExpiresAt().isAfter(now)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, AppConstants.Auth.REFRESH_TOKEN_EXPIRED);
		}
	}

	private void validateRefreshAllowed(User user) {
		if (user.getStatus() != AccountStatus.ACTIVE) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, AppConstants.Auth.ACCOUNT_NOT_ACTIVE);
		}
		if (!user.isEmailVerified()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, AppConstants.Auth.EMAIL_NOT_VERIFIED);
		}
		if (user.getUserType() == UserType.SELLER && !user.isAdminApproved()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, AppConstants.Auth.SELLER_NOT_APPROVED);
		}
	}

	private RefreshToken createRefreshToken(User user, String rawRefreshToken, String tokenFamilyId,
			RefreshToken parentToken, String ipAddress, String userAgent) {
		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setUser(user);
		refreshToken.setTokenHash(hashToken(rawRefreshToken));
		refreshToken.setTokenFamilyId(tokenFamilyId == null ? UUID.randomUUID().toString() : tokenFamilyId);
		refreshToken.setParentToken(parentToken);
		refreshToken.setIpAddress(normalizeNullable(ipAddress));
		refreshToken.setDeviceInfo(normalizeNullable(userAgent));
		refreshToken.setExpiresAt(LocalDateTime.ofInstant(Instant.now(clock)
			.plusSeconds(authenticationProperties.getJwt().getRefreshTokenExpirySeconds()), clock.getZone()));
		return refreshToken;
	}

	private String generateRefreshToken() {
		byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hashToken(String token) {
		if (token == null || token.isBlank()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, AppConstants.Auth.INVALID_REFRESH_TOKEN);
		}
		try {
			Mac mac = Mac.getInstance(AppConstants.Auth.HMAC_SHA_256);
			mac.init(new SecretKeySpec(authenticationProperties.getTokenHash().getSecret().getBytes(StandardCharsets.UTF_8),
					AppConstants.Auth.HMAC_SHA_256));
			return HexFormat.of().formatHex(mac.doFinal(token.getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to hash token", ex);
		}
	}

	private LocalDateTime now() {
		return LocalDateTime.now(clock);
	}

	private String normalizeNullable(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
