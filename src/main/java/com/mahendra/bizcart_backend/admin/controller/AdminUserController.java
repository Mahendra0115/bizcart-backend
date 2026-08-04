package com.mahendra.bizcart_backend.admin.controller;

import com.mahendra.bizcart_backend.admin.dto.AdminUserRequest;
import com.mahendra.bizcart_backend.admin.dto.AdminUserResponse;
import com.mahendra.bizcart_backend.admin.dto.ApiResponse;
import com.mahendra.bizcart_backend.admin.dto.UpdateAdminUserRequest;
import com.mahendra.bizcart_backend.admin.service.AdminUserService;
import com.mahendra.bizcart_backend.authentication.security.AuthenticatedUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {
	private final AdminUserService service;
	public AdminUserController(AdminUserService service) { this.service = service; }
	@PostMapping @ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<AdminUserResponse> create(@Valid @RequestBody AdminUserRequest request) { return new ApiResponse<>("Admin user created successfully", service.create(request)); }
	@GetMapping public ApiResponse<List<AdminUserResponse>> list() { return new ApiResponse<>("Admin users fetched successfully", service.list()); }
	@GetMapping("/{id}") public ApiResponse<AdminUserResponse> get(@PathVariable Long id) { return new ApiResponse<>("Admin user fetched successfully", service.get(id)); }
	@PutMapping("/{id}") public ApiResponse<AdminUserResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateAdminUserRequest request) { return new ApiResponse<>("Admin user updated successfully", service.update(id, request)); }
	@DeleteMapping("/{id}") public ApiResponse<Void> deactivate(@AuthenticationPrincipal AuthenticatedUserDetails actor, @PathVariable Long id) { service.deactivate(actor.getId(), id); return new ApiResponse<>("Admin user deactivated successfully", null); }
}
