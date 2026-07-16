package com.mahendra.bizcart_backend.authentication.dto.request;

import com.mahendra.bizcart_backend.authentication.dto.validation.AuthValidationConstants;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequestDto {

	@NotBlank(message = AuthValidationConstants.EMAIL_REQUIRED)
	@Email(message = AuthValidationConstants.EMAIL_INVALID)
	@Size(max = AppConstants.FieldLengths.EMAIL)
	private String email;

	@NotBlank(message = AuthValidationConstants.PASSWORD_REQUIRED)
	@Size(max = 64)
	private String password;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
