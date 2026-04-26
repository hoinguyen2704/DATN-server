package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.AdminNotificationRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

@Repository
public interface AdminNotificationReadRepository extends JpaRepository<AdminNotificationRead, UUID> {

    boolean existsByUserIdAndNotificationId(UUID userId, UUID notificationId);

    long countByUserId(UUID userId);

    @Query("""
            select r.notification.id
            from AdminNotificationRead r
            where r.user.id = :userId
              and r.notification.id in :notificationIds
            """)
    Set<UUID> findReadNotificationIds(@Param("userId") UUID userId,
                                      @Param("notificationIds") Collection<UUID> notificationIds);
}
