package com.mahendra.bizcart_backend.authentication.config;

import com.mahendra.bizcart_backend.common.constants.AppConstants;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "bizcart.auth")
public class AuthenticationProperties {

	private final Jwt jwt = new Jwt();
	private final Password password = new Password();
	private final Cors cors = new Cors();
	private final TokenHash tokenHash = new TokenHash();
	private final RefreshCookie refreshCookie = new RefreshCookie();
	private final Notification notification = new Notification();

	public Jwt getJwt() {
		return jwt;
	}

	public Password getPassword() {
		return password;
	}

	public Cors getCors() {
		return cors;
	}

	public TokenHash getTokenHash() {
		return tokenHash;
	}

	public RefreshCookie getRefreshCookie() {
		return refreshCookie;
	}

	public Notification getNotification() {
		return notification;
	}

	public static class Jwt {

		@NotBlank
		private String secret;

		@Min(1)
		private long accessTokenExpirySeconds = 900;

		@Min(1)
		private long refreshTokenExpirySeconds = 604800;

		@Min(0)
		private long refreshTokenReuseGraceSeconds = 2;

		@Min(1)
		private long passwordResetTokenExpirySeconds = 900;

		public String getSecret() {
			return secret;
		}

		public void setSecret(String secret) {
			this.secret = secret;
		}

		public long getAccessTokenExpirySeconds() {
			return accessTokenExpirySeconds;
		}

		public void setAccessTokenExpirySeconds(long accessTokenExpirySeconds) {
			this.accessTokenExpirySeconds = accessTokenExpirySeconds;
		}

		public long getRefreshTokenExpirySeconds() {
			return refreshTokenExpirySeconds;
		}

		public void setRefreshTokenExpirySeconds(long refreshTokenExpirySeconds) {
			this.refreshTokenExpirySeconds = refreshTokenExpirySeconds;
		}

		public long getRefreshTokenReuseGraceSeconds() {
			return refreshTokenReuseGraceSeconds;
		}

		public void setRefreshTokenReuseGraceSeconds(long refreshTokenReuseGraceSeconds) {
			this.refreshTokenReuseGraceSeconds = refreshTokenReuseGraceSeconds;
		}

		public long getPasswordResetTokenExpirySeconds() {
			return passwordResetTokenExpirySeconds;
		}

		public void setPasswordResetTokenExpirySeconds(long passwordResetTokenExpirySeconds) {
			this.passwordResetTokenExpirySeconds = passwordResetTokenExpirySeconds;
		}
	}

	public static class Password {

		@Min(4)
		private int bcryptStrength = 12;

		public int getBcryptStrength() {
			return bcryptStrength;
		}

		public void setBcryptStrength(int bcryptStrength) {
			this.bcryptStrength = bcryptStrength;
		}
	}

	public static class Cors {

		private String allowedOrigins = "http://localhost:3000,http://localhost:5173";

		private String allowedMethods = "GET,POST,PUT,PATCH,DELETE,OPTIONS";

		private String allowedHeaders = AppConstants.Auth.CORS_ALLOWED_HEADERS;

		private boolean allowCredentials = true;

		public String getAllowedOrigins() {
			return allowedOrigins;
		}

		public void setAllowedOrigins(String allowedOrigins) {
			this.allowedOrigins = allowedOrigins;
		}

		public String getAllowedMethods() {
			return allowedMethods;
		}

		public void setAllowedMethods(String allowedMethods) {
			this.allowedMethods = allowedMethods;
		}

		public String getAllowedHeaders() {
			return allowedHeaders;
		}

		public void setAllowedHeaders(String allowedHeaders) {
			this.allowedHeaders = allowedHeaders;
		}

		public boolean isAllowCredentials() {
			return allowCredentials;
		}

		public void setAllowCredentials(boolean allowCredentials) {
			this.allowCredentials = allowCredentials;
		}
	}

	public static class TokenHash {

		@NotBlank
		private String secret;

		public String getSecret() {
			return secret;
		}

		public void setSecret(String secret) {
			this.secret = secret;
		}
	}

	public static class RefreshCookie {

		@NotBlank
		private String name = "BIZCART_REFRESH_TOKEN";

		@NotBlank
		private String path = "/api/v1/auth";

		@NotBlank
		private String sameSite = "Lax";

		private boolean secure;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public String getSameSite() {
			return sameSite;
		}

		public void setSameSite(String sameSite) {
			this.sameSite = sameSite;
		}

		public boolean isSecure() {
			return secure;
		}

		public void setSecure(boolean secure) {
			this.secure = secure;
		}
	}

	public static class Notification {

		private String passwordResetProvider = AppConstants.Auth.NOTIFICATION_PROVIDER_NO_OP;

		private String passwordResetFrom = "no-reply@bizcart.local";

		private String passwordResetUrl = "http://localhost:5173/reset-password";

		private String passwordResetSubject = AppConstants.Auth.PASSWORD_RESET_EMAIL_SUBJECT;

		public String getPasswordResetProvider() {
			return passwordResetProvider;
		}

		public void setPasswordResetProvider(String passwordResetProvider) {
			this.passwordResetProvider = passwordResetProvider;
		}

		public String getPasswordResetFrom() {
			return passwordResetFrom;
		}

		public void setPasswordResetFrom(String passwordResetFrom) {
			this.passwordResetFrom = passwordResetFrom;
		}

		public String getPasswordResetUrl() {
			return passwordResetUrl;
		}

		public void setPasswordResetUrl(String passwordResetUrl) {
			this.passwordResetUrl = passwordResetUrl;
		}

		public String getPasswordResetSubject() {
			return passwordResetSubject;
		}

		public void setPasswordResetSubject(String passwordResetSubject) {
			this.passwordResetSubject = passwordResetSubject;
		}
	}
}
