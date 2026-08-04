package com.mahendra.bizcart_backend.admin.config;

import com.mahendra.bizcart_backend.user.entity.Role;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.entity.UserRole;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import com.mahendra.bizcart_backend.user.repository.RoleRepository;
import com.mahendra.bizcart_backend.user.repository.UserRepository;
import com.mahendra.bizcart_backend.user.repository.UserRoleRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

@Configuration
public class AdminBootstrapInitializer {
	@Bean
	ApplicationRunner initializeRbacAndOptionalAdmin(RoleRepository roles, UserRepository users, UserRoleRepository userRoles,
			AdminBootstrapProperties properties, PasswordEncoder encoder) {
		return args -> {
			Role adminRole = ensureRole(roles, "ADMIN", "Platform administrator");
			ensureRole(roles, "CUSTOMER", "Customer account"); ensureRole(roles, "SELLER", "Seller account");
			if (!properties.isEnabled() || !StringUtils.hasText(properties.getEmail()) || !StringUtils.hasText(properties.getPassword())) return;
			if (users.existsByEmail(properties.getEmail().trim().toLowerCase())) return;
			User user = new User(); user.setFirstName(value(properties.getFirstName(), "System")); user.setLastName(value(properties.getLastName(), "Admin"));
			user.setUsername(value(properties.getUsername(), "admin").toLowerCase()); user.setEmail(properties.getEmail().trim().toLowerCase());
			user.setPassword(encoder.encode(properties.getPassword())); user.setUserType(UserType.ADMIN); user.setStatus(AccountStatus.ACTIVE);
			user.setEmailVerified(true); user.setAdminApproved(true); user.setTokenVersion(0L); user = users.save(user);
			UserRole mapping = new UserRole(); mapping.setUser(user); mapping.setRole(adminRole); userRoles.save(mapping);
		};
	}
	private Role ensureRole(RoleRepository roles, String name, String description) { return roles.findByName(name).orElseGet(() -> { Role role = new Role(); role.setName(name); role.setDescription(description); return roles.save(role); }); }
	private String value(String value, String fallback) { return StringUtils.hasText(value) ? value.trim() : fallback; }
}
