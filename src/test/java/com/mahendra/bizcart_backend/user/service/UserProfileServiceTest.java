package com.mahendra.bizcart_backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mahendra.bizcart_backend.common.constants.AppConstants;
import com.mahendra.bizcart_backend.user.dto.request.UpdateProfileRequestDto;
import com.mahendra.bizcart_backend.user.dto.response.UserProfileResponseDto;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import com.mahendra.bizcart_backend.user.repository.UserRepository;
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
class UserProfileServiceTest {

	@Mock
	private UserRepository userRepository;

	private UserProfileService userProfileService;

	@BeforeEach
	void setUp() {
		userProfileService = new UserProfileService(userRepository);
	}

	@Test
	void getProfileReturnsOnlySafeProfileData() {
		User user = user();
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		UserProfileResponseDto response = userProfileService.getProfile(1L);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.email()).isEqualTo("user@example.com");
		assertThat(response.username()).isEqualTo("bizcart-user");
		assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
	}

	@Test
	void updateProfileUpdatesOnlyAllowlistedFieldsAndNormalizesOptionalValues() {
		User user = user();
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userRepository.existsByPhoneAndIdNot("+919876543210", 1L)).thenReturn(false);
		when(userRepository.save(user)).thenReturn(user);

		UserProfileResponseDto response = userProfileService.updateProfile(1L,
				new UpdateProfileRequestDto("  Updated  ", "  User  ", " +919876543210 ", "  https://img.test/me.png  "));

		assertThat(response.firstName()).isEqualTo("Updated");
		assertThat(response.lastName()).isEqualTo("User");
		assertThat(response.phone()).isEqualTo("+919876543210");
		assertThat(response.profileImage()).isEqualTo("https://img.test/me.png");
		assertThat(user.getEmail()).isEqualTo("user@example.com");
		assertThat(user.getUsername()).isEqualTo("bizcart-user");
		verify(userRepository).save(user);
	}

	@Test
	void updateProfileRejectsPhoneOwnedByAnotherUser() {
		User user = user();
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userRepository.existsByPhoneAndIdNot("+919876543210", 1L)).thenReturn(true);

		assertThatThrownBy(() -> userProfileService.updateProfile(1L,
				new UpdateProfileRequestDto("Updated", "User", "+919876543210", null)))
			.isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
				assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
				assertThat(exception.getReason()).isEqualTo(AppConstants.User.DUPLICATE_PHONE);
			});
		verify(userRepository, never()).save(user);
	}

	@Test
	void getProfileReturnsNotFoundWhenAuthenticatedUserNoLongerExists() {
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userProfileService.getProfile(99L))
			.isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
				assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
				assertThat(exception.getReason()).isEqualTo(AppConstants.User.USER_NOT_FOUND);
			});
	}

	private User user() {
		User user = new User();
		ReflectionTestUtils.setField(user, "id", 1L);
		user.setFirstName("Original");
		user.setLastName("User");
		user.setUsername("bizcart-user");
		user.setEmail("user@example.com");
		user.setPhone("+911234567890");
		user.setPassword("encoded");
		user.setProfileImage("https://img.test/original.png");
		user.setUserType(UserType.CUSTOMER);
		user.setStatus(AccountStatus.ACTIVE);
		user.setEmailVerified(true);
		user.setAdminApproved(false);
		return user;
	}
}
