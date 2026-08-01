package com.mahendra.bizcart_backend.user.entity;

import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.common.entity.BaseEntity;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
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
@Table(name = AppConstants.Tables.USER_STATUS_HISTORY,
		indexes = {
				@Index(name = AppConstants.Indexes.IX_USER_STATUS_HISTORY_TARGET_CHANGED,
						columnList = AppConstants.Columns.TARGET_USER_ID + "," + AppConstants.Columns.CHANGED_AT),
				@Index(name = AppConstants.Indexes.IX_USER_STATUS_HISTORY_ADMIN_CHANGED,
						columnList = AppConstants.Columns.ADMIN_USER_ID + "," + AppConstants.Columns.CHANGED_AT)
		})
public class UserStatusHistory extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = AppConstants.Columns.TARGET_USER_ID, nullable = false,
			foreignKey = @ForeignKey(name = AppConstants.Constraints.FK_USER_STATUS_HISTORY_TARGET))
	private User targetUser;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = AppConstants.Columns.ADMIN_USER_ID, nullable = false,
			foreignKey = @ForeignKey(name = AppConstants.Constraints.FK_USER_STATUS_HISTORY_ADMIN))
	private User adminUser;

	@Enumerated(EnumType.STRING)
	@Column(name = AppConstants.Columns.OLD_STATUS, nullable = false, length = AppConstants.FieldLengths.ENUM)
	private AccountStatus oldStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = AppConstants.Columns.NEW_STATUS, nullable = false, length = AppConstants.FieldLengths.ENUM)
	private AccountStatus newStatus;

	@Column(name = AppConstants.Columns.REASON, length = AppConstants.FieldLengths.DESCRIPTION)
	private String reason;

	@Column(name = AppConstants.Columns.CHANGED_AT, nullable = false, updatable = false)
	private LocalDateTime changedAt;

	public User getTargetUser() { return targetUser; }
	public void setTargetUser(User targetUser) { this.targetUser = targetUser; }
	public User getAdminUser() { return adminUser; }
	public void setAdminUser(User adminUser) { this.adminUser = adminUser; }
	public AccountStatus getOldStatus() { return oldStatus; }
	public void setOldStatus(AccountStatus oldStatus) { this.oldStatus = oldStatus; }
	public AccountStatus getNewStatus() { return newStatus; }
	public void setNewStatus(AccountStatus newStatus) { this.newStatus = newStatus; }
	public String getReason() { return reason; }
	public void setReason(String reason) { this.reason = reason; }
	public LocalDateTime getChangedAt() { return changedAt; }
	public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
