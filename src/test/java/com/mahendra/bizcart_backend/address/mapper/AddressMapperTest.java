package com.mahendra.bizcart_backend.address.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.mahendra.bizcart_backend.address.dto.request.CreateAddressRequestDto;
import com.mahendra.bizcart_backend.address.dto.request.UpdateAddressRequestDto;
import com.mahendra.bizcart_backend.address.entity.Address;
import com.mahendra.bizcart_backend.address.enums.AddressType;
import org.junit.jupiter.api.Test;

class AddressMapperTest {

	private final AddressMapper mapper = new AddressMapper();

	@Test
	void createNormalizesLocalIndianPhoneToE164() {
		Address address = mapper.fromCreateRequest(createRequest("9876543210"));

		assertThat(address.getPhone()).isEqualTo("+919876543210");
	}

	@Test
	void createPreservesCanonicalIndianE164Phone() {
		Address address = mapper.fromCreateRequest(createRequest("+919876543210"));

		assertThat(address.getPhone()).isEqualTo("+919876543210");
	}

	@Test
	void updateNormalizesLocalIndianPhoneToE164() {
		Address address = new Address();

		mapper.update(address, updateRequest("9876543210"));

		assertThat(address.getPhone()).isEqualTo("+919876543210");
	}

	private CreateAddressRequestDto createRequest(String phone) {
		return new CreateAddressRequestDto("Test Customer", phone, "B-101 Sector 62", null, null,
				"Noida", "Uttar Pradesh", "201309", "India", AddressType.HOME, false);
	}

	private UpdateAddressRequestDto updateRequest(String phone) {
		return new UpdateAddressRequestDto("Test Customer", phone, "B-101 Sector 62", null, null,
				"Noida", "Uttar Pradesh", "201309", "India", AddressType.HOME, false);
	}
}
