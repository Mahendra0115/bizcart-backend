package com.mahendra.bizcart_backend.authentication.dto.response;

import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import java.util.List;

public record CurrentUserResponseDto(
		Long id,
		String firstName,
		String lastName,
		String email,
		String phone,
		String profileImage,
		UserType userType,
		AccountStatus status,
		boolean emailVerified,
		List<String> roles,
		List<String> permissions
) {
}
