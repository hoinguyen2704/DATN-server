package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Role;
import com.hoz.hozitech.domain.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, RoleType> {
}
