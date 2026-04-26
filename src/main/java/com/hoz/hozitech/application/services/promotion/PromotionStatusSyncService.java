package com.hoz.hozitech.application.services.promotion;

import com.hoz.hozitech.application.repositories.CouponRepository;
import com.hoz.hozitech.application.repositories.FlashSaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class PromotionStatusSyncService {

    private final CouponRepository couponRepository;
    private final FlashSaleRepository flashSaleRepository;

    @Value("${app.timezone}")
    private String appTimezone;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int syncCouponStatuses() {
        LocalDateTime now = now();
        return couponRepository.markExpiredCoupons(now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int syncFlashSaleStatuses() {
        LocalDateTime now = now();
        int scheduledCount = flashSaleRepository.markScheduledFlashSales(now);
        int activeCount = flashSaleRepository.markActiveFlashSales(now);
        int endedCount = flashSaleRepository.markEndedFlashSales(now);
        return scheduledCount + activeCount + endedCount;
    }

    public LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of(appTimezone));
    }
}
