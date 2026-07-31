package com.mahendra.bizcart_backend.address.mapper;

import com.mahendra.bizcart_backend.address.dto.request.CreateAddressRequestDto;
import com.mahendra.bizcart_backend.address.dto.request.UpdateAddressRequestDto;
import com.mahendra.bizcart_backend.address.dto.response.AddressResponseDto;
import com.mahendra.bizcart_backend.address.entity.Address;
import com.mahendra.bizcart_backend.address.enums.AddressType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AddressMapper {

	public Address fromCreateRequest(CreateAddressRequestDto request) {
		Address address = new Address();
		apply(address, request.fullName(), request.phone(), request.addressLine1(), request.addressLine2(),
				request.landmark(), request.city(), request.state(), request.postalCode(), request.country(),
				request.addressType());
		return address;
	}

	public void update(Address address, UpdateAddressRequestDto request) {
		apply(address, request.fullName(), request.phone(), request.addressLine1(), request.addressLine2(),
				request.landmark(), request.city(), request.state(), request.postalCode(), request.country(),
				request.addressType());
	}

	public AddressResponseDto toResponse(Address address) {
		return new AddressResponseDto(address.getId(), address.getFullName(), address.getPhone(),
				address.getAddressLine1(), address.getAddressLine2(), address.getLandmark(), address.getCity(),
				address.getState(), address.getPostalCode(), address.getCountry(), address.getAddressType(),
				address.isDefaultAddress(), address.getCreatedAt(), address.getUpdatedAt());
	}

	private void apply(Address address, String fullName, String phone, String addressLine1, String addressLine2,
			String landmark, String city, String state, String postalCode, String country,
			AddressType addressType) {
		address.setFullName(fullName.trim());
		address.setPhone(phone.trim());
		address.setAddressLine1(addressLine1.trim());
		address.setAddressLine2(normalizeNullable(addressLine2));
		address.setLandmark(normalizeNullable(landmark));
		address.setCity(city.trim());
		address.setState(state.trim());
		address.setPostalCode(postalCode.trim());
		address.setCountry(country.trim());
		address.setAddressType(addressType);
	}

	private String normalizeNullable(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
