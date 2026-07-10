package com.mahendra.bizcart_backend.authentication.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.common.entity.BaseEntity;
import com.mahendra.bizcart_backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = AppConstants.Tables.PASSWORD_RESET_TOKENS,
		indexes = {
				@Index(name = AppConstants.Indexes.UX_PASSWORD_RESET_TOKENS_TOKEN_HASH,
						columnList = AppConstants.Columns.TOKEN_HASH, unique = true),
				@Index(name = AppConstants.Indexes.IX_PASSWORD_RESET_TOKENS_USER_ID, columnList = AppConstants.Columns.USER_ID),
				@Index(name = AppConstants.Indexes.IX_PASSWORD_RESET_TOKENS_EXPIRES_AT,
						columnList = AppConstants.Columns.EXPIRES_AT),
				@Index(name = AppConstants.Indexes.IX_PASSWORD_RESET_TOKENS_USER_USED_EXPIRES,
						columnList = AppConstants.Columns.USER_ID + "," + AppConstants.Columns.USED_AT + ","
								+ AppConstants.Columns.EXPIRES_AT)
		})
public class PasswordResetToken extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = AppConstants.Columns.USER_ID, nullable = false,
			foreignKey = @ForeignKey(name = AppConstants.Constraints.FK_PASSWORD_RESET_TOKENS_USER))
	private User user;

	@JsonIgnore
	@Column(name = AppConstants.Columns.TOKEN_HASH, nullable = false, length = AppConstants.FieldLengths.TOKEN_HASH)
	private String tokenHash;

	@Column(name = AppConstants.Columns.EXPIRES_AT, nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = AppConstants.Columns.USED_AT)
	private LocalDateTime usedAt;

	@Column(name = AppConstants.Columns.INVALIDATED_AT)
	private LocalDateTime invalidatedAt;

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

	public LocalDateTime getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(LocalDateTime expiresAt) {
		this.expiresAt = expiresAt;
	}

	public LocalDateTime getUsedAt() {
		return usedAt;
	}

	public void setUsedAt(LocalDateTime usedAt) {
		this.usedAt = usedAt;
	}

	public LocalDateTime getInvalidatedAt() {
		return invalidatedAt;
	}

	public void setInvalidatedAt(LocalDateTime invalidatedAt) {
		this.invalidatedAt = invalidatedAt;
	}
}
