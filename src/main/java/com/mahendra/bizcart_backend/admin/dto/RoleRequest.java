package com.mahendra.bizcart_backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record RoleRequest(@NotBlank @Size(max = 100) String name, @Size(max = 500) String description,
		Set<Long> permissionIds) {
}
