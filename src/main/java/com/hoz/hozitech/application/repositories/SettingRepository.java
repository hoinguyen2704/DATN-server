package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettingRepository extends JpaRepository<Setting, UUID> {

    Optional<Setting> findBySettingKey(String settingKey);

    List<Setting> findByGroupName(String groupName);

    List<Setting> findBySettingKeyIn(List<String> keys);

    boolean existsBySettingKey(String settingKey);
}
