package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Payment;
import com.hoz.hozitech.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByOrderId(UUID orderId);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByOrderIdAndStatus(UUID orderId, PaymentStatus status);
}
