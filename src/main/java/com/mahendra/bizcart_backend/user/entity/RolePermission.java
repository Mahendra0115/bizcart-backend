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
@Table(name = AppConstants.Tables.ROLE_PERMISSIONS,
		indexes = {
				@Index(name = AppConstants.Indexes.UX_ROLE_PERMISSIONS_ROLE_PERMISSION,
						columnList = AppConstants.Columns.ROLE_ID + "," + AppConstants.Columns.PERMISSION_ID, unique = true),
				@Index(name = AppConstants.Indexes.IX_ROLE_PERMISSIONS_ROLE_ID, columnList = AppConstants.Columns.ROLE_ID),
				@Index(name = AppConstants.Indexes.IX_ROLE_PERMISSIONS_PERMISSION_ID, columnList = AppConstants.Columns.PERMISSION_ID)
		})
public class RolePermission extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = AppConstants.Columns.ROLE_ID, nullable = false,
			foreignKey = @ForeignKey(name = AppConstants.Constraints.FK_ROLE_PERMISSIONS_ROLE))
	private Role role;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = AppConstants.Columns.PERMISSION_ID, nullable = false,
			foreignKey = @ForeignKey(name = AppConstants.Constraints.FK_ROLE_PERMISSIONS_PERMISSION))
	private Permission permission;

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public Permission getPermission() {
		return permission;
	}

	public void setPermission(Permission permission) {
		this.permission = permission;
	}
}
