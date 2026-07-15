package com.mahendra.bizcart_backend.authentication.service;

import com.mahendra.bizcart_backend.authentication.dto.response.LoginResponseDto;

public record LoginResult(
		LoginResponseDto response,
		String refreshToken
) {
}
