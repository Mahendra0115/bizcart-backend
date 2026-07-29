package com.mahendra.bizcart_backend.user.dto.request;

import com.mahendra.bizcart_backend.common.constants.AppConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequestDto(
		@NotBlank(message = "First name is required")
		@Size(max = AppConstants.FieldLengths.NAME, message = "First name must not exceed 100 characters")
		String firstName,

		@NotBlank(message = "Last name is required")
		@Size(max = AppConstants.FieldLengths.NAME, message = "Last name must not exceed 100 characters")
		String lastName,

		@Size(max = AppConstants.FieldLengths.PHONE, message = "Phone must not exceed 20 characters")
		@Pattern(regexp = "^$|^\\+?[0-9]{7,15}$", message = "Phone number is invalid")
		String phone,

		@Size(max = AppConstants.FieldLengths.PROFILE_IMAGE,
				message = "Profile image URL must not exceed 500 characters")
		String profileImage
) {
}
