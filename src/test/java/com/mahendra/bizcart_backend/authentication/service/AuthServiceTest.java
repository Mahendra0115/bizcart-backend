package com.mahendra.bizcart_backend.authentication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mahendra.bizcart_backend.authentication.config.AuthenticationProperties;
import com.mahendra.bizcart_backend.authentication.dto.request.LoginRequestDto;
import com.mahendra.bizcart_backend.authentication.dto.request.RegisterRequestDto;
import com.mahendra.bizcart_backend.authentication.dto.response.CurrentUserResponseDto;
import com.mahendra.bizcart_backend.authentication.dto.response.RegisterResponseDto;
import com.mahendra.bizcart_backend.authentication.entity.LoginAttempt;
import com.mahendra.bizcart_backend.authentication.entity.RefreshToken;
import com.mahendra.bizcart_backend.authentication.entity.RefreshTokenRevocationReason;
import com.mahendra.bizcart_backend.authentication.repository.RefreshTokenRepository;
import com.mahendra.bizcart_backend.authentication.security.JwtTokenProvider;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.user.entity.Permission;
import com.mahendra.bizcart_backend.user.entity.Role;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.entity.UserRole;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import com.mahendra.bizcart_backend.user.repository.PermissionRepository;
import com.mahendra.bizcart_backend.user.repository.RoleRepository;
import com.mahendra.bizcart_backend.user.repository.UserRepository;
import com.mahendra.bizcart_backend.user.repository.UserRoleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");

	@Mock
	private UserRepository userRepository;

	@Mock
	private RoleRepository roleRepository;

	@Mock
	private PermissionRepository permissionRepository;

	@Mock
	private UserRoleRepository userRoleRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private RefreshTokenRevocationService refreshTokenRevocationService;

	@Mock
	private LoginAttemptRecorder loginAttemptRecorder;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		AuthenticationProperties authenticationProperties = new AuthenticationProperties();
		authenticationProperties.getJwt().setAccessTokenExpirySeconds(900);
		authenticationProperties.getJwt().setRefreshTokenExpirySeconds(604800);
		authenticationProperties.getTokenHash().setSecret("test-token-hash-0123456789abcdef0123456789abcdef");
		authService = new AuthService(userRepository, roleRepository, permissionRepository, userRoleRepository,
				refreshTokenRepository, refreshTokenRevocationService, loginAttemptRecorder, passwordEncoder,
				jwtTokenProvider, authenticationProperties, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void registerCreatesPendingUserWithHashedPasswordAndDefaultRole() {
		RegisterRequestDto request = registerRequest();
		Role customerRole = role(12L, AppConstants.RoleNames.CUSTOMER);
		when(userRepository.existsByEmail("customer@example.com")).thenReturn(false);
		when(userRepository.existsByUsername("customer-one")).thenReturn(false);
		when(passwordEncoder.encode("Password@123")).thenReturn("hashed-password");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 44L));
		when(roleRepository.findByName(AppConstants.RoleNames.CUSTOMER)).thenReturn(Optional.of(customerRole));

		RegisterResponseDto response = authService.register(request);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		User savedUser = userCaptor.getValue();
		assertThat(savedUser.getEmail()).isEqualTo("customer@example.com");
		assertThat(savedUser.getUsername()).isEqualTo("customer-one");
		assertThat(savedUser.getPassword()).isEqualTo("hashed-password");
		assertThat(savedUser.getStatus()).isEqualTo(AccountStatus.PENDING);
		assertThat(savedUser.isEmailVerified()).isFalse();
		assertThat(response.username()).isEqualTo("customer-one");

		ArgumentCaptor<UserRole> userRoleCaptor = ArgumentCaptor.forClass(UserRole.class);
		verify(userRoleRepository).save(userRoleCaptor.capture());
		assertThat(userRoleCaptor.getValue().getUser().getId()).isEqualTo(44L);
		assertThat(userRoleCaptor.getValue().getRole()).isEqualTo(customerRole);
	}

	@Test
	void registerRejectsDuplicateEmail() {
		RegisterRequestDto request = registerRequest();
		when(userRepository.existsByEmail("customer@example.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(request))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining(AppConstants.Auth.DUPLICATE_EMAIL);

		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void registerRejectsDuplicateUsername() {
		RegisterRequestDto request = registerRequest();
		when(userRepository.existsByEmail("customer@example.com")).thenReturn(false);
		when(userRepository.existsByUsername("customer-one")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(request))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining(AppConstants.Auth.DUPLICATE_USERNAME);
	}

	@Test
	void loginGeneratesAccessAndRefreshTokensForActiveVerifiedUser() {
		User user = user(55L, AccountStatus.ACTIVE, true);
		when(userRepository.findByNormalizedEmail("customer@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("Password@123", "hashed-password")).thenReturn(true);
		when(roleRepository.findByUserId(55L)).thenReturn(List.of(role(2L, AppConstants.RoleNames.CUSTOMER)));
		when(permissionRepository.findByUserId(55L)).thenReturn(List.of(permission("ORDER_READ")));
		when(jwtTokenProvider.generateAccessToken(eq(user), eq(List.of(AppConstants.RoleNames.CUSTOMER))))
			.thenReturn("access-token");

		LoginResult result = authService.login(loginRequest(), "127.0.0.1", "JUnit");

		assertThat(result.response().accessToken()).isEqualTo("access-token");
		assertThat(result.refreshToken()).isNotBlank();
		assertThat(result.response().user().roles()).containsExactly(AppConstants.RoleNames.CUSTOMER);
		assertThat(result.response().user().permissions()).containsExactly("ORDER_READ");
		assertThat(user.getLastLoginAt()).isNotNull();

		ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
		assertThat(refreshTokenCaptor.getValue().getTokenHash()).hasSize(64);
		assertThat(refreshTokenCaptor.getValue().getTokenHash()).isNotEqualTo(result.refreshToken());

		ArgumentCaptor<LoginAttempt> loginAttemptCaptor = ArgumentCaptor.forClass(LoginAttempt.class);
		verify(loginAttemptRecorder).record(loginAttemptCaptor.capture());
		assertThat(loginAttemptCaptor.getValue().isWasSuccessful()).isTrue();
	}

	@Test
	void loginRejectsInvalidPasswordAndRecordsFailure() {
		User user = user(55L, AccountStatus.ACTIVE, true);
		when(userRepository.findByNormalizedEmail("customer@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

		assertThatThrownBy(() -> authService.login(loginRequest("wrong-password"), "127.0.0.1", "JUnit"))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining(AppConstants.Auth.INVALID_CREDENTIALS);

		ArgumentCaptor<LoginAttempt> loginAttemptCaptor = ArgumentCaptor.forClass(LoginAttempt.class);
		verify(loginAttemptRecorder).record(loginAttemptCaptor.capture());
		assertThat(loginAttemptCaptor.getValue().isWasSuccessful()).isFalse();
	}

	@Test
	void loginRejectsInactiveAccount() {
		User user = user(55L, AccountStatus.BLOCKED, true);
		when(userRepository.findByNormalizedEmail("customer@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("Password@123", "hashed-password")).thenReturn(true);

		assertThatThrownBy(() -> authService.login(loginRequest(), "127.0.0.1", "JUnit"))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining(AppConstants.Auth.ACCOUNT_NOT_ACTIVE);
	}

	@Test
	void loginRejectsUnapprovedSeller() {
		User user = user(55L, AccountStatus.ACTIVE, true);
		user.setUserType(UserType.SELLER);
		user.setAdminApproved(false);
		when(userRepository.findByNormalizedEmail("customer@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("Password@123", "hashed-password")).thenReturn(true);

		assertThatThrownBy(() -> authService.login(loginRequest(), "127.0.0.1", "JUnit"))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining(AppConstants.Auth.SELLER_NOT_APPROVED);
	}

	@Test
	void currentUserReturnsRolesAndPermissions() {
		User user = user(55L, AccountStatus.ACTIVE, true);
		when(userRepository.findById(55L)).thenReturn(Optional.of(user));
		when(roleRepository.findByUserId(55L)).thenReturn(List.of(role(2L, AppConstants.RoleNames.CUSTOMER)));
		when(permissionRepository.findByUserId(55L)).thenReturn(List.of(permission("ORDER_READ")));

		CurrentUserResponseDto response = authService.currentUser(55L);

		assertThat(response.email()).isEqualTo("customer@example.com");
		assertThat(response.username()).isEqualTo("customer-one");
		assertThat(response.roles()).containsExactly(AppConstants.RoleNames.CUSTOMER);
		assertThat(response.permissions()).containsExactly("ORDER_READ");
	}

	@Test
	void refreshAccessTokenRotatesRefreshToken() {
		User user = user(55L, AccountStatus.ACTIVE, true);
		RefreshToken currentToken = refreshToken(user, NOW.plusSeconds(60), null);
		when(refreshTokenRepository.findByTokenHashForUpdate(any(String.class))).thenReturn(Optional.of(currentToken));
		when(roleRepository.findByUserId(55L)).thenReturn(List.of(role(2L, AppConstants.RoleNames.CUSTOMER)));
		when(jwtTokenProvider.generateAccessToken(eq(user), eq(List.of(AppConstants.RoleNames.CUSTOMER))))
			.thenReturn("new-access-token");

		RefreshTokenResult result = authService.refreshAccessToken("raw-refresh", "127.0.0.1", "JUnit");

		assertThat(result.response().accessToken()).isEqualTo("new-access-token");
		assertThat(result.refreshToken()).isNotBlank();
		assertThat(currentToken.getRevokedAt()).isNotNull();
		assertThat(currentToken.getRevocationReason()).isEqualTo(RefreshTokenRevocationReason.ROTATED);

		ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
		assertThat(refreshTokenCaptor.getValue().getTokenHash()).hasSize(64);
		assertThat(refreshTokenCaptor.getValue().getTokenHash()).isNotEqualTo(result.refreshToken());
		assertThat(refreshTokenCaptor.getValue().getTokenFamilyId()).isEqualTo("family-1");
		assertThat(refreshTokenCaptor.getValue().getParentToken()).isEqualTo(currentToken);
		assertThat(currentToken.getReplacedByToken()).isEqualTo(refreshTokenCaptor.getValue());
	}

	@Test
	void refreshAccessTokenRejectsExpiredToken() {
		when(refreshTokenRepository.findByTokenHashForUpdate(any(String.class)))
			.thenReturn(Optional.of(refreshToken(user(55L, AccountStatus.ACTIVE, true), NOW.minusSeconds(1), null)));

		assertThatThrownBy(() -> authService.refreshAccessToken("raw-refresh", "127.0.0.1", "JUnit"))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining(AppConstants.Auth.REFRESH_TOKEN_EXPIRED);
	}

	@Test
	void refreshAccessTokenRejectsRevokedTokenAndRevokesFamily() {
		RefreshToken refreshToken = refreshToken(user(55L, AccountStatus.ACTIVE, true), NOW.plusSeconds(60),
				LocalDateTime.ofInstant(NOW.minusSeconds(10), ZoneOffset.UTC));
		when(refreshTokenRepository.findByTokenHashForUpdate(any(String.class))).thenReturn(Optional.of(refreshToken));

		assertThatThrownBy(() -> authService.refreshAccessToken("raw-refresh", "127.0.0.1", "JUnit"))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining(AppConstants.Auth.REFRESH_TOKEN_REVOKED);

		verify(refreshTokenRevocationService).revokeActiveFamilyTokens(eq("family-1"), any(LocalDateTime.class),
				eq(RefreshTokenRevocationReason.TOKEN_REUSE_DETECTED));
	}

	@Test
	void logoutRevokesActiveRefreshToken() {
		authService.logout(55L, "raw-refresh");

		verify(refreshTokenRepository).revokeActiveTokenByUserIdAndHash(eq(55L), any(String.class),
				any(LocalDateTime.class), eq(RefreshTokenRevocationReason.LOGOUT), any(LocalDateTime.class));
	}

	@Test
	void logoutIgnoresInvalidOrAlreadyRevokedRefreshToken() {
		authService.logout(55L, "raw-refresh");

		verify(refreshTokenRepository).revokeActiveTokenByUserIdAndHash(eq(55L), any(String.class),
				any(LocalDateTime.class), eq(RefreshTokenRevocationReason.LOGOUT), any(LocalDateTime.class));
	}

	@Test
	void logoutIgnoresMissingRefreshToken() {
		authService.logout(55L, null);

		verify(refreshTokenRepository, never()).revokeActiveTokenByUserIdAndHash(any(), any(), any(), any(), any());
	}

	@Test
	void logoutUsesAuthenticatedUserOwnershipWhenRevokingRefreshToken() {
		authService.logout(99L, "raw-refresh");

		verify(refreshTokenRepository).revokeActiveTokenByUserIdAndHash(eq(99L), any(String.class),
				any(LocalDateTime.class), eq(RefreshTokenRevocationReason.LOGOUT), any(LocalDateTime.class));
	}

	@Test
	void logoutAllRevokesActiveUserRefreshTokens() {
		authService.logoutAll(55L);

		verify(refreshTokenRepository).revokeActiveTokensByUserId(eq(55L), any(LocalDateTime.class),
				eq(RefreshTokenRevocationReason.LOGOUT_ALL), any(LocalDateTime.class));
	}

	private RegisterRequestDto registerRequest() {
		RegisterRequestDto request = new RegisterRequestDto();
		request.setFirstName("Customer");
		request.setLastName("One");
		request.setUsername(" Customer-One ");
		request.setEmail(" Customer@Example.COM ");
		request.setPhone("+919999999999");
		request.setPassword("Password@123");
		request.setConfirmPassword("Password@123");
		request.setUserType(UserType.CUSTOMER);
		return request;
	}

	private LoginRequestDto loginRequest() {
		return loginRequest("Password@123");
	}

	private LoginRequestDto loginRequest(String password) {
		LoginRequestDto request = new LoginRequestDto();
		request.setEmail(" Customer@Example.COM ");
		request.setPassword(password);
		return request;
	}

	private User user(Long id, AccountStatus status, boolean emailVerified) {
		User user = new User();
		ReflectionTestUtils.setField(user, "id", id);
		user.setFirstName("Customer");
		user.setLastName("One");
		user.setUsername("customer-one");
		user.setEmail("customer@example.com");
		user.setPassword("hashed-password");
		user.setUserType(UserType.CUSTOMER);
		user.setStatus(status);
		user.setEmailVerified(emailVerified);
		user.setTokenVersion(1L);
		return user;
	}

	private RefreshToken refreshToken(User user, Instant expiresAt, LocalDateTime revokedAt) {
		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setUser(user);
		refreshToken.setTokenHash("hashed-refresh-token");
		refreshToken.setTokenFamilyId("family-1");
		refreshToken.setExpiresAt(LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC));
		refreshToken.setRevokedAt(revokedAt);
		return refreshToken;
	}

	private Role role(Long id, String name) {
		Role role = new Role();
		ReflectionTestUtils.setField(role, "id", id);
		role.setName(name);
		return role;
	}

	private Permission permission(String name) {
		Permission permission = new Permission();
		permission.setName(name);
		return permission;
	}

	private User withId(User user, Long id) {
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}
}
