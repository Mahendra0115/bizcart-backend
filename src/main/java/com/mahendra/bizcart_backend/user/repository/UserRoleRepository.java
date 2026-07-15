package com.mahendra.bizcart_backend.user.repository;

import com.mahendra.bizcart_backend.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
}
