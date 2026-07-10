package com.mahendra.bizcart_backend.authentication.dto.request;

import com.mahendra.bizcart_backend.authentication.dto.validation.AuthValidationConstants;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ForgotPasswordRequestDto {

	@NotBlank(message = AuthValidationConstants.EMAIL_REQUIRED)
	@Email(message = AuthValidationConstants.EMAIL_INVALID)
	@Size(max = AppConstants.FieldLengths.EMAIL)
	private String email;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}
