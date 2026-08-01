package com.mahendra.bizcart_backend.address.controller;

import com.mahendra.bizcart_backend.authentication.security.AuthenticatedUserDetails;
import com.mahendra.bizcart_backend.address.dto.request.CreateAddressRequestDto;
import com.mahendra.bizcart_backend.address.dto.request.UpdateAddressRequestDto;
import com.mahendra.bizcart_backend.address.dto.response.AddressApiResponseDto;
import com.mahendra.bizcart_backend.address.dto.response.AddressResponseDto;
import com.mahendra.bizcart_backend.address.service.AddressService;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConstants.Address.API_BASE)
@Tag(name = "User Addresses", description = "Authenticated user's delivery address management")
public class AddressController {

	private final AddressService addressService;

	public AddressController(AddressService addressService) {
		this.addressService = addressService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create an address")
	public AddressApiResponseDto<AddressResponseDto> create(
			@AuthenticationPrincipal AuthenticatedUserDetails authenticatedUser,
			@Valid @RequestBody CreateAddressRequestDto request) {
		return new AddressApiResponseDto<>(AppConstants.Address.CREATED_SUCCESS,
				addressService.create(authenticatedUser.getId(), request));
	}

	@GetMapping
	@Operation(summary = "List the authenticated user's active addresses")
	public AddressApiResponseDto<List<AddressResponseDto>> list(
			@AuthenticationPrincipal AuthenticatedUserDetails authenticatedUser) {
		return new AddressApiResponseDto<>(AppConstants.Address.LIST_FETCH_SUCCESS,
				addressService.list(authenticatedUser.getId()));
	}

	@GetMapping("/{addressId}")
	@Operation(summary = "Get one owned active address")
	public AddressApiResponseDto<AddressResponseDto> get(
			@AuthenticationPrincipal AuthenticatedUserDetails authenticatedUser,
			@PathVariable Long addressId) {
		return new AddressApiResponseDto<>(AppConstants.Address.FETCH_SUCCESS,
				addressService.get(authenticatedUser.getId(), addressId));
	}

	@PutMapping("/{addressId}")
	@Operation(summary = "Replace one owned active address")
	public AddressApiResponseDto<AddressResponseDto> update(
			@AuthenticationPrincipal AuthenticatedUserDetails authenticatedUser,
			@PathVariable Long addressId,
			@Valid @RequestBody UpdateAddressRequestDto request) {
		return new AddressApiResponseDto<>(AppConstants.Address.UPDATE_SUCCESS,
				addressService.update(authenticatedUser.getId(), addressId, request));
	}

	@DeleteMapping("/{addressId}")
	@Operation(summary = "Soft-delete one owned active address")
	public AddressApiResponseDto<Void> delete(
			@AuthenticationPrincipal AuthenticatedUserDetails authenticatedUser,
			@PathVariable Long addressId) {
		addressService.delete(authenticatedUser.getId(), addressId);
		return new AddressApiResponseDto<>(AppConstants.Address.DELETE_SUCCESS, null);
	}

	@PutMapping("/{addressId}/default")
	@Operation(summary = "Set one owned active address as default")
	public AddressApiResponseDto<AddressResponseDto> setDefault(
			@AuthenticationPrincipal AuthenticatedUserDetails authenticatedUser,
			@PathVariable Long addressId) {
		return new AddressApiResponseDto<>(AppConstants.Address.DEFAULT_UPDATE_SUCCESS,
				addressService.setDefault(authenticatedUser.getId(), addressId));
	}
}
