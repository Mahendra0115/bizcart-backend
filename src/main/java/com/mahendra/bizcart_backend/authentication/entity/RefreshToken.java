package com.mahendra.bizcart_backend.authentication.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.common.entity.BaseEntity;
import com.mahendra.bizcart_backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = AppConstants.Tables.REFRESH_TOKENS,
		indexes = {
				@Index(name = AppConstants.Indexes.UX_REFRESH_TOKENS_TOKEN_HASH,
						columnList = AppConstants.Columns.TOKEN_HASH, unique = true),
				@Index(name = AppConstants.Indexes.IX_REFRESH_TOKENS_USER_ID, columnList = AppConstants.Columns.USER_ID),
				@Index(name = AppConstants.Indexes.IX_REFRESH_TOKENS_TOKEN_FAMILY_ID,
						columnList = AppConstants.Columns.TOKEN_FAMILY_ID),
				@Index(name = AppConstants.Indexes.IX_REFRESH_TOKENS_EXPIRES_AT, columnList = AppConstants.Columns.EXPIRES_AT),
				@Index(name = AppConstants.Indexes.IX_REFRESH_TOKENS_USER_REVOKED_EXPIRES,
						columnList = AppConstants.Columns.USER_ID + "," + AppConstants.Columns.REVOKED_AT + ","
								+ AppConstants.Columns.EXPIRES_AT)
		})
public class RefreshToken extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = AppConstants.Columns.USER_ID, nullable = false,
			foreignKey = @ForeignKey(name = AppConstants.Constraints.FK_REFRESH_TOKENS_USER))
	private User user;

	@JsonIgnore
	@Column(name = AppConstants.Columns.TOKEN_HASH, nullable = false, length = AppConstants.FieldLengths.TOKEN_HASH)
	private String tokenHash;

	@Column(name = AppConstants.Columns.TOKEN_FAMILY_ID, nullable = false,
			length = AppConstants.FieldLengths.TOKEN_FAMILY_ID)
	private String tokenFamilyId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = AppConstants.Columns.PARENT_TOKEN_ID,
			foreignKey = @ForeignKey(name = AppConstants.Constraints.FK_REFRESH_TOKENS_PARENT))
	private RefreshToken parentToken;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = AppConstants.Columns.REPLACED_BY_TOKEN_ID,
			foreignKey = @ForeignKey(name = AppConstants.Constraints.FK_REFRESH_TOKENS_REPLACED_BY))
	private RefreshToken replacedByToken;

	@Column(name = AppConstants.Columns.DEVICE_INFO, length = AppConstants.FieldLengths.DEVICE_INFO)
	private String deviceInfo;

	@Column(name = AppConstants.Columns.IP_ADDRESS, length = AppConstants.FieldLengths.IP_ADDRESS)
	private String ipAddress;

	@Column(name = AppConstants.Columns.EXPIRES_AT, nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = AppConstants.Columns.REVOKED_AT)
	private LocalDateTime revokedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = AppConstants.Columns.REVOCATION_REASON, length = AppConstants.FieldLengths.ENUM)
	private RefreshTokenRevocationReason revocationReason;

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public void setTokenHash(String tokenHash) {
		this.tokenHash = tokenHash;
	}

	public String getTokenFamilyId() {
		return tokenFamilyId;
	}

	public void setTokenFamilyId(String tokenFamilyId) {
		this.tokenFamilyId = tokenFamilyId;
	}

	public RefreshToken getParentToken() {
		return parentToken;
	}

	public void setParentToken(RefreshToken parentToken) {
		this.parentToken = parentToken;
	}

	public RefreshToken getReplacedByToken() {
		return replacedByToken;
	}

	public void setReplacedByToken(RefreshToken replacedByToken) {
		this.replacedByToken = replacedByToken;
	}

	public String getDeviceInfo() {
		return deviceInfo;
	}

	public void setDeviceInfo(String deviceInfo) {
		this.deviceInfo = deviceInfo;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public LocalDateTime getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(LocalDateTime expiresAt) {
		this.expiresAt = expiresAt;
	}

	public LocalDateTime getRevokedAt() {
		return revokedAt;
	}

	public void setRevokedAt(LocalDateTime revokedAt) {
		this.revokedAt = revokedAt;
	}

	public RefreshTokenRevocationReason getRevocationReason() {
		return revocationReason;
	}

	public void setRevocationReason(RefreshTokenRevocationReason revocationReason) {
		this.revocationReason = revocationReason;
	}
}
