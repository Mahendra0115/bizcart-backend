package com.mahendra.bizcart_backend.user.repository;

import com.mahendra.bizcart_backend.user.entity.RolePermission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
	void deleteByRoleId(Long roleId);
	List<RolePermission> findByRoleId(Long roleId);
}
