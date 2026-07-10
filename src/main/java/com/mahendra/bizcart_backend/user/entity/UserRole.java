package com.mahendra.bizcart_backend.user.entity;

import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = AppConstants.Tables.USER_ROLES,
		indexes = {
				@Index(name = AppConstants.Indexes.UX_USER_ROLES_USER_ROLE,
						columnList = AppConstants.Columns.USER_ID + "," + AppConstants.Columns.ROLE_ID, unique = true),
				@Index(name = AppConstants.Indexes.IX_USER_ROLES_USER_ID, columnList = AppConstants.Columns.USER_ID),
				@Index(name = AppConstants.Indexes.IX_USER_ROLES_ROLE_ID, columnList = AppConstants.Columns.ROLE_ID)
		})
public class UserRole extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = AppConstants.Columns.USER_ID, nullable = false,
			foreignKey = @ForeignKey(name = AppConstants.Constraints.FK_USER_ROLES_USER))
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = AppConstants.Columns.ROLE_ID, nullable = false,
			foreignKey = @ForeignKey(name = AppConstants.Constraints.FK_USER_ROLES_ROLE))
	private Role role;

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}
}
