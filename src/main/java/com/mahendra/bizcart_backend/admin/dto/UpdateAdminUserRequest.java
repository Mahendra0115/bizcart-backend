package com.mahendra.bizcart_backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UpdateAdminUserRequest(
		@NotBlank @Size(max = 100) String firstName,
		@NotBlank @Size(max = 100) String lastName,
		@Size(max = 20) String phone,
		@Size(max = 500) String profileImage,
		@NotEmpty Set<Long> roleIds
) {
}
