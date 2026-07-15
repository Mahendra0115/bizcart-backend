package com.mahendra.bizcart_backend.authentication.entity;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = AppConstants.Tables.LOGIN_ATTEMPTS,
		indexes = {
				@Index(name = AppConstants.Indexes.IX_LOGIN_ATTEMPTS_EMAIL, columnList = AppConstants.Columns.EMAIL),
				@Index(name = AppConstants.Indexes.IX_LOGIN_ATTEMPTS_IP_ADDRESS,
						columnList = AppConstants.Columns.IP_ADDRESS),
				@Index(name = AppConstants.Indexes.IX_LOGIN_ATTEMPTS_ATTEMPTED_AT,
						columnList = AppConstants.Columns.ATTEMPTED_AT),
				@Index(name = AppConstants.Indexes.IX_LOGIN_ATTEMPTS_EMAIL_ATTEMPTED,
						columnList = AppConstants.Columns.EMAIL + "," + AppConstants.Columns.ATTEMPTED_AT)
		})
public class LoginAttempt extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = AppConstants.Columns.USER_ID,
			foreignKey = @ForeignKey(name = AppConstants.Constraints.FK_LOGIN_ATTEMPTS_USER))
	private User user;

	@Column(name = AppConstants.Columns.EMAIL, length = AppConstants.FieldLengths.EMAIL)
	private String email;

	@Column(name = AppConstants.Columns.IP_ADDRESS, length = AppConstants.FieldLengths.IP_ADDRESS)
	private String ipAddress;

	@Column(name = AppConstants.Columns.USER_AGENT, length = AppConstants.FieldLengths.USER_AGENT)
	private String userAgent;

	@Column(name = AppConstants.Columns.WAS_SUCCESSFUL, nullable = false)
	private boolean wasSuccessful;

	@Column(name = AppConstants.Columns.FAILURE_REASON, length = AppConstants.FieldLengths.FAILURE_REASON)
	private String failureReason;

	@Column(name = AppConstants.Columns.ATTEMPTED_AT, nullable = false)
	private LocalDateTime attemptedAt;

	@Override
	@PrePersist
	protected void onCreate() {
		super.onCreate();
		if (attemptedAt == null) {
			attemptedAt = LocalDateTime.now();
		}
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public boolean isWasSuccessful() {
		return wasSuccessful;
	}

	public void setWasSuccessful(boolean wasSuccessful) {
		this.wasSuccessful = wasSuccessful;
	}

	public String getFailureReason() {
		return failureReason;
	}

	public void setFailureReason(String failureReason) {
		this.failureReason = failureReason;
	}

	public LocalDateTime getAttemptedAt() {
		return attemptedAt;
	}

	public void setAttemptedAt(LocalDateTime attemptedAt) {
		this.attemptedAt = attemptedAt;
	}
}
