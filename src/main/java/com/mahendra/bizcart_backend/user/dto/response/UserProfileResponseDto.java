package com.mahendra.bizcart_backend.user.dto.response;

import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;

public record UserProfileResponseDto(
		Long id,
		String firstName,
		String lastName,
		String username,
		String email,
		String phone,
		String profileImage,
		UserType userType,
		AccountStatus status,
		boolean emailVerified,
		boolean adminApproved
) {
}
