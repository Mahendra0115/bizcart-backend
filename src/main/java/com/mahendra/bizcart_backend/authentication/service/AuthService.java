package com.mahendra.bizcart_backend.authentication.service;

import com.mahendra.bizcart_backend.authentication.config.AuthenticationProperties;
import com.mahendra.bizcart_backend.authentication.dto.request.LoginRequestDto;
import com.mahendra.bizcart_backend.authentication.dto.request.RegisterRequestDto;
import com.mahendra.bizcart_backend.authentication.dto.response.CurrentUserResponseDto;
import com.mahendra.bizcart_backend.authentication.dto.response.LoginResponseDto;
import com.mahendra.bizcart_backend.authentication.dto.response.RegisterResponseDto;
import com.mahendra.bizcart_backend.authentication.dto.response.TokenResponseDto;
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
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

	private static final int REFRESH_TOKEN_BYTES = 64;

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PermissionRepository permissionRepository;
	private final UserRoleRepository userRoleRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final LoginAttemptRecorder loginAttemptRecorder;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final AuthenticationProperties authenticationProperties;
	private final Clock clock;
	private final SecureRandom secureRandom = new SecureRandom();

	public AuthService(UserRepository userRepository, RoleRepository roleRepository,
			PermissionRepository permissionRepository, UserRoleRepository userRoleRepository,
			RefreshTokenRepository refreshTokenRepository, LoginAttemptRecorder loginAttemptRecorder,
			PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider,
			AuthenticationProperties authenticationProperties, Clock clock) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.permissionRepository = permissionRepository;
		this.userRoleRepository = userRoleRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.loginAttemptRecorder = loginAttemptRecorder;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
		this.authenticationProperties = authenticationProperties;
		this.clock = clock;
	}

	@Transactional
	public RegisterResponseDto register(RegisterRequestDto request) {
		String normalizedEmail = normalizeEmail(request.getEmail());
		String normalizedUsername = normalizeUsername(request.getUsername());
		validateRegistrationUniqueness(normalizedEmail, normalizedUsername);

		User user = new User();
		user.setFirstName(request.getFirstName().trim());
		user.setLastName(request.getLastName().trim());
		user.setUsername(normalizedUsername);
		user.setEmail(normalizedEmail);
		user.setPhone(normalizeNullable(request.getPhone()));
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setUserType(request.getUserType());
		user.setStatus(AccountStatus.PENDING);
		user.setEmailVerified(false);
		user.setAdminApproved(false);
		user.setTokenVersion(0L);

		User savedUser = userRepository.save(user);
		assignDefaultRole(savedUser, request.getUserType());
		return toRegisterResponse(savedUser);
	}

	@Transactional
	public LoginResult login(LoginRequestDto request, String ipAddress, String userAgent) {
		String normalizedEmail = normalizeEmail(request.getEmail());
		User user = userRepository.findByNormalizedEmail(normalizedEmail).orElse(null);
		if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			recordLoginAttempt(null, normalizedEmail, ipAddress, userAgent, false, AppConstants.Auth.INVALID_CREDENTIALS);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, AppConstants.Auth.INVALID_CREDENTIALS);
		}

		validateLoginAllowed(user, ipAddress, userAgent);
		user.setLastLoginAt(LocalDateTime.now(clock));
		recordLoginAttempt(user, normalizedEmail, ipAddress, userAgent, true, null);

		List<String> roles = roleRepository.findByUserId(user.getId()).stream().map(Role::getName).toList();
		String accessToken = jwtTokenProvider.generateAccessToken(user, roles);
		String refreshToken = generateRefreshToken();
		refreshTokenRepository.save(createRefreshToken(user, refreshToken, UUID.randomUUID().toString(), null, ipAddress,
				userAgent));

		LoginResponseDto response = new LoginResponseDto(accessToken,
				authenticationProperties.getJwt().getAccessTokenExpirySeconds(),
				authenticationProperties.getJwt().getRefreshTokenExpirySeconds(), toCurrentUserResponse(user));
		return new LoginResult(response, refreshToken);
	}

	@Transactional
	public RefreshTokenResult refreshAccessToken(String rawRefreshToken, String ipAddress, String userAgent) {
		RefreshToken currentToken = findRefreshTokenForUpdate(rawRefreshToken);
		LocalDateTime now = LocalDateTime.now(clock);
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
		LocalDateTime now = LocalDateTime.now(clock);
		int revokedTokens = refreshTokenRepository.revokeActiveTokenByHash(tokenHash, now,
				RefreshTokenRevocationReason.LOGOUT, now);
		if (revokedTokens == 0) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, AppConstants.Auth.INVALID_REFRESH_TOKEN);
		}
	}

	@Transactional
	public void logoutAll(Long userId) {
		LocalDateTime now = LocalDateTime.now(clock);
		refreshTokenRepository.revokeActiveTokensByUserId(userId, now, RefreshTokenRevocationReason.LOGOUT_ALL, now);
	}

	@Transactional(readOnly = true)
	public CurrentUserResponseDto currentUser(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, AppConstants.Auth.INVALID_CREDENTIALS));
		validateCurrentUserAllowed(user);
		return toCurrentUserResponse(user);
	}

	private void validateRegistrationUniqueness(String normalizedEmail, String normalizedUsername) {
		if (userRepository.existsByEmail(normalizedEmail)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, AppConstants.Auth.DUPLICATE_EMAIL);
		}
		if (userRepository.existsByUsername(normalizedUsername)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, AppConstants.Auth.DUPLICATE_USERNAME);
		}
	}

	private void assignDefaultRole(User user, UserType userType) {
		String roleName = userType == UserType.SELLER ? AppConstants.RoleNames.SELLER : AppConstants.RoleNames.CUSTOMER;
		Role role = roleRepository.findByName(roleName).orElseGet(() -> createRole(roleName));
		UserRole userRole = new UserRole();
		userRole.setUser(user);
		userRole.setRole(role);
		userRoleRepository.save(userRole);
	}

	private Role createRole(String roleName) {
		Role role = new Role();
		role.setName(roleName);
		role.setDescription(AppConstants.Auth.DEFAULT_ROLE_DESCRIPTION);
		return roleRepository.save(role);
	}

	private void validateLoginAllowed(User user, String ipAddress, String userAgent) {
		if (user.getStatus() != AccountStatus.ACTIVE) {
			recordLoginAttempt(user, user.getEmail(), ipAddress, userAgent, false, AppConstants.Auth.ACCOUNT_NOT_ACTIVE);
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, AppConstants.Auth.ACCOUNT_NOT_ACTIVE);
		}
		if (!user.isEmailVerified()) {
			recordLoginAttempt(user, user.getEmail(), ipAddress, userAgent, false, AppConstants.Auth.EMAIL_NOT_VERIFIED);
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, AppConstants.Auth.EMAIL_NOT_VERIFIED);
		}
		if (user.getUserType() == UserType.SELLER && !user.isAdminApproved()) {
			recordLoginAttempt(user, user.getEmail(), ipAddress, userAgent, false, AppConstants.Auth.SELLER_NOT_APPROVED);
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, AppConstants.Auth.SELLER_NOT_APPROVED);
		}
	}

	private void validateCurrentUserAllowed(User user) {
		if (user.getStatus() != AccountStatus.ACTIVE || !user.isEmailVerified()
				|| (user.getUserType() == UserType.SELLER && !user.isAdminApproved())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, AppConstants.Auth.INVALID_CREDENTIALS);
		}
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
		refreshToken.setTokenFamilyId(tokenFamilyId);
		refreshToken.setParentToken(parentToken);
		refreshToken.setIpAddress(normalizeNullable(ipAddress));
		refreshToken.setDeviceInfo(normalizeNullable(userAgent));
		refreshToken.setExpiresAt(LocalDateTime.ofInstant(Instant.now(clock)
			.plusSeconds(authenticationProperties.getJwt().getRefreshTokenExpirySeconds()), clock.getZone()));
		return refreshToken;
	}

	private void recordLoginAttempt(User user, String email, String ipAddress, String userAgent, boolean successful,
			String failureReason) {
		LoginAttempt loginAttempt = new LoginAttempt();
		loginAttempt.setUser(user);
		loginAttempt.setEmail(email);
		loginAttempt.setIpAddress(normalizeNullable(ipAddress));
		loginAttempt.setUserAgent(normalizeNullable(userAgent));
		loginAttempt.setWasSuccessful(successful);
		loginAttempt.setFailureReason(failureReason);
		loginAttempt.setAttemptedAt(LocalDateTime.now(clock));
		loginAttemptRecorder.record(loginAttempt);
	}

	private CurrentUserResponseDto toCurrentUserResponse(User user) {
		List<String> roles = roleRepository.findByUserId(user.getId()).stream().map(Role::getName).toList();
		List<String> permissions = permissionRepository.findByUserId(user.getId())
			.stream()
			.map(Permission::getName)
			.toList();
		return new CurrentUserResponseDto(user.getId(), user.getFirstName(), user.getLastName(), user.getUsername(),
				user.getEmail(), user.getPhone(), user.getProfileImage(), user.getUserType(), user.getStatus(),
				user.isEmailVerified(), roles, permissions);
	}

	private RegisterResponseDto toRegisterResponse(User user) {
		return new RegisterResponseDto(user.getId(), user.getFirstName(), user.getLastName(), user.getUsername(),
				user.getEmail(), user.getPhone(), user.getUserType(), user.getStatus(), user.isEmailVerified());
	}

	private String generateRefreshToken() {
		byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hashToken(String token) {
		if (!StringUtils.hasText(token)) {
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

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private String normalizeUsername(String username) {
		return username.trim().toLowerCase(Locale.ROOT);
	}

	private String normalizeNullable(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
