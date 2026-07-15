package com.mahendra.bizcart_backend.authentication.config;

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

	public static class Jwt {

		@NotBlank
		private String secret;

		@Min(1)
		private long accessTokenExpirySeconds = 900;

		@Min(1)
		private long refreshTokenExpirySeconds = 604800;

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

		private String allowedHeaders = "Authorization,Content-Type,X-CSRF-TOKEN";

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
}
