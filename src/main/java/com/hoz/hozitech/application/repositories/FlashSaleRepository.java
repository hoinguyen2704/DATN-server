package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.FlashSale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface FlashSaleRepository extends JpaRepository<FlashSale, UUID> {

    @Query("""
            select fs.id as id,
                   fs.name as name,
                   fs.description as description,
                   fs.startTime as startTime,
                   fs.endTime as endTime,
                   fs.createdAt as createdAt
            from FlashSale fs
            where fs.status <> com.hoz.hozitech.domain.enums.FlashSaleStatus.HIDDEN
              and fs.startTime <= CURRENT_TIMESTAMP
              and fs.endTime >= CURRENT_TIMESTAMP
            order by fs.endTime asc
            """)
    List<ActiveStorefrontFlashSaleView> findActiveStorefrontFlashSales();

    @Query(
            value = """
                    select fs.id as id,
                           fs.name as name,
                           fs.description as description,
                           fs.startTime as startTime,
                           fs.endTime as endTime,
                           fs.createdAt as createdAt
                    from FlashSale fs
                    where fs.status <> com.hoz.hozitech.domain.enums.FlashSaleStatus.HIDDEN
                      and fs.startTime <= CURRENT_TIMESTAMP
                      and fs.endTime >= CURRENT_TIMESTAMP
                    order by fs.endTime asc
                    """,
            countQuery = """
                    select count(fs)
                    from FlashSale fs
                    where fs.status <> com.hoz.hozitech.domain.enums.FlashSaleStatus.HIDDEN
                      and fs.startTime <= CURRENT_TIMESTAMP
                      and fs.endTime >= CURRENT_TIMESTAMP
                    """)
    Page<ActiveStorefrontFlashSaleView> findActiveStorefrontFlashSales(Pageable pageable);

    Page<FlashSale> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            select fs.id, count(fsi.id)
            from FlashSale fs
            left join fs.items fsi
            where fs.id in :flashSaleIds
            group by fs.id
            """)
    List<Object[]> countItemsByFlashSaleIds(@Param("flashSaleIds") Collection<UUID> flashSaleIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE FlashSale fs SET fs.status = com.hoz.hozitech.domain.enums.FlashSaleStatus.SCHEDULED " +
            "WHERE fs.startTime > :now " +
            "AND fs.status <> com.hoz.hozitech.domain.enums.FlashSaleStatus.HIDDEN " +
            "AND fs.status <> com.hoz.hozitech.domain.enums.FlashSaleStatus.SCHEDULED")
    int markScheduledFlashSales(@Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE FlashSale fs SET fs.status = com.hoz.hozitech.domain.enums.FlashSaleStatus.ACTIVE " +
            "WHERE fs.startTime <= :now AND fs.endTime >= :now " +
            "AND fs.status <> com.hoz.hozitech.domain.enums.FlashSaleStatus.HIDDEN " +
            "AND fs.status <> com.hoz.hozitech.domain.enums.FlashSaleStatus.ACTIVE")
    int markActiveFlashSales(@Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE FlashSale fs SET fs.status = com.hoz.hozitech.domain.enums.FlashSaleStatus.ENDED " +
            "WHERE fs.endTime < :now " +
            "AND fs.status <> com.hoz.hozitech.domain.enums.FlashSaleStatus.HIDDEN " +
            "AND fs.status <> com.hoz.hozitech.domain.enums.FlashSaleStatus.ENDED")
    int markEndedFlashSales(@Param("now") LocalDateTime now);

    interface ActiveStorefrontFlashSaleView {
        UUID getId();

        String getName();

        String getDescription();

        LocalDateTime getStartTime();

        LocalDateTime getEndTime();

        LocalDateTime getCreatedAt();
    }
}
