package com.mahendra.bizcart_backend.admin.controller;

import com.mahendra.bizcart_backend.admin.dto.ApiResponse;
import com.mahendra.bizcart_backend.admin.dto.PermissionRequest;
import com.mahendra.bizcart_backend.admin.dto.PermissionResponse;
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
@RequestMapping("/api/v1/admin/permissions")
public class PermissionController {
	private final RbacService service;
	public PermissionController(RbacService service) { this.service = service; }
	@PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('PERMISSIONS_WRITE')") public ApiResponse<PermissionResponse> create(@AuthenticationPrincipal AuthenticatedUserDetails actor, @Valid @RequestBody PermissionRequest request) { return new ApiResponse<>("Permission created successfully", service.createPermission(actor.getId(), request)); }
	@GetMapping @PreAuthorize("hasAuthority('PERMISSIONS_READ')") public ApiResponse<List<PermissionResponse>> list() { return new ApiResponse<>("Permissions fetched successfully", service.listPermissions()); }
	@GetMapping("/{id}") @PreAuthorize("hasAuthority('PERMISSIONS_READ')") public ApiResponse<PermissionResponse> get(@PathVariable Long id) { return new ApiResponse<>("Permission fetched successfully", service.getPermission(id)); }
	@PutMapping("/{id}") @PreAuthorize("hasAuthority('PERMISSIONS_WRITE')") public ApiResponse<PermissionResponse> update(@AuthenticationPrincipal AuthenticatedUserDetails actor, @PathVariable Long id, @Valid @RequestBody PermissionRequest request) { return new ApiResponse<>("Permission updated successfully", service.updatePermission(actor.getId(), id, request)); }
	@DeleteMapping("/{id}") @PreAuthorize("hasAuthority('PERMISSIONS_WRITE')") public ApiResponse<Void> delete(@AuthenticationPrincipal AuthenticatedUserDetails actor, @PathVariable Long id) { service.deletePermission(actor.getId(), id); return new ApiResponse<>("Permission deleted successfully", null); }
}
