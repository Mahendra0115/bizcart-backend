package com.mahendra.bizcart_backend.authentication.controller;

import com.mahendra.bizcart_backend.authentication.dto.request.LoginRequestDto;
import com.mahendra.bizcart_backend.authentication.dto.request.RegisterRequestDto;
import com.mahendra.bizcart_backend.authentication.dto.response.AuthResponseDto;
import com.mahendra.bizcart_backend.authentication.dto.response.CurrentUserResponseDto;
import com.mahendra.bizcart_backend.authentication.dto.response.CsrfTokenResponseDto;
import com.mahendra.bizcart_backend.authentication.dto.response.LoginResponseDto;
import com.mahendra.bizcart_backend.authentication.dto.response.RegisterResponseDto;
import com.mahendra.bizcart_backend.authentication.config.AuthenticationProperties;
import com.mahendra.bizcart_backend.authentication.security.AuthenticatedUserDetails;
import com.mahendra.bizcart_backend.authentication.service.LoginResult;
import com.mahendra.bizcart_backend.authentication.service.AuthService;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
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

	@PostMapping(AppConstants.Auth.REGISTER_PATH)
	public AuthResponseDto<RegisterResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
		return new AuthResponseDto<>(AppConstants.Auth.REGISTER_SUCCESS, authService.register(request));
	}

	@PostMapping(AppConstants.Auth.LOGIN_PATH)
	public AuthResponseDto<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request,
			HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
		LoginResult loginResult = authService.login(request, clientIp(httpServletRequest),
				httpServletRequest.getHeader(HEADER_USER_AGENT));
		addRefreshTokenCookie(httpServletResponse, loginResult.refreshToken());
		return new AuthResponseDto<>(AppConstants.Auth.LOGIN_SUCCESS, loginResult.response());
	}

	@GetMapping(AppConstants.Auth.CURRENT_USER_PATH)
	public AuthResponseDto<CurrentUserResponseDto> currentUser(
			@AuthenticationPrincipal AuthenticatedUserDetails authenticatedUserDetails) {
		return new AuthResponseDto<>(AppConstants.Auth.CURRENT_USER_SUCCESS,
				authService.currentUser(authenticatedUserDetails.getId()));
	}

	@GetMapping(AppConstants.Auth.CSRF_PATH)
	public AuthResponseDto<CsrfTokenResponseDto> csrfToken(CsrfToken csrfToken) {
		return new AuthResponseDto<>(AppConstants.Auth.CSRF_TOKEN_SUCCESS,
				new CsrfTokenResponseDto(csrfToken.getHeaderName(), csrfToken.getParameterName(), csrfToken.getToken()));
	}

	private String clientIp(HttpServletRequest request) {
		String forwardedFor = request.getHeader(HEADER_FORWARDED_FOR);
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return forwardedFor.split(FORWARDED_FOR_SEPARATOR)[0].trim();
		}
		return request.getRemoteAddr();
	}

	private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
		AuthenticationProperties.RefreshCookie cookieProperties = authenticationProperties.getRefreshCookie();
		ResponseCookie cookie = ResponseCookie.from(cookieProperties.getName(), refreshToken)
			.httpOnly(true)
			.secure(cookieProperties.isSecure())
			.sameSite(cookieProperties.getSameSite())
			.path(cookieProperties.getPath())
			.maxAge(Duration.ofSeconds(authenticationProperties.getJwt().getRefreshTokenExpirySeconds()))
			.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}
}
