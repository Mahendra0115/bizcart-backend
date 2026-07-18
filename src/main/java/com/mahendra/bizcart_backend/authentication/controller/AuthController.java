package com.mahendra.bizcart_backend.authentication.controller;

import com.mahendra.bizcart_backend.authentication.dto.request.LoginRequestDto;
import com.mahendra.bizcart_backend.authentication.dto.request.ForgotPasswordRequestDto;
import com.mahendra.bizcart_backend.authentication.dto.request.RegisterRequestDto;
import com.mahendra.bizcart_backend.authentication.dto.request.ResetPasswordRequestDto;
import com.mahendra.bizcart_backend.authentication.dto.request.VerifyEmailRequestDto;
import com.mahendra.bizcart_backend.authentication.dto.request.ResendVerificationRequestDto;
import com.mahendra.bizcart_backend.authentication.dto.request.ChangePasswordRequestDto;
import com.mahendra.bizcart_backend.authentication.dto.response.AuthResponseDto;
import com.mahendra.bizcart_backend.authentication.dto.response.CurrentUserResponseDto;
import com.mahendra.bizcart_backend.authentication.dto.response.CsrfTokenResponseDto;
import com.mahendra.bizcart_backend.authentication.dto.response.LoginResponseDto;
import com.mahendra.bizcart_backend.authentication.dto.response.MessageResponseDto;
import com.mahendra.bizcart_backend.authentication.dto.response.RegisterResponseDto;
import com.mahendra.bizcart_backend.authentication.dto.response.TokenResponseDto;
import com.mahendra.bizcart_backend.authentication.config.AuthenticationProperties;
import com.mahendra.bizcart_backend.authentication.security.AuthenticatedUserDetails;
import com.mahendra.bizcart_backend.authentication.service.LoginResult;
import com.mahendra.bizcart_backend.authentication.service.AuthService;
import com.mahendra.bizcart_backend.authentication.service.RefreshTokenResult;
import com.mahendra.bizcart_backend.authentication.service.AuthRateLimiter;
import com.mahendra.bizcart_backend.authentication.web.ClientIpResolver;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.beans.factory.annotation.Autowired;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(AppConstants.Auth.API_AUTH_BASE)
@Tag(name = "Authentication", description = "Registration, sessions, email verification, and password management")
public class AuthController {

	private static final String HEADER_USER_AGENT = "User-Agent";
	private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";
	private static final String FORWARDED_FOR_SEPARATOR = ",";

	private final AuthService authService;
	private final AuthenticationProperties authenticationProperties;
	@Autowired(required = false)
	private AuthRateLimiter rateLimiter;
	@Autowired(required = false)
	private ClientIpResolver clientIpResolver;

	public AuthController(AuthService authService, AuthenticationProperties authenticationProperties) {
		this.authService = authService;
		this.authenticationProperties = authenticationProperties;
		this.clientIpResolver = new ClientIpResolver(authenticationProperties);
	}

	@PostMapping(AppConstants.Auth.REGISTER_PATH)
	@Operation(summary = "Register a customer or seller")
	public AuthResponseDto<RegisterResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
		return new AuthResponseDto<>(AppConstants.Auth.REGISTER_SUCCESS, authService.register(request));
	}

	@PostMapping(AppConstants.Auth.LOGIN_PATH)
	@Operation(summary = "Log in and issue an access/refresh token pair")
	public AuthResponseDto<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request,
			HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
		rateLimit("login", request.getEmail() + ":" + clientIp(httpServletRequest),
				authenticationProperties.getRateLimit().getLogin());
		LoginResult loginResult = authService.login(request, clientIp(httpServletRequest),
				httpServletRequest.getHeader(HEADER_USER_AGENT));
		addRefreshTokenCookie(httpServletResponse, loginResult.refreshToken());
		return new AuthResponseDto<>(AppConstants.Auth.LOGIN_SUCCESS, loginResult.response());
	}

	@GetMapping(AppConstants.Auth.CURRENT_USER_PATH)
	@Operation(summary = "Get the authenticated user")
	public AuthResponseDto<CurrentUserResponseDto> currentUser(
			@AuthenticationPrincipal AuthenticatedUserDetails authenticatedUserDetails) {
		return new AuthResponseDto<>(AppConstants.Auth.CURRENT_USER_SUCCESS,
				authService.currentUser(authenticatedUserDetails.getId()));
	}

	@GetMapping(AppConstants.Auth.CSRF_PATH)
	@Operation(summary = "Get a CSRF token")
	public AuthResponseDto<CsrfTokenResponseDto> csrfToken(CsrfToken csrfToken) {
		return new AuthResponseDto<>(AppConstants.Auth.CSRF_TOKEN_SUCCESS,
				new CsrfTokenResponseDto(csrfToken.getHeaderName(), csrfToken.getParameterName(), csrfToken.getToken()));
	}

	@PostMapping(AppConstants.Auth.REFRESH_TOKEN_PATH)
	@Operation(summary = "Rotate a refresh token and issue a new access token")
	public AuthResponseDto<TokenResponseDto> refreshAccessToken(HttpServletRequest request,
			HttpServletResponse response) {
		rateLimit("refresh", clientIp(request), authenticationProperties.getRateLimit().getRefreshToken());
		RefreshTokenResult result = authService.refreshAccessToken(refreshToken(request), clientIp(request),
				request.getHeader(HEADER_USER_AGENT));
		addRefreshTokenCookie(response, result.refreshToken());
		return new AuthResponseDto<>(AppConstants.Auth.REFRESH_TOKEN_SUCCESS, result.response());
	}

	@PostMapping(AppConstants.Auth.LOGOUT_PATH)
	@Operation(summary = "Log out the current refresh-token session")
	public AuthResponseDto<MessageResponseDto> logout(
			@AuthenticationPrincipal AuthenticatedUserDetails authenticatedUserDetails,
			HttpServletRequest request, HttpServletResponse response) {
		authService.logout(authenticatedUserDetails.getId(), refreshToken(request));
		clearRefreshTokenCookie(response);
		return new AuthResponseDto<>(AppConstants.Auth.LOGOUT_SUCCESS,
				new MessageResponseDto(AppConstants.Auth.LOGOUT_SUCCESS));
	}

	@PostMapping(AppConstants.Auth.LOGOUT_ALL_PATH)
	@Operation(summary = "Log out all sessions")
	public AuthResponseDto<MessageResponseDto> logoutAll(
			@AuthenticationPrincipal AuthenticatedUserDetails authenticatedUserDetails, HttpServletResponse response) {
		authService.logoutAll(authenticatedUserDetails.getId());
		clearRefreshTokenCookie(response);
		return new AuthResponseDto<>(AppConstants.Auth.LOGOUT_ALL_SUCCESS,
				new MessageResponseDto(AppConstants.Auth.LOGOUT_ALL_SUCCESS));
	}

	@PostMapping(AppConstants.Auth.FORGOT_PASSWORD_PATH)
	@Operation(summary = "Request a password-reset link")
	public AuthResponseDto<MessageResponseDto> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request,
			HttpServletRequest httpRequest) {
		rateLimit("forgot", request.getEmail() + ":" + clientIp(httpRequest),
				authenticationProperties.getRateLimit().getForgotPassword());
		authService.forgotPassword(request);
		return new AuthResponseDto<>(AppConstants.Auth.FORGOT_PASSWORD_SUCCESS,
				new MessageResponseDto(AppConstants.Auth.FORGOT_PASSWORD_SUCCESS));
	}

	@PostMapping(AppConstants.Auth.RESET_PASSWORD_PATH)
	@Operation(summary = "Reset a password with a one-time token")
	public AuthResponseDto<MessageResponseDto> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request,
			HttpServletRequest httpRequest) {
		rateLimit("reset", clientIp(httpRequest), authenticationProperties.getRateLimit().getResetPassword());
		authService.resetPassword(request);
		return new AuthResponseDto<>(AppConstants.Auth.RESET_PASSWORD_SUCCESS,
				new MessageResponseDto(AppConstants.Auth.RESET_PASSWORD_SUCCESS));
	}

	@PostMapping(AppConstants.Auth.VERIFY_EMAIL_PATH)
	@Operation(summary = "Verify an email address with a one-time token")
	public AuthResponseDto<MessageResponseDto> verifyEmail(@Valid @RequestBody VerifyEmailRequestDto request) {
		authService.verifyEmail(request.token());
		return new AuthResponseDto<>(AppConstants.Auth.VERIFY_EMAIL_SUCCESS,
				new MessageResponseDto(AppConstants.Auth.VERIFY_EMAIL_SUCCESS));
	}

	@PostMapping(AppConstants.Auth.RESEND_VERIFICATION_PATH)
	@Operation(summary = "Resend an email-verification link")
	public AuthResponseDto<MessageResponseDto> resendVerification(
			@Valid @RequestBody ResendVerificationRequestDto request, HttpServletRequest httpRequest) {
		rateLimit("resend", request.email() + ":" + clientIp(httpRequest),
				authenticationProperties.getRateLimit().getResendVerification());
		authService.resendVerification(request.email());
		return new AuthResponseDto<>(AppConstants.Auth.RESEND_VERIFICATION_SUCCESS,
				new MessageResponseDto(AppConstants.Auth.RESEND_VERIFICATION_SUCCESS));
	}

	@PutMapping(AppConstants.Auth.CHANGE_PASSWORD_PATH)
	@Operation(summary = "Change the authenticated user's password and revoke all sessions")
	public AuthResponseDto<MessageResponseDto> changePassword(
			@AuthenticationPrincipal AuthenticatedUserDetails user,
			@Valid @RequestBody ChangePasswordRequestDto request) {
		authService.changePassword(user.getId(), request);
		return new AuthResponseDto<>(AppConstants.Auth.CHANGE_PASSWORD_SUCCESS,
				new MessageResponseDto(AppConstants.Auth.CHANGE_PASSWORD_SUCCESS));
	}

	private String clientIp(HttpServletRequest request) {
		return clientIpResolver == null ? request.getRemoteAddr() : clientIpResolver.resolve(request);
	}

	private void rateLimit(String operation, String key, AuthenticationProperties.Limit limit) {
		if (rateLimiter != null) rateLimiter.check(operation, key, limit);
	}

	private String refreshToken(HttpServletRequest request) {
		if (request.getCookies() == null) {
			return null;
		}
		String cookieName = authenticationProperties.getRefreshCookie().getName();
		for (Cookie cookie : request.getCookies()) {
			if (cookieName.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
		response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie(refreshToken,
				Duration.ofSeconds(authenticationProperties.getJwt().getRefreshTokenExpirySeconds())).toString());
	}

	private void clearRefreshTokenCookie(HttpServletResponse response) {
		response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie("", Duration.ZERO).toString());
	}

	private ResponseCookie refreshTokenCookie(String value, Duration maxAge) {
		AuthenticationProperties.RefreshCookie cookieProperties = authenticationProperties.getRefreshCookie();
		return ResponseCookie.from(cookieProperties.getName(), value)
			.httpOnly(true)
			.secure(cookieProperties.isSecure())
			.sameSite(cookieProperties.getSameSite())
			.path(cookieProperties.getPath())
			.maxAge(maxAge)
			.build();
	}
}
