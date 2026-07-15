package com.mahendra.bizcart_backend.authentication.service;

import com.mahendra.bizcart_backend.authentication.dto.response.TokenResponseDto;

public record RefreshTokenResult(
		TokenResponseDto response,
		String refreshToken
) {
}
