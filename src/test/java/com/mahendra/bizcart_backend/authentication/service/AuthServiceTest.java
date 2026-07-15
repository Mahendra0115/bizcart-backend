package com.mahendra.bizcart_backend.authentication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mahendra.bizcart_backend.authentication.config.AuthenticationProperties;
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
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class AuthServiceTest {

	private static final Instant NOW = Instant.parse("2026-07-15T00:00:00Z");
	private static final String HASH_SECRET = "test-token-hash-0123456789abcdef0123456789abcdef";

	private AuthenticationProperties authenticationProperties;
	private FakeRefreshTokenRepository refreshTokenRepository;
	private AuthService authService;

	@BeforeEach
	void setUp() {
		authenticationProperties = new AuthenticationProperties();
		authenticationProperties.getJwt().setSecret("test-only-0123456789abcdef0123456789abcdef");
		authenticationProperties.getJwt().setAccessTokenExpirySeconds(900);
		authenticationProperties.getJwt().setRefreshTokenExpirySeconds(604800);
		authenticationProperties.getTokenHash().setSecret(HASH_SECRET);
		refreshTokenRepository = new FakeRefreshTokenRepository();
		RoleRepository roleRepository = roleRepository(List.of(role(AppConstants.RoleNames.CUSTOMER)));
		JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(authenticationProperties, new ObjectMapper(),
				Clock.fixed(NOW, ZoneOffset.UTC));
		authService = new AuthService(refreshTokenRepository.proxy(), roleRepository, jwtTokenProvider,
				authenticationProperties, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void refreshAccessTokenRotatesRefreshToken() {
		User user = user(AccountStatus.ACTIVE, true);
		RefreshToken currentToken = refreshToken("raw-refresh", user, NOW.plusSeconds(60), null);
		refreshTokenRepository.token = currentToken;

		RefreshTokenResult result = authService.refreshAccessToken("raw-refresh", "127.0.0.1", "JUnit");

		assertThat(result.response().accessToken()).isNotBlank();
		assertThat(result.refreshToken()).isNotBlank();
		assertThat(currentToken.getRevokedAt()).isNotNull();
		assertThat(currentToken.getRevocationReason()).isEqualTo(RefreshTokenRevocationReason.ROTATED);
		assertThat(refreshTokenRepository.savedToken).isNotNull();
		assertThat(refreshTokenRepository.savedToken.getTokenHash()).hasSize(64);
		assertThat(refreshTokenRepository.savedToken.getTokenHash()).isNotEqualTo(result.refreshToken());
		assertThat(refreshTokenRepository.savedToken.getTokenFamilyId()).isEqualTo("family-1");
		assertThat(refreshTokenRepository.savedToken.getParentToken()).isEqualTo(currentToken);
		assertThat(currentToken.getReplacedByToken()).isEqualTo(refreshTokenRepository.savedToken);
	}

	@Test
	void refreshAccessTokenRejectsExpiredToken() {
		refreshTokenRepository.token = refreshToken("raw-refresh", user(AccountStatus.ACTIVE, true),
				NOW.minusSeconds(1), null);

		assertThatThrownBy(() -> authService.refreshAccessToken("raw-refresh", "127.0.0.1", "JUnit"))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining(AppConstants.Auth.REFRESH_TOKEN_EXPIRED);
	}

	@Test
	void refreshAccessTokenRejectsRevokedTokenAndRevokesFamily() {
		refreshTokenRepository.token = refreshToken("raw-refresh", user(AccountStatus.ACTIVE, true), NOW.plusSeconds(60),
				LocalDateTime.ofInstant(NOW.minusSeconds(10), ZoneOffset.UTC));

		assertThatThrownBy(() -> authService.refreshAccessToken("raw-refresh", "127.0.0.1", "JUnit"))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining(AppConstants.Auth.REFRESH_TOKEN_REVOKED);

		assertThat(refreshTokenRepository.revokedFamilyId).isEqualTo("family-1");
		assertThat(refreshTokenRepository.revocationReason).isEqualTo(RefreshTokenRevocationReason.TOKEN_REUSE_DETECTED);
	}

	@Test
	void logoutRevokesActiveTokenByHash() {
		refreshTokenRepository.revokeByHashCount = 1;

		authService.logout("raw-refresh");

		assertThat(refreshTokenRepository.revokedTokenHash).isEqualTo(hash("raw-refresh"));
		assertThat(refreshTokenRepository.revocationReason).isEqualTo(RefreshTokenRevocationReason.LOGOUT);
	}

	@Test
	void logoutRejectsInvalidOrAlreadyRevokedToken() {
		refreshTokenRepository.revokeByHashCount = 0;

		assertThatThrownBy(() -> authService.logout("raw-refresh"))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining(AppConstants.Auth.INVALID_REFRESH_TOKEN);
	}

	private RefreshToken refreshToken(String rawToken, User user, Instant expiresAt, LocalDateTime revokedAt) {
		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setUser(user);
		refreshToken.setTokenHash(hash(rawToken));
		refreshToken.setTokenFamilyId("family-1");
		refreshToken.setExpiresAt(LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC));
		refreshToken.setRevokedAt(revokedAt);
		return refreshToken;
	}

	private User user(AccountStatus status, boolean emailVerified) {
		User user = new User();
		ReflectionTestUtils.setField(user, "id", 7L);
		user.setFirstName("Test");
		user.setLastName("User");
		user.setEmail("customer@example.com");
		user.setPassword("hashed-password");
		user.setUserType(UserType.CUSTOMER);
		user.setStatus(status);
		user.setEmailVerified(emailVerified);
		user.setTokenVersion(1L);
		return user;
	}

	private Role role(String name) {
		Role role = new Role();
		role.setName(name);
		return role;
	}

	private RoleRepository roleRepository(List<Role> roles) {
		return repositoryProxy(RoleRepository.class, invocation -> {
			if ("findByUserId".equals(invocation.methodName())) {
				return roles;
			}
			throw unsupported(invocation.methodName());
		});
	}

	private String hash(String token) {
		try {
			Mac mac = Mac.getInstance(AppConstants.Auth.HMAC_SHA_256);
			mac.init(new SecretKeySpec(HASH_SECRET.getBytes(StandardCharsets.UTF_8), AppConstants.Auth.HMAC_SHA_256));
			return HexFormat.of().formatHex(mac.doFinal(token.getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private <T> T repositoryProxy(Class<T> repositoryType, RepositoryInvocationHandler invocationHandler) {
		Object proxy = Proxy.newProxyInstance(repositoryType.getClassLoader(), new Class<?>[] { repositoryType },
				(proxyObject, method, args) -> {
					if ("toString".equals(method.getName())) {
						return repositoryType.getSimpleName() + "Proxy";
					}
					return invocationHandler.invoke(new RepositoryInvocation(method.getName(), args));
				});
		return repositoryType.cast(proxy);
	}

	private UnsupportedOperationException unsupported(String methodName) {
		return new UnsupportedOperationException(methodName + " is not needed by this unit test");
	}

	private record RepositoryInvocation(String methodName, Object[] args) {
	}

	@FunctionalInterface
	private interface RepositoryInvocationHandler {

		Object invoke(RepositoryInvocation invocation);
	}

	private class FakeRefreshTokenRepository {

		private RefreshToken token;
		private RefreshToken savedToken;
		private String revokedTokenHash;
		private String revokedFamilyId;
		private RefreshTokenRevocationReason revocationReason;
		private int revokeByHashCount;

		private RefreshTokenRepository proxy() {
			return repositoryProxy(RefreshTokenRepository.class, invocation -> {
				if ("findByTokenHashForUpdate".equals(invocation.methodName())) {
					String tokenHash = (String) invocation.args()[0];
					return token != null && token.getTokenHash().equals(tokenHash) ? Optional.of(token) : Optional.empty();
				}
				if ("save".equals(invocation.methodName())) {
					savedToken = (RefreshToken) invocation.args()[0];
					return savedToken;
				}
				if ("revokeActiveTokenByHash".equals(invocation.methodName())) {
					revokedTokenHash = (String) invocation.args()[0];
					revocationReason = (RefreshTokenRevocationReason) invocation.args()[2];
					return revokeByHashCount;
				}
				if ("revokeActiveTokensByFamilyId".equals(invocation.methodName())) {
					revokedFamilyId = (String) invocation.args()[0];
					revocationReason = (RefreshTokenRevocationReason) invocation.args()[2];
					return 1;
				}
				throw unsupported(invocation.methodName());
			});
		}
	}
}
