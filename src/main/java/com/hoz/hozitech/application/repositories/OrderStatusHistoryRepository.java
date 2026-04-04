package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.entities.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {
    List<OrderStatusHistory> findByOrderOrderByCreatedAtDesc(Order order);
}
