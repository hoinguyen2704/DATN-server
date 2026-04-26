package com.hoz.hozitech.application.scheduler;

import com.hoz.hozitech.application.services.promotion.PromotionStatusSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionStatusScheduler {

    private final PromotionStatusSyncService promotionStatusSyncService;

    @Scheduled(fixedRate = 60 * 1000)
    public void syncPromotionStatuses() {
        int couponChanges = promotionStatusSyncService.syncCouponStatuses();
        int flashSaleChanges = promotionStatusSyncService.syncFlashSaleStatuses();

        if (couponChanges > 0 || flashSaleChanges > 0) {
            log.info("[PromotionStatusScheduler] synced promotions couponChanges={} flashSaleChanges={}",
                    couponChanges, flashSaleChanges);
        }
    }
}
