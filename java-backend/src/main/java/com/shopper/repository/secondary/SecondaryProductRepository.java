package com.shopper.repository.secondary;

import com.shopper.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SecondaryProductRepository extends JpaRepository<Product, UUID> {
    
    List<Product> findByCategory(String category);
    
    @Query("SELECT p FROM Product p WHERE p.name ILIKE %:name%")
    List<Product> findByNameContainingIgnoreCase(@Param("name") String name);
    
    @Query("SELECT p FROM Product p WHERE p.stockQuantity > 0")
    List<Product> findInStock();
    
    @Query("SELECT p FROM Product p WHERE p.category = :category AND p.stockQuantity > 0")
    List<Product> findByCategoryAndInStock(@Param("category") String category);
    
    @Modifying
    @Transactional
    @Query(value = "MERGE INTO products (id, name, description, price, image_url, category, stock_quantity, created_at, updated_at) " +
                   "VALUES (:id, :name, :description, :price, :imageUrl, :category, :stockQuantity, :createdAt, :updatedAt)", 
           nativeQuery = true)
    void saveWithSpecificId(@Param("id") String id,
                          @Param("name") String name,
                          @Param("description") String description,
                          @Param("price") BigDecimal price,
                          @Param("imageUrl") String imageUrl,
                          @Param("category") String category,
                          @Param("stockQuantity") Integer stockQuantity,
                          @Param("createdAt") LocalDateTime createdAt,
                          @Param("updatedAt") LocalDateTime updatedAt);
} 