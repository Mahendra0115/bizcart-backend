package com.mahendra.bizcart_backend.address.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mahendra.bizcart_backend.address.config.AddressProperties;
import com.mahendra.bizcart_backend.address.dto.request.CreateAddressRequestDto;
import com.mahendra.bizcart_backend.address.dto.request.UpdateAddressRequestDto;
import com.mahendra.bizcart_backend.address.dto.response.AddressResponseDto;
import com.mahendra.bizcart_backend.address.entity.Address;
import com.mahendra.bizcart_backend.address.enums.AddressType;
import com.mahendra.bizcart_backend.address.mapper.AddressMapper;
import com.mahendra.bizcart_backend.address.repository.AddressRepository;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

	@Mock AddressRepository addressRepository;
	@Mock UserRepository userRepository;

	private AddressService service;
	private final AddressMapper mapper = new AddressMapper();
	private final AddressProperties properties = new AddressProperties();

	@BeforeEach
	void setUp() {
		service = new AddressService(addressRepository, userRepository, mapper, properties);
	}

	@Test
	void createMakesFirstAddressDefaultAutomatically() {
		User user = user(1L);
		when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
		when(addressRepository.countByUserIdAndDeletedFalse(1L)).thenReturn(0L);
		when(addressRepository.save(org.mockito.ArgumentMatchers.any(Address.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		AddressResponseDto response = service.create(1L, createRequest(false));

		assertThat(response.defaultAddress()).isTrue();
		verify(addressRepository).clearDefaultAddresses(1L);
	}

	@Test
	void createKeepsExistingDefaultWhenNewAddressIsNotRequestedAsDefault() {
		when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user(1L)));
		when(addressRepository.countByUserIdAndDeletedFalse(1L)).thenReturn(1L);
		when(addressRepository.save(org.mockito.ArgumentMatchers.any(Address.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		AddressResponseDto response = service.create(1L, createRequest(false));

		assertThat(response.defaultAddress()).isFalse();
		verify(addressRepository, never()).clearDefaultAddresses(1L);
	}

	@Test
	void createRejectsAddressWhenActiveLimitIsReached() {
		when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user(1L)));
		when(addressRepository.countByUserIdAndDeletedFalse(1L)).thenReturn(20L);

		assertThatThrownBy(() -> service.create(1L, createRequest(false)))
			.isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
				assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
				assertThat(exception.getReason()).isEqualTo(AppConstants.Address.LIMIT_EXCEEDED);
			});

		verify(addressRepository, never()).clearDefaultAddresses(1L);
		verify(addressRepository, never()).save(org.mockito.ArgumentMatchers.any(Address.class));
	}

	@Test
	void getDoesNotExposeAnotherUsersAddress() {
		when(addressRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(1L, 10L))
			.isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
				assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
				assertThat(exception.getReason()).isEqualTo(AppConstants.Address.NOT_FOUND);
			});
	}

	@Test
	void updateCanSetOwnedAddressAsDefaultTransactionally() {
		Address address = address(10L, user(1L), false);
		when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(address.getUser()));
		when(addressRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(address));
		when(addressRepository.save(address)).thenReturn(address);

		AddressResponseDto response = service.update(1L, 10L, updateRequest(true));

		assertThat(response.defaultAddress()).isTrue();
		verify(addressRepository).clearDefaultAddresses(1L);
	}

	@Test
	void deletingDefaultSoftDeletesAndPromotesAnotherAddress() {
		User user = user(1L);
		Address deleted = address(10L, user, true);
		Address replacement = address(11L, user, false);
		when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
		when(addressRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(deleted));
		when(addressRepository.findFirstByUserIdAndDeletedFalseAndIdNotOrderByIdAsc(1L, 10L))
			.thenReturn(Optional.of(replacement));

		service.delete(1L, 10L);

		assertThat(deleted.isDeleted()).isTrue();
		assertThat(deleted.isDefaultAddress()).isFalse();
		assertThat(replacement.isDefaultAddress()).isTrue();
		verify(addressRepository).saveAndFlush(deleted);
		verify(addressRepository).save(replacement);
	}

	@Test
	void setDefaultIsIdempotentForCurrentDefault() {
		User user = user(1L);
		Address address = address(10L, user, true);
		when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
		when(addressRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(address));

		AddressResponseDto response = service.setDefault(1L, 10L);

		assertThat(response.defaultAddress()).isTrue();
		verify(addressRepository, never()).clearDefaultAddresses(1L);
		verify(addressRepository, never()).save(address);
	}

	@Test
	void listReturnsOnlyRepositoryProvidedActiveAddresses() {
		User user = user(1L);
		Address defaultAddress = address(10L, user, true);
		Address otherAddress = address(11L, user, false);
		when(addressRepository.findAllByUserIdAndDeletedFalseOrderByDefaultAddressDescCreatedAtDesc(1L))
			.thenReturn(List.of(defaultAddress, otherAddress));

		List<AddressResponseDto> response = service.list(1L);

		assertThat(response).extracting(AddressResponseDto::id).containsExactly(10L, 11L);
		assertThat(response.getFirst().defaultAddress()).isTrue();
	}

	private CreateAddressRequestDto createRequest(boolean defaultAddress) {
		return new CreateAddressRequestDto("Test Customer", "+919876543210", "B-101 Sector 62", "Near Metro",
				"Electronic City", "Noida", "Uttar Pradesh", "201309", "India", AddressType.HOME, defaultAddress);
	}

	private UpdateAddressRequestDto updateRequest(boolean defaultAddress) {
		return new UpdateAddressRequestDto("Updated Customer", "+919876543211", "B-102 Sector 62", null,
				null, "Noida", "Uttar Pradesh", "201309", "India", AddressType.WORK, defaultAddress);
	}

	private User user(Long id) {
		User user = new User();
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private Address address(Long id, User user, boolean defaultAddress) {
		Address address = mapper.fromCreateRequest(createRequest(defaultAddress));
		ReflectionTestUtils.setField(address, "id", id);
		address.setUser(user);
		address.setDefaultAddress(defaultAddress);
		return address;
	}
}
