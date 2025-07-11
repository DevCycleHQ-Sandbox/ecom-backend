package com.shopper.controller;

import com.shopper.dto.CreateOrderDto;
import com.shopper.entity.Order;
import com.shopper.entity.User;
import com.shopper.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping
    @Operation(summary = "Create a new order")
    public ResponseEntity<Order> createOrder(@Valid @RequestBody CreateOrderDto createOrderDto) {
        UUID userId = getCurrentUserId();
        
        try {
            Order order = orderService.createOrder(userId, createOrderDto);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            log.error("Failed to create order: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping
    @Operation(summary = "Get user's orders")
    public ResponseEntity<List<Order>> getUserOrders() {
        UUID userId = getCurrentUserId();
        List<Order> orders = orderService.getUserOrders(userId);
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<Order> getOrderById(@Parameter(description = "Order ID") @PathVariable UUID id) {
        UUID userId = getCurrentUserId();
        Optional<Order> order = orderService.getOrderById(id, userId);
        return order.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update order status (Admin only)")
    public ResponseEntity<Order> updateOrderStatus(
            @Parameter(description = "Order ID") @PathVariable UUID id,
            @RequestBody Map<String, String> statusUpdate) {
        UUID adminUserId = getCurrentUserId();
        String status = statusUpdate.get("status");
        
        if (status == null || status.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            Order order = orderService.updateOrderStatus(id, status, adminUserId);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            log.error("Failed to update order status: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all orders (Admin only)")
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/admin/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get orders by status (Admin only)")
    public ResponseEntity<List<Order>> getOrdersByStatus(@Parameter(description = "Order status") @PathVariable String status) {
        try {
            Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            List<Order> orders = orderService.getOrdersByStatus(orderStatus);
            return ResponseEntity.ok(orders);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/stats")
    @Operation(summary = "Get order statistics")
    public ResponseEntity<Map<String, Object>> getOrderStats() {
        UUID userId = getCurrentUserId();
        
        return ResponseEntity.ok(Map.of(
            "totalOrders", orderService.getUserOrderCount(userId),
            "totalRevenue", orderService.getUserTotalRevenue(userId)
        ));
    }
    
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getId();
        }
        throw new RuntimeException("User not authenticated");
    }
}