package com.mahendra.bizcart_backend.authentication.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mahendra.bizcart_backend.authentication.dto.validation.AuthValidationConstants;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequestDto {

	@NotBlank(message = AuthValidationConstants.TOKEN_REQUIRED)
	private String token;

	@NotBlank(message = AuthValidationConstants.NEW_PASSWORD_REQUIRED)
	@Size(min = 8, max = 64)
	@Pattern(regexp = AuthValidationConstants.PASSWORD_PATTERN,
			message = AuthValidationConstants.PASSWORD_PATTERN_MESSAGE)
	private String newPassword;

	@NotBlank(message = AuthValidationConstants.CONFIRM_PASSWORD_REQUIRED)
	private String confirmPassword;

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}

	public String getConfirmPassword() {
		return confirmPassword;
	}

	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}

	@JsonIgnore
	@AssertTrue(message = AuthValidationConstants.NEW_PASSWORD_MISMATCH)
	public boolean isNewPasswordConfirmed() {
		if (newPassword == null || confirmPassword == null) {
			return true;
		}
		return newPassword.equals(confirmPassword);
	}
}
