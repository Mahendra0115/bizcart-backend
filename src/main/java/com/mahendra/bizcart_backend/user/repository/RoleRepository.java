package com.mahendra.bizcart_backend.user.repository;

import com.mahendra.bizcart_backend.user.entity.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleRepository extends JpaRepository<Role, Long> {

	String PARAM_NAME = "name";
	String PARAM_USER_ID = "userId";

	String FIND_BY_NAME = """
			select r
			from Role r
			where r.name = :name
			""";

	String FIND_BY_USER_ID = """
			select distinct r
			from Role r
			join UserRole ur on ur.role = r
			where ur.user.id = :userId
			""";

	@Query(FIND_BY_NAME)
	Optional<Role> findByName(@Param(PARAM_NAME) String name);

	@Query(FIND_BY_USER_ID)
	List<Role> findByUserId(@Param(PARAM_USER_ID) Long userId);

	List<Role> findAllByOrderByNameAsc();
}
