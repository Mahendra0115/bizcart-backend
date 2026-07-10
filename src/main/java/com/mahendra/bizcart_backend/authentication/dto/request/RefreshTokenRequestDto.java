package com.mahendra.bizcart_backend.authentication.dto.request;

import com.mahendra.bizcart_backend.authentication.dto.validation.AuthValidationConstants;
import jakarta.validation.constraints.NotBlank;

public class RefreshTokenRequestDto {

	@NotBlank(message = AuthValidationConstants.REFRESH_TOKEN_REQUIRED)
	private String refreshToken;

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}
}
