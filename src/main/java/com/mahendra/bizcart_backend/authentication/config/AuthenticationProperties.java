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

	public Jwt getJwt() {
		return jwt;
	}

	public Password getPassword() {
		return password;
	}

	public Cors getCors() {
		return cors;
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
}
