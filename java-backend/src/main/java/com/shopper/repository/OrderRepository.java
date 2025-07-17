package com.shopper.repository;

import com.shopper.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    
    List<Order> findByUserId(UUID userId);
    
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    Optional<Order> findByIdAndUserId(UUID id, UUID userId);
    
    List<Order> findByStatus(Order.OrderStatus status);
    
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.status = :status")
    List<Order> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") Order.OrderStatus status);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") Order.OrderStatus status);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId")
    long countByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'DELIVERED'")
    Double getTotalRevenue();
    
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.userId = :userId AND o.status = 'DELIVERED'")
    Double getTotalRevenueByUser(@Param("userId") UUID userId);
}