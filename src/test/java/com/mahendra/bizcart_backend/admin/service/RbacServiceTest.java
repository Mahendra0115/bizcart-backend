package com.mahendra.bizcart_backend.admin.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mahendra.bizcart_backend.admin.dto.RoleRequest;
import com.mahendra.bizcart_backend.user.entity.Permission;
import com.mahendra.bizcart_backend.user.entity.Role;
import com.mahendra.bizcart_backend.user.repository.PermissionRepository;
import com.mahendra.bizcart_backend.user.repository.RolePermissionRepository;
import com.mahendra.bizcart_backend.user.repository.RoleRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class RbacServiceTest {

	@Mock RoleRepository roles;
	@Mock PermissionRepository permissions;
	@Mock RolePermissionRepository rolePermissions;

	private RbacService service;

	@BeforeEach
	void setUp() {
		service = new RbacService(roles, permissions, rolePermissions);
	}

	@Test
	void normalAdminCannotGrantProtectedPermissionThroughCustomRole() {
		Permission protectedPermission = permission(7L, "ADMIN_ROLE_ASSIGN");
		when(roles.findByName("OPERATIONS_ADMIN")).thenReturn(Optional.empty());
		when(permissions.findById(7L)).thenReturn(Optional.of(protectedPermission));
		when(roles.findByUserId(1L)).thenReturn(List.of(role(1L, "ADMIN")));

		assertForbidden(() -> service.createRole(1L,
				new RoleRequest("operations_admin", null, Set.of(7L))));

		verify(rolePermissions, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void superAdminCanManageProtectedPermissionWithoutDelegatingSuperAdminStatus() {
		Role newRole = role(3L, "OPERATIONS_ADMIN");
		Permission protectedPermission = permission(7L, "ADMIN_ROLE_ASSIGN");
		when(roles.findByName("OPERATIONS_ADMIN")).thenReturn(Optional.empty());
		when(roles.findByUserId(2L)).thenReturn(List.of(role(2L, "SUPER_ADMIN")));
		when(roles.save(org.mockito.ArgumentMatchers.any(Role.class))).thenReturn(newRole);
		when(permissions.findById(7L)).thenReturn(Optional.of(protectedPermission));
		when(permissions.findByRoleId(3L)).thenReturn(List.of(protectedPermission));

		service.createRole(2L, new RoleRequest("operations_admin", null, Set.of(7L)));

		verify(rolePermissions).save(org.mockito.ArgumentMatchers.any());
	}

	private void assertForbidden(Runnable action) {
		assertThatThrownBy(action::run).isInstanceOf(ResponseStatusException.class)
			.satisfies(error -> org.assertj.core.api.Assertions.assertThat(((ResponseStatusException) error).getStatusCode())
				.isEqualTo(HttpStatus.FORBIDDEN));
	}

	private Role role(Long id, String name) {
		Role role = new Role(); role.setName(name); ReflectionTestUtils.setField(role, "id", id); return role;
	}

	private Permission permission(Long id, String name) {
		Permission permission = new Permission(); permission.setName(name); ReflectionTestUtils.setField(permission, "id", id); return permission;
	}
}
