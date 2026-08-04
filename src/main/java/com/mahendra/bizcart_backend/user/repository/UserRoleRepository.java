package com.mahendra.bizcart_backend.user.repository;

import com.mahendra.bizcart_backend.user.entity.UserRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

	void deleteByUserId(Long userId);

	List<UserRole> findByUserId(Long userId);
}
