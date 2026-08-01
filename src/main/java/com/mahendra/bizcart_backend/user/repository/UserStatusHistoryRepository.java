package com.mahendra.bizcart_backend.user.repository;

import com.mahendra.bizcart_backend.user.entity.UserStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserStatusHistoryRepository extends JpaRepository<UserStatusHistory, Long> {
}
