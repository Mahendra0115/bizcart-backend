package com.mahendra.bizcart_backend.address.dto.response;

import com.mahendra.bizcart_backend.address.enums.AddressType;
import java.time.LocalDateTime;

public record AddressResponseDto(
		Long id,
		String fullName,
		String phone,
		String addressLine1,
		String addressLine2,
		String landmark,
		String city,
		String state,
		String postalCode,
		String country,
		AddressType addressType,
		boolean defaultAddress,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
