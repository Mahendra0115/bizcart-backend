package com.mahendra.bizcart_backend.authentication.dto.validation;

public final class AuthValidationConstants {

	public static final String EMAIL_REQUIRED = "Email is required";
	public static final String EMAIL_INVALID = "Email must be valid";
	public static final String PASSWORD_REQUIRED = "Password is required";
	public static final String CURRENT_PASSWORD_REQUIRED = "Current password is required";
	public static final String NEW_PASSWORD_REQUIRED = "New password is required";
	public static final String CONFIRM_PASSWORD_REQUIRED = "Confirm password is required";
	public static final String TOKEN_REQUIRED = "Token is required";
	public static final String REFRESH_TOKEN_REQUIRED = "Refresh token is required";
	public static final String FIRST_NAME_REQUIRED = "First name is required";
	public static final String LAST_NAME_REQUIRED = "Last name is required";
	public static final String USER_TYPE_REQUIRED = "User type is required";
	public static final String PASSWORD_MISMATCH = "Password and confirmation password must match";
	public static final String NEW_PASSWORD_MISMATCH = "New password and confirmation password must match";
	public static final String PASSWORD_PATTERN_MESSAGE =
			"Password must include uppercase, lowercase, number, special character and no leading or trailing spaces";
	public static final String PASSWORD_PATTERN =
			"^(?=\\S)(?=.*\\S$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,64}$";

	private AuthValidationConstants() {
	}
}
