package com.mahendra.bizcart_backend.authentication.dto.response;

import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;

public record RegisterResponseDto(
		Long id,
		String firstName,
		String lastName,
		String email,
		String phone,
		UserType userType,
		AccountStatus status,
		boolean emailVerified
) {
}
