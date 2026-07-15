package com.mahendra.bizcart_backend.common.constants;

public final class AppConstants {

	private AppConstants() {
	}

	public static final class Tables {

		public static final String USERS = "users";
		public static final String ROLES = "roles";
		public static final String PERMISSIONS = "permissions";
		public static final String USER_ROLES = "user_roles";
		public static final String ROLE_PERMISSIONS = "role_permissions";
		public static final String REFRESH_TOKENS = "refresh_tokens";
		public static final String PASSWORD_RESET_TOKENS = "password_reset_tokens";
		public static final String VERIFICATION_TOKENS = "verification_tokens";
		public static final String LOGIN_ATTEMPTS = "login_attempts";

		private Tables() {
		}
	}

	public static final class Columns {

		public static final String ID = "id";
		public static final String CREATED_AT = "created_at";
		public static final String UPDATED_AT = "updated_at";
		public static final String USER_ID = "user_id";
		public static final String ROLE_ID = "role_id";
		public static final String PERMISSION_ID = "permission_id";
		public static final String FIRST_NAME = "first_name";
		public static final String LAST_NAME = "last_name";
		public static final String USERNAME = "username";
		public static final String EMAIL = "email";
		public static final String PHONE = "phone";
		public static final String PASSWORD = "password";
		public static final String PROFILE_IMAGE = "profile_image";
		public static final String USER_TYPE = "user_type";
		public static final String STATUS = "status";
		public static final String EMAIL_VERIFIED = "email_verified";
		public static final String ADMIN_APPROVED = "admin_approved";
		public static final String TOKEN_VERSION = "token_version";
		public static final String LAST_LOGIN_AT = "last_login_at";
		public static final String NAME = "name";
		public static final String DESCRIPTION = "description";
		public static final String TOKEN_HASH = "token_hash";
		public static final String TOKEN_FAMILY_ID = "token_family_id";
		public static final String PARENT_TOKEN_ID = "parent_token_id";
		public static final String REPLACED_BY_TOKEN_ID = "replaced_by_token_id";
		public static final String DEVICE_INFO = "device_info";
		public static final String IP_ADDRESS = "ip_address";
		public static final String EXPIRES_AT = "expires_at";
		public static final String REVOKED_AT = "revoked_at";
		public static final String REVOCATION_REASON = "revocation_reason";
		public static final String USED_AT = "used_at";
		public static final String INVALIDATED_AT = "invalidated_at";
		public static final String VERIFICATION_TYPE = "verification_type";
		public static final String VERIFIED_AT = "verified_at";
		public static final String USER_AGENT = "user_agent";
		public static final String WAS_SUCCESSFUL = "was_successful";
		public static final String FAILURE_REASON = "failure_reason";
		public static final String ATTEMPTED_AT = "attempted_at";

		private Columns() {
		}
	}

	public static final class Indexes {

		public static final String UX_USERS_EMAIL = "ux_users_email";
		public static final String UX_USERS_USERNAME = "ux_users_username";
		public static final String UX_USERS_PHONE = "ux_users_phone";
		public static final String IX_USERS_STATUS = "ix_users_status";
		public static final String IX_USERS_USER_TYPE = "ix_users_user_type";
		public static final String UX_ROLES_NAME = "ux_roles_name";
		public static final String UX_PERMISSIONS_NAME = "ux_permissions_name";
		public static final String UX_USER_ROLES_USER_ROLE = "ux_user_roles_user_role";
		public static final String IX_USER_ROLES_USER_ID = "ix_user_roles_user_id";
		public static final String IX_USER_ROLES_ROLE_ID = "ix_user_roles_role_id";
		public static final String UX_ROLE_PERMISSIONS_ROLE_PERMISSION = "ux_role_permissions_role_permission";
		public static final String IX_ROLE_PERMISSIONS_ROLE_ID = "ix_role_permissions_role_id";
		public static final String IX_ROLE_PERMISSIONS_PERMISSION_ID = "ix_role_permissions_permission_id";
		public static final String UX_REFRESH_TOKENS_TOKEN_HASH = "ux_refresh_tokens_token_hash";
		public static final String IX_REFRESH_TOKENS_USER_ID = "ix_refresh_tokens_user_id";
		public static final String IX_REFRESH_TOKENS_TOKEN_FAMILY_ID = "ix_refresh_tokens_token_family_id";
		public static final String IX_REFRESH_TOKENS_EXPIRES_AT = "ix_refresh_tokens_expires_at";
		public static final String IX_REFRESH_TOKENS_USER_REVOKED_EXPIRES = "ix_refresh_tokens_user_revoked_expires";
		public static final String UX_PASSWORD_RESET_TOKENS_TOKEN_HASH = "ux_password_reset_tokens_token_hash";
		public static final String IX_PASSWORD_RESET_TOKENS_USER_ID = "ix_password_reset_tokens_user_id";
		public static final String IX_PASSWORD_RESET_TOKENS_EXPIRES_AT = "ix_password_reset_tokens_expires_at";
		public static final String IX_PASSWORD_RESET_TOKENS_USER_USED_EXPIRES = "ix_password_reset_tokens_user_used_expires";
		public static final String UX_VERIFICATION_TOKENS_TOKEN_HASH = "ux_verification_tokens_token_hash";
		public static final String IX_VERIFICATION_TOKENS_USER_ID = "ix_verification_tokens_user_id";
		public static final String IX_VERIFICATION_TOKENS_EXPIRES_AT = "ix_verification_tokens_expires_at";
		public static final String IX_VERIFICATION_TOKENS_USER_VERIFIED_EXPIRES = "ix_verification_tokens_user_verified_expires";
		public static final String IX_LOGIN_ATTEMPTS_EMAIL = "ix_login_attempts_email";
		public static final String IX_LOGIN_ATTEMPTS_IP_ADDRESS = "ix_login_attempts_ip_address";
		public static final String IX_LOGIN_ATTEMPTS_ATTEMPTED_AT = "ix_login_attempts_attempted_at";
		public static final String IX_LOGIN_ATTEMPTS_EMAIL_ATTEMPTED = "ix_login_attempts_email_attempted";

		private Indexes() {
		}
	}

	public static final class Constraints {

		public static final String FK_USER_ROLES_USER = "fk_user_roles_user";
		public static final String FK_USER_ROLES_ROLE = "fk_user_roles_role";
		public static final String FK_ROLE_PERMISSIONS_ROLE = "fk_role_permissions_role";
		public static final String FK_ROLE_PERMISSIONS_PERMISSION = "fk_role_permissions_permission";
		public static final String FK_REFRESH_TOKENS_USER = "fk_refresh_tokens_user";
		public static final String FK_REFRESH_TOKENS_PARENT = "fk_refresh_tokens_parent";
		public static final String FK_REFRESH_TOKENS_REPLACED_BY = "fk_refresh_tokens_replaced_by";
		public static final String FK_PASSWORD_RESET_TOKENS_USER = "fk_password_reset_tokens_user";
		public static final String FK_VERIFICATION_TOKENS_USER = "fk_verification_tokens_user";
		public static final String FK_LOGIN_ATTEMPTS_USER = "fk_login_attempts_user";

		private Constraints() {
		}
	}

	public static final class FieldLengths {

		public static final int NAME = 100;
		public static final int USERNAME = 50;
		public static final int EMAIL = 255;
		public static final int PHONE = 20;
		public static final int PASSWORD = 255;
		public static final int PROFILE_IMAGE = 500;
		public static final int TOKEN_HASH = 255;
		public static final int TOKEN_FAMILY_ID = 36;
		public static final int DEVICE_INFO = 500;
		public static final int IP_ADDRESS = 45;
		public static final int USER_AGENT = 500;
		public static final int FAILURE_REASON = 255;
		public static final int DESCRIPTION = 500;
		public static final int ENUM = 50;

		private FieldLengths() {
		}
	}

	public static final class RoleNames {

		public static final String ADMIN = "ADMIN";
		public static final String SELLER = "SELLER";
		public static final String CUSTOMER = "CUSTOMER";

		private RoleNames() {
		}
	}

	public static final class Auth {

		public static final String API_AUTH_BASE = "/api/v1/auth";
		public static final String REGISTER_PATH = "/register";
		public static final String LOGIN_PATH = "/login";
		public static final String CURRENT_USER_PATH = "/me";
		public static final String CSRF_PATH = "/csrf";
		public static final String REGISTER_SUCCESS = "User registered successfully";
		public static final String LOGIN_SUCCESS = "Login successful";
		public static final String CURRENT_USER_SUCCESS = "Current user fetched successfully";
		public static final String CSRF_TOKEN_SUCCESS = "CSRF token fetched successfully";
		public static final String DUPLICATE_EMAIL = "Email is already registered";
		public static final String DUPLICATE_USERNAME = "Username is already registered";
		public static final String INVALID_CREDENTIALS = "Invalid email or password";
		public static final String ACCOUNT_NOT_ACTIVE = "Account is not active";
		public static final String EMAIL_NOT_VERIFIED = "Email is not verified";
		public static final String SELLER_NOT_APPROVED = "Seller account is not approved";
		public static final String VALIDATION_FAILED = "Request validation failed";
		public static final String INTERNAL_ERROR = "Internal server error";
		public static final String DEFAULT_ROLE_DESCRIPTION = "Default customer role";
		public static final String REFRESH_TOKEN_COOKIE = "BIZCART_REFRESH_TOKEN";
		public static final String SAME_SITE_LAX = "Lax";
		public static final String HMAC_SHA_256 = "HmacSHA256";
		public static final String CSRF_HEADER = "X-XSRF-TOKEN";
		public static final String CORS_ALLOWED_HEADERS = "Authorization,Content-Type," + CSRF_HEADER;
		public static final String AUTH_VALIDATION_FAILED = "AUTH_VALIDATION_FAILED";
		public static final String AUTH_INVALID_CREDENTIALS = "AUTH_INVALID_CREDENTIALS";
		public static final String AUTH_INVALID_TOKEN = "AUTH_INVALID_TOKEN";
		public static final String AUTH_ACCESS_DENIED = "AUTH_ACCESS_DENIED";
		public static final String AUTH_ACCOUNT_INACTIVE = "AUTH_ACCOUNT_INACTIVE";
		public static final String AUTH_EMAIL_NOT_VERIFIED = "AUTH_EMAIL_NOT_VERIFIED";
		public static final String AUTH_SELLER_NOT_APPROVED = "AUTH_SELLER_NOT_APPROVED";
		public static final String AUTH_EMAIL_ALREADY_EXISTS = "AUTH_EMAIL_ALREADY_EXISTS";
		public static final String AUTH_USERNAME_ALREADY_EXISTS = "AUTH_USERNAME_ALREADY_EXISTS";
		public static final String AUTH_CONFLICT = "AUTH_CONFLICT";
		public static final String AUTH_INTERNAL_ERROR = "AUTH_INTERNAL_ERROR";

		private Auth() {
		}
	}
}
