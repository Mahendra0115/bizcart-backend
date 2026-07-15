package com.mahendra.bizcart_backend.authentication.controller;

import com.mahendra.bizcart_backend.authentication.config.AuthenticationProperties;
import com.mahendra.bizcart_backend.authentication.dto.response.AuthResponseDto;
import com.mahendra.bizcart_backend.authentication.dto.response.MessageResponseDto;
import com.mahendra.bizcart_backend.authentication.dto.response.TokenResponseDto;
import com.mahendra.bizcart_backend.authentication.security.AuthenticatedUserDetails;
import com.mahendra.bizcart_backend.authentication.service.AuthService;
import com.mahendra.bizcart_backend.authentication.service.RefreshTokenResult;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConstants.Auth.API_AUTH_BASE)
public class AuthController {

	private static final String HEADER_USER_AGENT = "User-Agent";
	private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";
	private static final String FORWARDED_FOR_SEPARATOR = ",";

	private final AuthService authService;
	private final AuthenticationProperties authenticationProperties;

	public AuthController(AuthService authService, AuthenticationProperties authenticationProperties) {
		this.authService = authService;
		this.authenticationProperties = authenticationProperties;
	}

	@PostMapping(AppConstants.Auth.REFRESH_TOKEN_PATH)
	public AuthResponseDto<TokenResponseDto> refreshAccessToken(
			HttpServletRequest request, HttpServletResponse response) {
		RefreshTokenResult result = authService.refreshAccessToken(refreshToken(request), clientIp(request),
				request.getHeader(HEADER_USER_AGENT));
		addRefreshTokenCookie(response, result.refreshToken());
		return new AuthResponseDto<>(AppConstants.Auth.REFRESH_TOKEN_SUCCESS, result.response());
	}

	@PostMapping(AppConstants.Auth.LOGOUT_PATH)
	public AuthResponseDto<MessageResponseDto> logout(
			HttpServletRequest request,
			HttpServletResponse response) {
		authService.logout(refreshToken(request));
		clearRefreshTokenCookie(response);
		return new AuthResponseDto<>(AppConstants.Auth.LOGOUT_SUCCESS,
				new MessageResponseDto(AppConstants.Auth.LOGOUT_SUCCESS));
	}

	@PostMapping(AppConstants.Auth.LOGOUT_ALL_PATH)
	public AuthResponseDto<MessageResponseDto> logoutAll(
			@AuthenticationPrincipal AuthenticatedUserDetails authenticatedUserDetails, HttpServletResponse response) {
		authService.logoutAll(authenticatedUserDetails.getId());
		clearRefreshTokenCookie(response);
		return new AuthResponseDto<>(AppConstants.Auth.LOGOUT_ALL_SUCCESS,
				new MessageResponseDto(AppConstants.Auth.LOGOUT_ALL_SUCCESS));
	}

	private String clientIp(HttpServletRequest request) {
		String forwardedFor = request.getHeader(HEADER_FORWARDED_FOR);
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return forwardedFor.split(FORWARDED_FOR_SEPARATOR)[0].trim();
		}
		return request.getRemoteAddr();
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
