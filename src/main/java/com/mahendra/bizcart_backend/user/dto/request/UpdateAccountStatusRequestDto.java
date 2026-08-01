package com.mahendra.bizcart_backend.user.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAccountStatusRequestDto(
		@NotNull AccountStatus status,
		@Size(max = AppConstants.FieldLengths.DESCRIPTION) String reason
) {
	@JsonAnySetter
	public void rejectUnknownField(String fieldName, Object value) {
		throw new IllegalArgumentException("Unknown account status field: " + fieldName);
	}
}
