package com.mahendra.bizcart_backend.user.dto.response;

import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;

public record AccountStatusResponseDto(
		Long id,
		String email,
		UserType userType,
		AccountStatus oldStatus,
		AccountStatus newStatus,
		String reason
) {
}
