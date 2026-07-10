package com.mahendra.bizcart_backend.authentication.security;

import com.mahendra.bizcart_backend.user.entity.Permission;
import com.mahendra.bizcart_backend.user.entity.Role;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.repository.PermissionRepository;
import com.mahendra.bizcart_backend.user.repository.RoleRepository;
import com.mahendra.bizcart_backend.user.repository.UserRepository;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private static final String USER_NOT_FOUND_MESSAGE = "User not found";
	public static final String ROLE_PREFIX = "ROLE_";

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PermissionRepository permissionRepository;

	public CustomUserDetailsService(UserRepository userRepository, RoleRepository roleRepository,
			PermissionRepository permissionRepository) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.permissionRepository = permissionRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String email) {
		String normalizedEmail = normalizeEmail(email);
		User user = userRepository.findByNormalizedEmail(normalizedEmail)
			.orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_MESSAGE));
		return new AuthenticatedUserDetails(user, loadAuthorities(user.getId()));
	}

	private Set<GrantedAuthority> loadAuthorities(Long userId) {
		Set<GrantedAuthority> authorities = new LinkedHashSet<>();
		for (Role role : roleRepository.findByUserId(userId)) {
			authorities.add(new SimpleGrantedAuthority(toRoleAuthority(role.getName())));
		}
		for (Permission permission : permissionRepository.findByUserId(userId)) {
			authorities.add(new SimpleGrantedAuthority(permission.getName()));
		}
		return authorities;
	}

	private String normalizeEmail(String email) {
		if (!StringUtils.hasText(email)) {
			throw new UsernameNotFoundException(USER_NOT_FOUND_MESSAGE);
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}

	public static String toRoleAuthority(String roleName) {
		if (roleName == null || roleName.startsWith(ROLE_PREFIX)) {
			return roleName;
		}
		return ROLE_PREFIX + roleName;
	}
}
