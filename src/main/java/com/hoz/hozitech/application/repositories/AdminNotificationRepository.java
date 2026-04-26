package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.AdminNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdminNotificationRepository extends JpaRepository<AdminNotification, UUID> {

    Page<AdminNotification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            select n
            from AdminNotification n
            where not exists (
                select 1
                from AdminNotificationRead r
                where r.notification = n
                  and r.user.id = :userId
            )
            order by n.createdAt desc
            """)
    List<AdminNotification> findUnreadByUserId(@Param("userId") UUID userId);
}
