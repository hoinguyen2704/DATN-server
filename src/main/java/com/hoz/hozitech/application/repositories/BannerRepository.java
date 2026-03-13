package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Banner;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BannerRepository extends JpaRepository<Banner, UUID> {
    List<Banner> findByIsActiveTrue(Sort sort);
}
