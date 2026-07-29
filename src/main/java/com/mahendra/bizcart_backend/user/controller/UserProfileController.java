package com.mahendra.bizcart_backend.user.controller;

import com.mahendra.bizcart_backend.authentication.security.AuthenticatedUserDetails;
import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.user.dto.request.UpdateProfileRequestDto;
import com.mahendra.bizcart_backend.user.dto.response.UserProfileResponseDto;
import com.mahendra.bizcart_backend.user.dto.response.UserResponseDto;
import com.mahendra.bizcart_backend.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConstants.User.API_USERS_BASE)
@Tag(name = "User Profile", description = "Authenticated user profile management")
public class UserProfileController {

	private final UserProfileService userProfileService;

	public UserProfileController(UserProfileService userProfileService) {
		this.userProfileService = userProfileService;
	}

	@GetMapping(AppConstants.User.PROFILE_PATH)
	@Operation(summary = "Get the authenticated user's profile")
	public UserResponseDto<UserProfileResponseDto> getProfile(
			@AuthenticationPrincipal AuthenticatedUserDetails authenticatedUser) {
		return new UserResponseDto<>(AppConstants.User.PROFILE_FETCH_SUCCESS,
				userProfileService.getProfile(authenticatedUser.getId()));
	}

	@PatchMapping(AppConstants.User.PROFILE_PATH)
	@Operation(summary = "Update the authenticated user's editable profile fields",
			description = """
					Updates only the supplied editable profile fields. Omitted fields are preserved. Sending phone or
					profileImage as null or blank clears that field. Restricted account fields are ignored.
					""")
	public UserResponseDto<UserProfileResponseDto> updateProfile(
			@AuthenticationPrincipal AuthenticatedUserDetails authenticatedUser,
			@Valid @RequestBody UpdateProfileRequestDto request) {
		return new UserResponseDto<>(AppConstants.User.PROFILE_UPDATE_SUCCESS,
				userProfileService.updateProfile(authenticatedUser.getId(), request));
	}
}
