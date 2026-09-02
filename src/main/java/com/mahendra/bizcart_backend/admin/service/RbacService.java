package com.mahendra.bizcart_backend.admin.service;

import com.mahendra.bizcart_backend.admin.dto.PermissionRequest;
import com.mahendra.bizcart_backend.admin.dto.PermissionResponse;
import com.mahendra.bizcart_backend.admin.dto.RoleRequest;
import com.mahendra.bizcart_backend.admin.dto.RoleResponse;
import com.mahendra.bizcart_backend.user.entity.Permission;
import com.mahendra.bizcart_backend.user.entity.Role;
import com.mahendra.bizcart_backend.user.entity.RolePermission;
import com.mahendra.bizcart_backend.user.repository.PermissionRepository;
import com.mahendra.bizcart_backend.user.repository.RolePermissionRepository;
import com.mahendra.bizcart_backend.user.repository.RoleRepository;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RbacService {
	private final RoleRepository roles; private final PermissionRepository permissions; private final RolePermissionRepository rolePermissions;
	public RbacService(RoleRepository roles, PermissionRepository permissions, RolePermissionRepository rolePermissions) {
		this.roles = roles; this.permissions = permissions; this.rolePermissions = rolePermissions;
	}
	@Transactional public RoleResponse createRole(Long actorId, RoleRequest request) { return roleResponse(apply(actorId, new Role(), request, true)); }
	@Transactional(readOnly = true) public List<RoleResponse> listRoles() { return roles.findAllByOrderByNameAsc().stream().map(this::roleResponse).toList(); }
	@Transactional(readOnly = true) public RoleResponse getRole(Long id) { return roleResponse(role(id)); }
	@Transactional public RoleResponse updateRole(Long actorId, Long id, RoleRequest request) { return roleResponse(apply(actorId, role(id), request, false)); }
	@Transactional public void deleteRole(Long actorId, Long id) { Role role = role(id); if (isSystemRole(role.getName())) throw badRequest("System roles cannot be deleted"); rolePermissions.deleteByRoleId(id); roles.delete(role); }

	@Transactional public PermissionResponse createPermission(Long actorId, PermissionRequest request) { Permission permission = new Permission(); apply(actorId, permission, request); return permissionResponse(permission); }
	@Transactional(readOnly = true) public List<PermissionResponse> listPermissions() { return permissions.findAllByOrderByNameAsc().stream().map(this::permissionResponse).toList(); }
	@Transactional(readOnly = true) public PermissionResponse getPermission(Long id) { return permissionResponse(permission(id)); }
	@Transactional public PermissionResponse updatePermission(Long actorId, Long id, PermissionRequest request) { Permission permission = permission(id); apply(actorId, permission, request); return permissionResponse(permission); }
	@Transactional public void deletePermission(Long actorId, Long id) { Permission permission = permission(id); if (isProtectedPermission(permission.getName())) throw badRequest("Protected permissions cannot be deleted"); rolePermissions.findAll().stream().filter(item -> item.getPermission().getId().equals(id)).forEach(rolePermissions::delete); permissions.delete(permission); }

	private Role apply(Long actorId, Role role, RoleRequest request, boolean creating) {
		String name = request.name().trim().toUpperCase(Locale.ROOT);
		if (creating && roles.findByName(name).isPresent()) throw conflict("Role name already exists");
		if (!creating && !role.getName().equals(name)) throw badRequest("Role name cannot be changed");
		if (isSystemRole(name)) requireSuperAdmin(actorId);
		List<Permission> selectedPermissions = (request.permissionIds() == null ? Set.<Long>of() : request.permissionIds()).stream()
			.map(this::permission).toList();
		if (selectedPermissions.stream().anyMatch(permission -> isProtectedPermission(permission.getName()))) requireSuperAdmin(actorId);
		role.setName(name); role.setDescription(blankToNull(request.description())); role = roles.save(role);
		rolePermissions.deleteByRoleId(role.getId());
		for (Permission permission : selectedPermissions) {
			RolePermission mapping = new RolePermission(); mapping.setRole(role); mapping.setPermission(permission); rolePermissions.save(mapping);
		}
		return role;
	}
	private void apply(Long actorId, Permission permission, PermissionRequest request) {
		String name = request.name().trim().toUpperCase(Locale.ROOT);
		permissions.findByName(name).filter(existing -> !existing.getId().equals(permission.getId())).ifPresent(existing -> { throw conflict("Permission name already exists"); });
		if (isProtectedPermission(name)) requireSuperAdmin(actorId);
		permission.setName(name); permission.setDescription(blankToNull(request.description())); permissions.save(permission);
	}
	private void requireSuperAdmin(Long actorId) {
		if (roles.findByUserId(actorId).stream().noneMatch(role -> "SUPER_ADMIN".equals(role.getName()))) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a super administrator can manage protected RBAC settings");
		}
	}
	private boolean isSystemRole(String name) { return List.of("ADMIN", "SUPER_ADMIN", "SYSTEM_OWNER", "CUSTOMER", "SELLER").contains(name); }
	private boolean isProtectedPermission(String name) { return "ADMIN_ROLE_ASSIGN".equals(name); }
	private Role role(Long id) { return roles.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found")); }
	private Permission permission(Long id) { return permissions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permission not found")); }
	private RoleResponse roleResponse(Role role) { return new RoleResponse(role.getId(), role.getName(), role.getDescription(), permissions.findByRoleId(role.getId()).stream().map(Permission::getName).sorted().toList()); }
	private PermissionResponse permissionResponse(Permission permission) { return new PermissionResponse(permission.getId(), permission.getName(), permission.getDescription()); }
	private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
	private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
	private ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
