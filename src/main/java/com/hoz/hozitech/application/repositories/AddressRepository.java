package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findByUserIdOrderByIsDefaultDesc(UUID userId);

    List<Address> findByUserIdAndIsDefaultTrue(UUID userId);

    long countByUserId(UUID userId);
}
