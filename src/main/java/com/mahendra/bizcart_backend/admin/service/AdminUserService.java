package com.mahendra.bizcart_backend.admin.service;

import com.mahendra.bizcart_backend.admin.dto.AdminUserRequest;
import com.mahendra.bizcart_backend.admin.dto.AdminUserResponse;
import com.mahendra.bizcart_backend.admin.dto.UpdateAdminUserRequest;
import com.mahendra.bizcart_backend.authentication.entity.RefreshTokenRevocationReason;
import com.mahendra.bizcart_backend.authentication.repository.RefreshTokenRepository;
import com.mahendra.bizcart_backend.user.entity.Role;
import com.mahendra.bizcart_backend.user.entity.User;
import com.mahendra.bizcart_backend.user.entity.UserRole;
import com.mahendra.bizcart_backend.user.enums.AccountStatus;
import com.mahendra.bizcart_backend.user.enums.UserType;
import com.mahendra.bizcart_backend.user.repository.RoleRepository;
import com.mahendra.bizcart_backend.user.repository.UserRepository;
import com.mahendra.bizcart_backend.user.repository.UserRoleRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminUserService {
	private final UserRepository users;
	private final RoleRepository roles;
	private final UserRoleRepository userRoles;
	private final RefreshTokenRepository refreshTokens;
	private final PasswordEncoder passwordEncoder;

	public AdminUserService(UserRepository users, RoleRepository roles, UserRoleRepository userRoles,
			RefreshTokenRepository refreshTokens, PasswordEncoder passwordEncoder) {
		this.users = users; this.roles = roles; this.userRoles = userRoles;
		this.refreshTokens = refreshTokens; this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public AdminUserResponse create(AdminUserRequest request) {
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		String username = request.username().trim().toLowerCase(Locale.ROOT);
		if (users.existsByEmail(email) || users.existsByUsername(username)) throw conflict("Email or username is already registered");
		User user = new User();
		user.setFirstName(request.firstName().trim()); user.setLastName(request.lastName().trim());
		user.setUsername(username); user.setEmail(email); user.setPhone(nullable(request.phone()));
		user.setProfileImage(nullable(request.profileImage())); user.setPassword(passwordEncoder.encode(request.password()));
		user.setUserType(UserType.ADMIN); user.setStatus(AccountStatus.ACTIVE); user.setEmailVerified(true);
		user.setAdminApproved(true); user.setTokenVersion(0L);
		user = users.save(user); replaceRoles(user, request.roleIds());
		return response(user);
	}

	@Transactional(readOnly = true)
	public List<AdminUserResponse> list() {
		return users.findAll().stream().filter(user -> user.getUserType() == UserType.ADMIN)
			.sorted(Comparator.comparing(User::getId)).map(this::response).toList();
	}

	@Transactional(readOnly = true)
	public AdminUserResponse get(Long id) { return response(findAdmin(id)); }

	@Transactional
	public AdminUserResponse update(Long id, UpdateAdminUserRequest request) {
		User user = findAdmin(id);
		user.setFirstName(request.firstName().trim()); user.setLastName(request.lastName().trim());
		String phone = nullable(request.phone());
		if (phone != null && users.existsByPhoneAndIdNot(phone, id)) throw conflict("Phone number is already registered");
		user.setPhone(phone); user.setProfileImage(nullable(request.profileImage()));
		replaceRoles(user, request.roleIds());
		user.setTokenVersion(user.getTokenVersion() + 1);
		refreshTokens.revokeActiveTokensByUserId(id, LocalDateTime.now(), RefreshTokenRevocationReason.ADMIN_REVOKED, LocalDateTime.now());
		return response(user);
	}

	@Transactional
	public void deactivate(Long actorId, Long id) {
		if (actorId.equals(id)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot deactivate your own account");
		User user = findAdmin(id); user.setStatus(AccountStatus.INACTIVE); user.setTokenVersion(user.getTokenVersion() + 1);
		LocalDateTime now = LocalDateTime.now();
		refreshTokens.revokeActiveTokensByUserId(id, now, RefreshTokenRevocationReason.ADMIN_REVOKED, now);
	}

	private User findAdmin(Long id) {
		User user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));
		if (user.getUserType() != UserType.ADMIN) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found");
		return user;
	}
	private void replaceRoles(User user, Set<Long> roleIds) {
		List<Role> selected = roleIds.stream().map(roleId -> roles.findById(roleId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found: " + roleId))).toList();
		if (selected.stream().noneMatch(role -> "ADMIN".equals(role.getName()))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An admin user must have the ADMIN role");
		}
		userRoles.deleteByUserId(user.getId());
		for (Role role : selected) { UserRole mapping = new UserRole(); mapping.setUser(user); mapping.setRole(role); userRoles.save(mapping); }
	}
	private AdminUserResponse response(User user) {
		List<String> assignedRoles = roles.findByUserId(user.getId()).stream().map(Role::getName).sorted().toList();
		return new AdminUserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getUsername(), user.getEmail(),
				user.getPhone(), user.getProfileImage(), user.getStatus(), assignedRoles);
	}
	private String nullable(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
	private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
