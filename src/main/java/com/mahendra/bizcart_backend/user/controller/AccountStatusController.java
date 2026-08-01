package com.mahendra.bizcart_backend.user.controller;

import com.mahendra.bizcart_backend.authentication.security.AuthenticatedUserDetails;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.user.dto.request.UpdateAccountStatusRequestDto;
import com.mahendra.bizcart_backend.user.dto.response.AccountStatusResponseDto;
import com.mahendra.bizcart_backend.user.dto.response.UserResponseDto;
import com.mahendra.bizcart_backend.user.service.AccountStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConstants.User.API_ADMIN_BASE)
@Tag(name = "User Account Status", description = "Admin-controlled user account status management")
public class AccountStatusController {

	private final AccountStatusService accountStatusService;

	public AccountStatusController(AccountStatusService accountStatusService) {
		this.accountStatusService = accountStatusService;
	}

	@PatchMapping(AppConstants.User.ADMIN_STATUS_PATH)
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Update a user's account status")
	public UserResponseDto<AccountStatusResponseDto> updateStatus(
			@AuthenticationPrincipal AuthenticatedUserDetails authenticatedAdmin,
			@PathVariable Long userId,
			@Valid @RequestBody UpdateAccountStatusRequestDto request) {
		return new UserResponseDto<>(AppConstants.User.STATUS_UPDATE_SUCCESS,
				accountStatusService.updateStatus(authenticatedAdmin.getId(), userId, request));
	}
}
