package com.mahendra.bizcart_backend.user.service;

import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.user.dto.request.UpdateProfileRequestDto;
import com.mahendra.bizcart_backend.user.dto.response.UserProfileResponseDto;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserProfileService {

	private final UserRepository userRepository;

	public UserProfileService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public UserProfileResponseDto getProfile(Long userId) {
		return toResponse(findUser(userId));
	}

	@Transactional
	public UserProfileResponseDto updateProfile(Long userId, UpdateProfileRequestDto request) {
		User user = findUser(userId);
		String phone = normalizeNullable(request.phone());
		if (phone != null && userRepository.existsByPhoneAndIdNot(phone, userId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, AppConstants.User.DUPLICATE_PHONE);
		}

		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName().trim());
		user.setPhone(phone);
		user.setProfileImage(normalizeNullable(request.profileImage()));
		return toResponse(userRepository.save(user));
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, AppConstants.User.USER_NOT_FOUND));
	}

	private String normalizeNullable(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private UserProfileResponseDto toResponse(User user) {
		return new UserProfileResponseDto(user.getId(), user.getFirstName(), user.getLastName(), user.getUsername(),
				user.getEmail(), user.getPhone(), user.getProfileImage(), user.getUserType(), user.getStatus(),
				user.isEmailVerified(), user.isAdminApproved());
	}
}
