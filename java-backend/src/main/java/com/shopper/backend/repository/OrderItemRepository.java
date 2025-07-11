package com.shopper.backend.repository;

import com.shopper.backend.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    
    List<OrderItem> findByOrderId(UUID orderId);
    
    List<OrderItem> findByProductId(UUID productId);
    
    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.product WHERE oi.orderId = :orderId")
    List<OrderItem> findByOrderIdWithProduct(@Param("orderId") UUID orderId);
    
    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.orderId = :orderId")
    Long countByOrderId(@Param("orderId") UUID orderId);
}