package com.mahendra.bizcart_backend.address.dto.response;

public record AddressApiResponseDto<T>(
		String message,
		T data) {
}
