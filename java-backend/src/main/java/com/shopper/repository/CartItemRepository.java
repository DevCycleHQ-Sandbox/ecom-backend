package com.shopper.repository;

import com.shopper.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    
    List<CartItem> findByUserId(UUID userId);
    
    Optional<CartItem> findByUserIdAndProductId(UUID userId, UUID productId);
    
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
    
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.userId = :userId AND ci.productId = :productId")
    void deleteByUserIdAndProductId(@Param("userId") UUID userId, @Param("productId") UUID productId);
    
    @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.userId = :userId")
    long countByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT SUM(ci.quantity) FROM CartItem ci WHERE ci.userId = :userId")
    Integer sumQuantityByUserId(@Param("userId") UUID userId);
}