package com.shopper.service;

import com.shopper.dto.CreateOrderDto;
import com.shopper.entity.CartItem;
import com.shopper.entity.Order;
import com.shopper.entity.OrderItem;
import com.shopper.entity.Product;
import com.shopper.repository.CartItemRepository;
import com.shopper.repository.OrderItemRepository;
import com.shopper.repository.OrderRepository;
import com.shopper.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    
    @Transactional
    public Order createOrder(UUID userId, CreateOrderDto createOrderDto) {
        // Get cart items
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }
        
        // Calculate total amount
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            
            // Check stock availability
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }
            
            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
        }
        
        // Create order
        Order order = Order.builder()
                .userId(userId)
                .totalAmount(totalAmount)
                .status(Order.OrderStatus.PENDING)
                .shippingAddress(createOrderDto.getShippingAddress())
                .cardNumber(createOrderDto.getCardNumber())
                .build();
        
        Order savedOrder = orderRepository.save(order);
        
        // Create order items and update product stock
        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            
            // Create order item
            OrderItem orderItem = OrderItem.builder()
                    .orderId(savedOrder.getId())
                    .productId(cartItem.getProductId())
                    .quantity(cartItem.getQuantity())
                    .price(product.getPrice())
                    .build();
            
            orderItemRepository.save(orderItem);
            
            // Update product stock
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }
        
        // Clear cart
        cartItemRepository.deleteByUserId(userId);
        
        return savedOrder;
    }
    
    public List<Order> getUserOrders(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    public Optional<Order> getOrderById(UUID orderId, UUID userId) {
        return orderRepository.findByIdAndUserId(orderId, userId);
    }
    
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
    
    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatus(status);
    }
    
    @Transactional
    public Order updateOrderStatus(UUID orderId, String status, UUID adminUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        try {
            Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            order.setStatus(orderStatus);
            
            log.info("Order {} status updated to {} by admin {}", orderId, status, adminUserId);
            
            return orderRepository.save(order);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid order status: " + status);
        }
    }
    
    public long getOrderCount() {
        return orderRepository.count();
    }
    
    public long getOrderCountByStatus(Order.OrderStatus status) {
        return orderRepository.countByStatus(status);
    }
    
    public long getUserOrderCount(UUID userId) {
        return orderRepository.countByUserId(userId);
    }
    
    public Double getTotalRevenue() {
        Double revenue = orderRepository.getTotalRevenue();
        return revenue != null ? revenue : 0.0;
    }
    
    public Double getUserTotalRevenue(UUID userId) {
        Double revenue = orderRepository.getTotalRevenueByUser(userId);
        return revenue != null ? revenue : 0.0;
    }
}