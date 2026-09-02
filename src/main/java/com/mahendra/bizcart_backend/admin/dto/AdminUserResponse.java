package com.mahendra.bizcart_backend.admin.dto;

import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import java.util.List;

public record AdminUserResponse(Long id, String firstName, String lastName, String username, String email,
		String phone, String profileImage, AccountStatus status, List<String> roles) {
}
