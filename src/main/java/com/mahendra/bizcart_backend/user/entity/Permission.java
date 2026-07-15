package com.mahendra.bizcart_backend.user.entity;

import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = AppConstants.Tables.PERMISSIONS,
		indexes = @Index(name = AppConstants.Indexes.UX_PERMISSIONS_NAME, columnList = AppConstants.Columns.NAME, unique = true))
public class Permission extends BaseEntity {

	@Column(name = AppConstants.Columns.NAME, nullable = false, length = AppConstants.FieldLengths.NAME)
	private String name;

	@Column(name = AppConstants.Columns.DESCRIPTION, length = AppConstants.FieldLengths.DESCRIPTION)
	private String description;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
