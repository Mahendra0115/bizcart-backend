package com.mahendra.bizcart_backend.admin.controller;

import com.mahendra.bizcart_backend.admin.dto.ApiResponse;
import com.mahendra.bizcart_backend.admin.dto.RoleRequest;
import com.mahendra.bizcart_backend.admin.dto.RoleResponse;
import com.mahendra.bizcart_backend.admin.service.RbacService;
import com.mahendra.bizcart_backend.authentication.security.AuthenticatedUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/roles")
public class RoleController {
	private final RbacService service;
	public RoleController(RbacService service) { this.service = service; }
	@PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('ROLES_WRITE')") public ApiResponse<RoleResponse> create(@AuthenticationPrincipal AuthenticatedUserDetails actor, @Valid @RequestBody RoleRequest request) { return new ApiResponse<>("Role created successfully", service.createRole(actor.getId(), request)); }
	@GetMapping @PreAuthorize("hasAuthority('ROLES_READ')") public ApiResponse<List<RoleResponse>> list() { return new ApiResponse<>("Roles fetched successfully", service.listRoles()); }
	@GetMapping("/{id}") @PreAuthorize("hasAuthority('ROLES_READ')") public ApiResponse<RoleResponse> get(@PathVariable Long id) { return new ApiResponse<>("Role fetched successfully", service.getRole(id)); }
	@PutMapping("/{id}") @PreAuthorize("hasAuthority('ROLES_WRITE')") public ApiResponse<RoleResponse> update(@AuthenticationPrincipal AuthenticatedUserDetails actor, @PathVariable Long id, @Valid @RequestBody RoleRequest request) { return new ApiResponse<>("Role updated successfully", service.updateRole(actor.getId(), id, request)); }
	@DeleteMapping("/{id}") @PreAuthorize("hasAuthority('ROLES_WRITE')") public ApiResponse<Void> delete(@AuthenticationPrincipal AuthenticatedUserDetails actor, @PathVariable Long id) { service.deleteRole(actor.getId(), id); return new ApiResponse<>("Role deleted successfully", null); }
}
