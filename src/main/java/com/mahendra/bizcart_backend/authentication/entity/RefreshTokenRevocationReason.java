package com.mahendra.bizcart_backend.authentication.entity;

public enum RefreshTokenRevocationReason {
	ROTATED,
	LOGOUT,
	LOGOUT_ALL,
	PASSWORD_RESET,
	PASSWORD_CHANGE,
	TOKEN_REUSE_DETECTED,
	ADMIN_REVOKED
}
