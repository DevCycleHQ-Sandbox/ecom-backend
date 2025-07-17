package com.shopper.repository.primary;

import com.shopper.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrimaryProductRepository extends JpaRepository<Product, UUID> {
    
    List<Product> findByCategory(String category);
    
    List<Product> findByNameContainingIgnoreCase(String name);
    
    List<Product> findByDescriptionContainingIgnoreCase(String description);
    
    @Query("SELECT p FROM Product p WHERE p.stockQuantity > 0")
    List<Product> findInStockProducts();
    
    @Query("SELECT p FROM Product p WHERE p.category = :category AND p.stockQuantity > 0")
    List<Product> findInStockProductsByCategory(@Param("category") String category);
    
    @Query("SELECT DISTINCT p.category FROM Product p ORDER BY p.category")
    List<String> findAllCategories();
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.stockQuantity > 0")
    long countInStockProducts();
} 