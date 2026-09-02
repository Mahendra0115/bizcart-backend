package com.mahendra.bizcart_backend.admin.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mahendra.bizcart_backend.admin.dto.AdminUserRequest;
import com.mahendra.bizcart_backend.authentication.repository.RefreshTokenRepository;
import com.mahendra.bizcart_backend.user.entity.Role;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.repository.RoleRepository;
import com.mahendra.bizcart_backend.user.repository.UserRepository;
import com.mahendra.bizcart_backend.user.repository.UserRoleRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

	@Mock UserRepository users;
	@Mock RoleRepository roles;
	@Mock UserRoleRepository userRoles;
	@Mock RefreshTokenRepository refreshTokens;
	@Mock PasswordEncoder passwordEncoder;

	private AdminUserService service;

	@BeforeEach
	void setUp() {
		service = new AdminUserService(users, roles, userRoles, refreshTokens, passwordEncoder);
	}

	@Test
	void normalAdminCannotCreateAdminBecauseAdminRoleIsProtected() {
		when(users.existsByEmail("new@example.com")).thenReturn(false);
		when(users.existsByUsername("newadmin")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encoded");
		when(users.save(org.mockito.ArgumentMatchers.any(User.class))).thenAnswer(invocation -> {
			User user = invocation.getArgument(0); ReflectionTestUtils.setField(user, "id", 9L); return user;
		});
		when(roles.findById(1L)).thenReturn(Optional.of(role(1L, "ADMIN")));
		when(roles.findByUserId(1L)).thenReturn(List.of(role(1L, "ADMIN")));

		assertThatThrownBy(() -> service.create(1L, request(Set.of(1L))))
			.isInstanceOf(ResponseStatusException.class)
			.satisfies(error -> org.assertj.core.api.Assertions.assertThat(((ResponseStatusException) error).getStatusCode())
				.isEqualTo(HttpStatus.FORBIDDEN));

		verify(userRoles, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void superAdminCanCreateAdministrator() {
		when(users.existsByEmail("new@example.com")).thenReturn(false);
		when(users.existsByUsername("newadmin")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encoded");
		when(users.save(org.mockito.ArgumentMatchers.any(User.class))).thenAnswer(invocation -> {
			User user = invocation.getArgument(0); ReflectionTestUtils.setField(user, "id", 9L); return user;
		});
		when(roles.findById(1L)).thenReturn(Optional.of(role(1L, "ADMIN")));
		when(roles.findByUserId(2L)).thenReturn(List.of(role(2L, "SUPER_ADMIN")));
		when(roles.findByUserId(9L)).thenReturn(List.of(role(1L, "ADMIN")));

		service.create(2L, request(Set.of(1L)));

		verify(userRoles).save(org.mockito.ArgumentMatchers.any());
	}

	private AdminUserRequest request(Set<Long> roleIds) {
		return new AdminUserRequest("New", "Admin", "newadmin", "new@example.com", null,
				"password123", null, roleIds);
	}

	private Role role(Long id, String name) {
		Role role = new Role(); role.setName(name); ReflectionTestUtils.setField(role, "id", id); return role;
	}
}
