package com.mahendra.bizcart_backend.address.service;

import com.mahendra.bizcart_backend.address.config.AddressProperties;
import com.mahendra.bizcart_backend.address.dto.request.CreateAddressRequestDto;
import com.mahendra.bizcart_backend.address.dto.request.UpdateAddressRequestDto;
import com.mahendra.bizcart_backend.address.dto.response.AddressResponseDto;
import com.mahendra.bizcart_backend.address.entity.Address;
import com.mahendra.bizcart_backend.address.mapper.AddressMapper;
import com.mahendra.bizcart_backend.address.repository.AddressRepository;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AddressService {

	private final AddressRepository addressRepository;
	private final UserRepository userRepository;
	private final AddressMapper addressMapper;
	private final AddressProperties addressProperties;

	public AddressService(AddressRepository addressRepository, UserRepository userRepository,
			AddressMapper addressMapper, AddressProperties addressProperties) {
		this.addressRepository = addressRepository;
		this.userRepository = userRepository;
		this.addressMapper = addressMapper;
		this.addressProperties = addressProperties;
	}

	@Transactional
	public AddressResponseDto create(Long userId, CreateAddressRequestDto request) {
		User user = lockUser(userId);
		long activeAddressCount = addressRepository.countByUserIdAndDeletedFalse(userId);
		if (activeAddressCount >= addressProperties.getMaxActiveAddresses()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, AppConstants.Address.LIMIT_EXCEEDED);
		}
		boolean makeDefault = request.defaultAddress() || activeAddressCount == 0;
		if (makeDefault) {
			addressRepository.clearDefaultAddresses(userId);
		}

		Address address = addressMapper.fromCreateRequest(request);
		address.setUser(user);
		address.setDefaultAddress(makeDefault);
		address.setDeleted(false);
		return addressMapper.toResponse(addressRepository.save(address));
	}

	@Transactional(readOnly = true)
	public List<AddressResponseDto> list(Long userId) {
		return addressRepository.findAllByUserIdAndDeletedFalseOrderByDefaultAddressDescCreatedAtDesc(userId)
			.stream()
			.map(addressMapper::toResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public AddressResponseDto get(Long userId, Long addressId) {
		return addressMapper.toResponse(findOwnedActiveAddress(userId, addressId));
	}

	@Transactional
	public AddressResponseDto update(Long userId, Long addressId, UpdateAddressRequestDto request) {
		lockUser(userId);
		Address address = findOwnedActiveAddress(userId, addressId);
		if (request.defaultAddress() && !address.isDefaultAddress()) {
			addressRepository.clearDefaultAddresses(userId);
			address.setDefaultAddress(true);
		}
		addressMapper.update(address, request);
		return addressMapper.toResponse(addressRepository.save(address));
	}

	@Transactional
	public void delete(Long userId, Long addressId) {
		lockUser(userId);
		Address address = findOwnedActiveAddress(userId, addressId);
		boolean wasDefault = address.isDefaultAddress();
		address.setDefaultAddress(false);
		address.setDeleted(true);
		addressRepository.saveAndFlush(address);

		if (wasDefault) {
			addressRepository.findFirstByUserIdAndDeletedFalseAndIdNotOrderByIdAsc(userId, addressId)
				.ifPresent(replacement -> {
					replacement.setDefaultAddress(true);
					addressRepository.save(replacement);
				});
		}
	}

	@Transactional
	public AddressResponseDto setDefault(Long userId, Long addressId) {
		lockUser(userId);
		Address address = findOwnedActiveAddress(userId, addressId);
		if (!address.isDefaultAddress()) {
			addressRepository.clearDefaultAddresses(userId);
			address.setDefaultAddress(true);
			address = addressRepository.save(address);
		}
		return addressMapper.toResponse(address);
	}

	private User lockUser(Long userId) {
		return userRepository.findByIdForUpdate(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, AppConstants.User.USER_NOT_FOUND));
	}

	private Address findOwnedActiveAddress(Long userId, Long addressId) {
		return addressRepository.findByIdAndUserIdAndDeletedFalse(addressId, userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, AppConstants.Address.NOT_FOUND));
	}
}
