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
	@Transactional public RoleResponse createRole(RoleRequest request) { Role role = new Role(); apply(role, request, true); return roleResponse(role); }
	@Transactional(readOnly = true) public List<RoleResponse> listRoles() { return roles.findAllByOrderByNameAsc().stream().map(this::roleResponse).toList(); }
	@Transactional(readOnly = true) public RoleResponse getRole(Long id) { return roleResponse(role(id)); }
	@Transactional public RoleResponse updateRole(Long id, RoleRequest request) { Role role = role(id); apply(role, request, false); return roleResponse(role); }
	@Transactional public void deleteRole(Long id) { Role role = role(id); if (List.of("ADMIN", "CUSTOMER", "SELLER").contains(role.getName())) throw badRequest("System roles cannot be deleted"); rolePermissions.deleteByRoleId(id); roles.delete(role); }

	@Transactional public PermissionResponse createPermission(PermissionRequest request) { Permission permission = new Permission(); apply(permission, request); return permissionResponse(permission); }
	@Transactional(readOnly = true) public List<PermissionResponse> listPermissions() { return permissions.findAllByOrderByNameAsc().stream().map(this::permissionResponse).toList(); }
	@Transactional(readOnly = true) public PermissionResponse getPermission(Long id) { return permissionResponse(permission(id)); }
	@Transactional public PermissionResponse updatePermission(Long id, PermissionRequest request) { Permission permission = permission(id); apply(permission, request); return permissionResponse(permission); }
	@Transactional public void deletePermission(Long id) { Permission permission = permission(id); rolePermissions.findAll().stream().filter(item -> item.getPermission().getId().equals(id)).forEach(rolePermissions::delete); permissions.delete(permission); }

	private void apply(Role role, RoleRequest request, boolean creating) {
		String name = request.name().trim().toUpperCase(Locale.ROOT);
		if (creating && roles.findByName(name).isPresent()) throw conflict("Role name already exists");
		if (!creating && !role.getName().equals(name)) throw badRequest("Role name cannot be changed");
		role.setName(name); role.setDescription(blankToNull(request.description())); roles.save(role);
		rolePermissions.deleteByRoleId(role.getId());
		for (Long permissionId : request.permissionIds() == null ? Set.<Long>of() : request.permissionIds()) {
			Permission permission = permission(permissionId); RolePermission mapping = new RolePermission(); mapping.setRole(role); mapping.setPermission(permission); rolePermissions.save(mapping);
		}
	}
	private void apply(Permission permission, PermissionRequest request) {
		String name = request.name().trim().toUpperCase(Locale.ROOT);
		permissions.findByName(name).filter(existing -> !existing.getId().equals(permission.getId())).ifPresent(existing -> { throw conflict("Permission name already exists"); });
		permission.setName(name); permission.setDescription(blankToNull(request.description())); permissions.save(permission);
	}
	private Role role(Long id) { return roles.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found")); }
	private Permission permission(Long id) { return permissions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permission not found")); }
	private RoleResponse roleResponse(Role role) { return new RoleResponse(role.getId(), role.getName(), role.getDescription(), permissions.findByRoleId(role.getId()).stream().map(Permission::getName).sorted().toList()); }
	private PermissionResponse permissionResponse(Permission permission) { return new PermissionResponse(permission.getId(), permission.getName(), permission.getDescription()); }
	private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
	private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
	private ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
