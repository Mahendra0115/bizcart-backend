package com.mahendra.bizcart_backend.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record AdminUserRequest(
		@NotBlank @Size(max = 100) String firstName,
		@NotBlank @Size(max = 100) String lastName,
		@NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[A-Za-z0-9._-]+$") String username,
		@NotBlank @Email @Size(max = 255) String email,
		@Size(max = 20) String phone,
		@NotBlank @Size(min = 8, max = 64) String password,
		@Size(max = 500) String profileImage,
		@NotEmpty Set<Long> roleIds
) {
}
