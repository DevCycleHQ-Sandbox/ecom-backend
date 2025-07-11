package com.shopper.backend.repository;

import com.shopper.backend.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
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
    
    Optional<CartItem> findByIdAndUserId(UUID id, UUID userId);
    
    void deleteByUserId(UUID userId);
    
    void deleteByUserIdAndProductId(UUID userId, UUID productId);
    
    @Query("SELECT ci FROM CartItem ci JOIN FETCH ci.product WHERE ci.userId = :userId")
    List<CartItem> findByUserIdWithProduct(@Param("userId") UUID userId);
    
    @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.userId = :userId")
    Long countByUserId(@Param("userId") UUID userId);
}