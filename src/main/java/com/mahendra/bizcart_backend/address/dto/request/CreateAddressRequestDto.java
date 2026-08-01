package com.mahendra.bizcart_backend.address.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.mahendra.bizcart_backend.address.enums.AddressType;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAddressRequestDto(
		@NotBlank @Size(max = AppConstants.FieldLengths.NAME) String fullName,
		@NotBlank @Size(max = AppConstants.FieldLengths.PHONE)
		@Pattern(regexp = "^(?:\\+91)?[6-9][0-9]{9}$",
				message = "Phone number must be a valid Indian mobile number") String phone,
		@NotBlank @Size(max = AppConstants.FieldLengths.ADDRESS_LINE) String addressLine1,
		@Size(max = AppConstants.FieldLengths.ADDRESS_LINE) String addressLine2,
		@Size(max = AppConstants.FieldLengths.LANDMARK) String landmark,
		@NotBlank @Size(max = AppConstants.FieldLengths.CITY) String city,
		@NotBlank @Size(max = AppConstants.FieldLengths.STATE) String state,
		@NotBlank @Size(max = AppConstants.FieldLengths.POSTAL_CODE)
		@Pattern(regexp = "^[1-9][0-9]{5}$",
				message = "Postal code must be a valid 6-digit Indian PIN code") String postalCode,
		@NotBlank @Size(max = AppConstants.FieldLengths.COUNTRY) String country,
		@NotNull AddressType addressType,
		boolean defaultAddress
) {
	@JsonAnySetter
	public void rejectUnknownField(String fieldName, Object value) {
		throw new IllegalArgumentException("Unknown address field: " + fieldName);
	}
}
