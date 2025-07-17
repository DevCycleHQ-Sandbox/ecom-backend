package com.shopper.repository.secondary;

import com.shopper.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
} 