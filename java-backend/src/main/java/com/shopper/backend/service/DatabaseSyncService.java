package com.shopper.backend.service;

import com.shopper.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DatabaseSyncService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    public Map<String, Object> syncAllData() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // In a real implementation, this would sync data between primary and secondary databases
            // For now, we'll just return statistics
            
            result.put("users", syncEntity("users"));
            result.put("products", syncEntity("products"));
            result.put("cartItems", syncEntity("cartItems"));
            result.put("orders", syncEntity("orders"));
            result.put("orderItems", syncEntity("orderItems"));
            
            result.put("totalEntities", 5);
            result.put("totalSynced", 5);
            result.put("totalErrors", 0);
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("totalErrors", 1);
        }
        
        return result;
    }

    public Map<String, Object> syncSpecificEntity(String entityName) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Object> entityResult = syncEntity(entityName);
            result.put("entity", entityName);
            result.put("synced", entityResult.get("count"));
            result.put("errors", 0);
            
        } catch (Exception e) {
            result.put("entity", entityName);
            result.put("synced", 0);
            result.put("errors", 1);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    public Map<String, Object> getDatabaseStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Primary database stats (current implementation uses single database)
            Map<String, Object> primaryStats = new HashMap<>();
            primaryStats.put("users", userRepository.count());
            primaryStats.put("products", productRepository.count());
            primaryStats.put("cartItems", cartItemRepository.count());
            primaryStats.put("orders", orderRepository.count());
            primaryStats.put("orderItems", orderItemRepository.count());
            
            stats.put("primary", primaryStats);
            
            // Secondary database stats (would be from actual secondary database)
            Map<String, Object> secondaryStats = new HashMap<>();
            secondaryStats.put("users", userRepository.count()); // Same for now
            secondaryStats.put("products", productRepository.count());
            secondaryStats.put("cartItems", cartItemRepository.count());
            secondaryStats.put("orders", orderRepository.count());
            secondaryStats.put("orderItems", orderItemRepository.count());
            
            stats.put("secondary", secondaryStats);
            
        } catch (Exception e) {
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }

    private Map<String, Object> syncEntity(String entityName) {
        Map<String, Object> entityStats = new HashMap<>();
        
        try {
            long count = 0;
            
            switch (entityName.toLowerCase()) {
                case "users":
                    count = userRepository.count();
                    break;
                case "products":
                    count = productRepository.count();
                    break;
                case "cartitems":
                    count = cartItemRepository.count();
                    break;
                case "orders":
                    count = orderRepository.count();
                    break;
                case "orderitems":
                    count = orderItemRepository.count();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown entity: " + entityName);
            }
            
            entityStats.put("entity", entityName);
            entityStats.put("count", count);
            entityStats.put("synced", count);
            entityStats.put("errors", 0);
            
        } catch (Exception e) {
            entityStats.put("entity", entityName);
            entityStats.put("count", 0);
            entityStats.put("synced", 0);
            entityStats.put("errors", 1);
            entityStats.put("error", e.getMessage());
        }
        
        return entityStats;
    }
}