package com.mahendra.bizcart_backend.address.repository;

import com.mahendra.bizcart_backend.address.entity.Address;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AddressRepository extends JpaRepository<Address, Long> {

	List<Address> findAllByUserIdAndDeletedFalseOrderByDefaultAddressDescCreatedAtDesc(Long userId);

	Optional<Address> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);

	long countByUserIdAndDeletedFalse(Long userId);

	Optional<Address> findFirstByUserIdAndDeletedFalseAndIdNotOrderByIdAsc(Long userId, Long excludedId);

	@Modifying(flushAutomatically = true)
	@Query("""
			update Address address
			set address.defaultAddress = false
			where address.user.id = :userId
			  and address.deleted = false
			  and address.defaultAddress = true
			""")
	int clearDefaultAddresses(@Param("userId") Long userId);
}
