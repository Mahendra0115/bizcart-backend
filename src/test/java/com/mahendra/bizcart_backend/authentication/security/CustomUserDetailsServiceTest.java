package com.mahendra.bizcart_backend.authentication.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mahendra.bizcart_backend.user.entity.Permission;
import com.mahendra.bizcart_backend.user.entity.Role;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import com.mahendra.bizcart_backend.user.repository.PermissionRepository;
import com.mahendra.bizcart_backend.user.repository.RoleRepository;
import com.mahendra.bizcart_backend.user.repository.UserRepository;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

class CustomUserDetailsServiceTest {

	@Test
	void loadUserByUsernameNormalizesEmailAndLoadsAuthorities() {
		User user = user();
		Role role = role("CUSTOMER");
		Permission permission = permission("ORDER_READ");
		CustomUserDetailsService customUserDetailsService = new CustomUserDetailsService(
				userRepository(Optional.of(user)), roleRepository(List.of(role)), permissionRepository(List.of(permission)));

		AuthenticatedUserDetails details = (AuthenticatedUserDetails) customUserDetailsService
			.loadUserByUsername(" Customer@Example.COM ");

		assertThat(details.getUsername()).isEqualTo("customer@example.com");
		assertThat(details.getAuthorities()).extracting(Object::toString)
			.containsExactly("ROLE_CUSTOMER", "ORDER_READ");
	}

	@Test
	void loadUserByUsernameThrowsWhenUserDoesNotExist() {
		CustomUserDetailsService customUserDetailsService = new CustomUserDetailsService(
				userRepository(Optional.empty()), roleRepository(List.of()), permissionRepository(List.of()));

		assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("missing@example.com"))
			.isInstanceOf(UsernameNotFoundException.class);
	}

	private User user() {
		User user = new User();
		ReflectionTestUtils.setField(user, "id", 22L);
		user.setFirstName("Test");
		user.setLastName("Customer");
		user.setEmail("customer@example.com");
		user.setPassword("$2a$12$encodedPasswordPlaceholder");
		user.setUserType(UserType.CUSTOMER);
		user.setStatus(AccountStatus.ACTIVE);
		user.setEmailVerified(true);
		user.setTokenVersion(1L);
		return user;
	}

	private Role role(String name) {
		Role role = new Role();
		role.setName(name);
		return role;
	}

	private Permission permission(String name) {
		Permission permission = new Permission();
		permission.setName(name);
		return permission;
	}

	private UserRepository userRepository(Optional<User> user) {
		return repositoryProxy(UserRepository.class, invocation -> {
			if ("findByNormalizedEmail".equals(invocation.methodName())) {
				return user;
			}
			throw unsupported(invocation.methodName());
		});
	}

	private RoleRepository roleRepository(List<Role> roles) {
		return repositoryProxy(RoleRepository.class, invocation -> {
			if ("findByUserId".equals(invocation.methodName())) {
				return roles;
			}
			throw unsupported(invocation.methodName());
		});
	}

	private PermissionRepository permissionRepository(List<Permission> permissions) {
		return repositoryProxy(PermissionRepository.class, invocation -> {
			if ("findByUserId".equals(invocation.methodName())) {
				return permissions;
			}
			throw unsupported(invocation.methodName());
		});
	}

	private <T> T repositoryProxy(Class<T> repositoryType, RepositoryInvocationHandler invocationHandler) {
		Object proxy = Proxy.newProxyInstance(repositoryType.getClassLoader(), new Class<?>[] { repositoryType },
				(proxyObject, method, args) -> {
					if ("toString".equals(method.getName())) {
						return repositoryType.getSimpleName() + "Proxy";
					}
					return invocationHandler.invoke(new RepositoryInvocation(method.getName()));
				});
		return repositoryType.cast(proxy);
	}

	private UnsupportedOperationException unsupported(String methodName) {
		return new UnsupportedOperationException(methodName + " is not needed by this unit test");
	}

	private record RepositoryInvocation(String methodName) {
	}

	@FunctionalInterface
	private interface RepositoryInvocationHandler {

		Object invoke(RepositoryInvocation invocation);
	}
}
