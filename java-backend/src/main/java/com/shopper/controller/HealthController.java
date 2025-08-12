package com.shopper.controller;

import com.shopper.entity.CartItem;
import com.shopper.repository.primary.PrimaryCartItemRepository;
import com.shopper.repository.secondary.SecondaryCartItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    // Add this for debugging the sync issue
    @Autowired
    private PrimaryCartItemRepository primaryCartItemRepository;
    
    @Autowired(required = false)
    private SecondaryCartItemRepository secondaryCartItemRepository;

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
    
    @GetMapping("/debug/cart-items")
    public Map<String, Object> debugCartItems() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Test primary repository
            List<CartItem> primaryItems = primaryCartItemRepository.findAll();
            result.put("primary_count", primaryItems.size());
            result.put("primary_items", primaryItems);
            log.info("Primary repository found {} cart items", primaryItems.size());
            
            // Test secondary repository
            if (secondaryCartItemRepository != null) {
                List<CartItem> secondaryItems = secondaryCartItemRepository.findAll();
                result.put("secondary_count", secondaryItems.size());
                result.put("secondary_items", secondaryItems);
                log.info("Secondary repository found {} cart items", secondaryItems.size());
            } else {
                result.put("secondary_count", "Repository not available");
                log.info("Secondary repository not available");
            }
            
            result.put("success", true);
        } catch (Exception e) {
            log.error("Error debugging cart items: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
} 