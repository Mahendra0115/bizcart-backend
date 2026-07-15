package com.mahendra.bizcart_backend.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.common.entity.BaseEntity;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = AppConstants.Tables.USERS,
		indexes = {
				@Index(name = AppConstants.Indexes.UX_USERS_EMAIL, columnList = AppConstants.Columns.EMAIL, unique = true),
				@Index(name = AppConstants.Indexes.UX_USERS_USERNAME, columnList = AppConstants.Columns.USERNAME,
						unique = true),
				@Index(name = AppConstants.Indexes.UX_USERS_PHONE, columnList = AppConstants.Columns.PHONE, unique = true),
				@Index(name = AppConstants.Indexes.IX_USERS_STATUS, columnList = AppConstants.Columns.STATUS),
				@Index(name = AppConstants.Indexes.IX_USERS_USER_TYPE, columnList = AppConstants.Columns.USER_TYPE)
		})
public class User extends BaseEntity {

	@Column(name = AppConstants.Columns.FIRST_NAME, nullable = false, length = AppConstants.FieldLengths.NAME)
	private String firstName;

	@Column(name = AppConstants.Columns.LAST_NAME, nullable = false, length = AppConstants.FieldLengths.NAME)
	private String lastName;

	@Column(name = AppConstants.Columns.USERNAME, nullable = false, length = AppConstants.FieldLengths.USERNAME)
	private String username;

	@Column(name = AppConstants.Columns.EMAIL, nullable = false, length = AppConstants.FieldLengths.EMAIL)
	private String email;

	@Column(name = AppConstants.Columns.PHONE, length = AppConstants.FieldLengths.PHONE)
	private String phone;

	@JsonIgnore
	@Column(name = AppConstants.Columns.PASSWORD, nullable = false, length = AppConstants.FieldLengths.PASSWORD)
	private String password;

	@Column(name = AppConstants.Columns.PROFILE_IMAGE, length = AppConstants.FieldLengths.PROFILE_IMAGE)
	private String profileImage;

	@Enumerated(EnumType.STRING)
	@Column(name = AppConstants.Columns.USER_TYPE, nullable = false, length = AppConstants.FieldLengths.ENUM)
	private UserType userType;

	@Enumerated(EnumType.STRING)
	@Column(name = AppConstants.Columns.STATUS, nullable = false, length = AppConstants.FieldLengths.ENUM)
	private AccountStatus status;

	@Column(name = AppConstants.Columns.EMAIL_VERIFIED, nullable = false)
	private boolean emailVerified;

	@Column(name = AppConstants.Columns.ADMIN_APPROVED, nullable = false)
	private boolean adminApproved;

	@Column(name = AppConstants.Columns.TOKEN_VERSION, nullable = false)
	private long tokenVersion;

	@Column(name = AppConstants.Columns.LAST_LOGIN_AT)
	private LocalDateTime lastLoginAt;

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

	public String getProfileImage() {
		return profileImage;
	}

	public void setProfileImage(String profileImage) {
		this.profileImage = profileImage;
	}

	public UserType getUserType() {
		return userType;
	}

	public void setUserType(UserType userType) {
		this.userType = userType;
	}

	public AccountStatus getStatus() {
		return status;
	}

	public void setStatus(AccountStatus status) {
		this.status = status;
	}

	public boolean isEmailVerified() {
		return emailVerified;
	}

	public void setEmailVerified(boolean emailVerified) {
		this.emailVerified = emailVerified;
	}

	public boolean isAdminApproved() {
		return adminApproved;
	}

	public void setAdminApproved(boolean adminApproved) {
		this.adminApproved = adminApproved;
	}

	public long getTokenVersion() {
		return tokenVersion;
	}

	public void setTokenVersion(long tokenVersion) {
		this.tokenVersion = tokenVersion;
	}

	public LocalDateTime getLastLoginAt() {
		return lastLoginAt;
	}

	public void setLastLoginAt(LocalDateTime lastLoginAt) {
		this.lastLoginAt = lastLoginAt;
	}
}
