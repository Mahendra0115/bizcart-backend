package com.mahendra.bizcart_backend.user.repository;

import com.mahendra.bizcart_backend.user.entity.Permission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

	String PARAM_NAME = "name";
	String PARAM_ROLE_ID = "roleId";
	String PARAM_USER_ID = "userId";

	String FIND_BY_NAME = """
			select p
			from Permission p
			where p.name = :name
			""";

	String FIND_BY_ROLE_ID = """
			select distinct p
			from Permission p
			join RolePermission rp on rp.permission = p
			where rp.role.id = :roleId
			""";

	String FIND_BY_USER_ID = """
			select distinct p
			from Permission p
			join RolePermission rp on rp.permission = p
			join UserRole ur on ur.role = rp.role
			where ur.user.id = :userId
			""";

	@Query(FIND_BY_NAME)
	Optional<Permission> findByName(@Param(PARAM_NAME) String name);

	@Query(FIND_BY_ROLE_ID)
	List<Permission> findByRoleId(@Param(PARAM_ROLE_ID) Long roleId);

	@Query(FIND_BY_USER_ID)
	List<Permission> findByUserId(@Param(PARAM_USER_ID) Long userId);

	List<Permission> findAllByOrderByNameAsc();
}
