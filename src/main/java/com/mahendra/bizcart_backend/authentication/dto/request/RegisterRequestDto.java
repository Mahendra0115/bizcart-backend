package com.mahendra.bizcart_backend.authentication.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mahendra.bizcart_backend.authentication.dto.validation.AuthValidationConstants;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.user.enums.UserType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequestDto {

	@NotBlank(message = AuthValidationConstants.FIRST_NAME_REQUIRED)
	@Size(max = AppConstants.FieldLengths.NAME)
	private String firstName;

	@NotBlank(message = AuthValidationConstants.LAST_NAME_REQUIRED)
	@Size(max = AppConstants.FieldLengths.NAME)
	private String lastName;

	@NotBlank(message = AuthValidationConstants.USERNAME_REQUIRED)
	@Size(min = 3, max = AppConstants.FieldLengths.USERNAME)
	@Pattern(regexp = AuthValidationConstants.USERNAME_PATTERN,
			message = AuthValidationConstants.USERNAME_PATTERN_MESSAGE)
	private String username;

	@NotBlank(message = AuthValidationConstants.EMAIL_REQUIRED)
	@Email(message = AuthValidationConstants.EMAIL_INVALID)
	@Size(max = AppConstants.FieldLengths.EMAIL)
	private String email;

	@Size(max = AppConstants.FieldLengths.PHONE)
	private String phone;

	@NotBlank(message = AuthValidationConstants.PASSWORD_REQUIRED)
	@Size(min = 8, max = 64)
	@Pattern(regexp = AuthValidationConstants.PASSWORD_PATTERN,
			message = AuthValidationConstants.PASSWORD_PATTERN_MESSAGE)
	private String password;

	@NotBlank(message = AuthValidationConstants.CONFIRM_PASSWORD_REQUIRED)
	private String confirmPassword;

	@NotNull(message = AuthValidationConstants.USER_TYPE_REQUIRED)
	private UserType userType;

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getConfirmPassword() {
		return confirmPassword;
	}

	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}

	public UserType getUserType() {
		return userType;
	}

	public void setUserType(UserType userType) {
		this.userType = userType;
	}

	@JsonIgnore
	@AssertTrue(message = AuthValidationConstants.PASSWORD_MISMATCH)
	public boolean isPasswordConfirmed() {
		if (password == null || confirmPassword == null) {
			return true;
		}
		return password.equals(confirmPassword);
	}

	@JsonIgnore
	@AssertTrue(message = AuthValidationConstants.USER_TYPE_PUBLIC_REGISTRATION_NOT_ALLOWED)
	public boolean isAllowedPublicRegistrationUserType() {
		return userType == null || userType == UserType.CUSTOMER || userType == UserType.SELLER;
	}
}
