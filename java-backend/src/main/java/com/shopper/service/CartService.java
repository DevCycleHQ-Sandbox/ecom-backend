package com.shopper.service;

import com.shopper.dto.AddToCartDto;
import com.shopper.dto.UpdateCartItemDto;
import com.shopper.entity.CartItem;
import com.shopper.entity.Product;
import com.shopper.repository.CartItemRepository;
import com.shopper.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    
    public List<CartItem> getCartItems(UUID userId) {
        return cartItemRepository.findByUserIdWithProduct(userId);
    }
    
    @Transactional
    public CartItem addToCart(UUID userId, AddToCartDto addToCartDto) {
        // Check if product exists
        Product product = productRepository.findById(addToCartDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // Check if product is in stock
        if (product.getStockQuantity() < addToCartDto.getQuantity()) {
            throw new RuntimeException("Insufficient stock");
        }
        
        // Check if item already exists in cart
        Optional<CartItem> existingItem = cartItemRepository.findByUserIdAndProductId(userId, addToCartDto.getProductId());
        
        if (existingItem.isPresent()) {
            // Update existing item
            CartItem cartItem = existingItem.get();
            int newQuantity = cartItem.getQuantity() + addToCartDto.getQuantity();
            
            if (product.getStockQuantity() < newQuantity) {
                throw new RuntimeException("Insufficient stock");
            }
            
            cartItem.setQuantity(newQuantity);
            return cartItemRepository.save(cartItem);
        } else {
            // Create new cart item
            CartItem cartItem = CartItem.builder()
                    .userId(userId)
                    .productId(addToCartDto.getProductId())
                    .quantity(addToCartDto.getQuantity())
                    .build();
            
            return cartItemRepository.save(cartItem);
        }
    }
    
    @Transactional
    public CartItem updateCartItem(UUID userId, UUID cartItemId, UpdateCartItemDto updateCartItemDto) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));
        
        // Check if cart item belongs to user
        if (!cartItem.getUserId().equals(userId)) {
            throw new RuntimeException("Cart item does not belong to user");
        }
        
        // Check if product has enough stock
        Product product = productRepository.findById(cartItem.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        if (product.getStockQuantity() < updateCartItemDto.getQuantity()) {
            throw new RuntimeException("Insufficient stock");
        }
        
        cartItem.setQuantity(updateCartItemDto.getQuantity());
        return cartItemRepository.save(cartItem);
    }
    
    @Transactional
    public void removeCartItem(UUID userId, UUID cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));
        
        // Check if cart item belongs to user
        if (!cartItem.getUserId().equals(userId)) {
            throw new RuntimeException("Cart item does not belong to user");
        }
        
        cartItemRepository.delete(cartItem);
    }
    
    @Transactional
    public void clearCart(UUID userId) {
        cartItemRepository.deleteByUserId(userId);
    }
    
    public long getCartItemCount(UUID userId) {
        return cartItemRepository.countByUserId(userId);
    }
    
    public Integer getTotalCartQuantity(UUID userId) {
        Integer total = cartItemRepository.sumQuantityByUserId(userId);
        return total != null ? total : 0;
    }
}