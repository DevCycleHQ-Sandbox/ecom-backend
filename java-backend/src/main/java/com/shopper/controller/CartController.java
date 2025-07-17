package com.shopper.controller;

import com.shopper.dto.AddToCartDto;
import com.shopper.dto.UpdateCartItemDto;
import com.shopper.entity.CartItem;
import com.shopper.entity.User;
import com.shopper.service.CartService;
import com.shopper.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cart", description = "Shopping cart endpoints")
public class CartController {
    
    private final CartService cartService;
    private final UserService userService;
    
    @GetMapping
    @Operation(summary = "Get cart items")
    public ResponseEntity<List<CartItem>> getCart() {
        UUID userId = getCurrentUserId();
        List<CartItem> cartItems = cartService.getCartItems(userId);
        return ResponseEntity.ok(cartItems);
    }
    
    @PostMapping
    @Operation(summary = "Add item to cart")
    public ResponseEntity<CartItem> addToCart(@Valid @RequestBody AddToCartDto addToCartDto) {
        UUID userId = getCurrentUserId();
        
        try {
            CartItem cartItem = cartService.addToCart(userId, addToCartDto);
            return ResponseEntity.ok(cartItem);
        } catch (RuntimeException e) {
            log.error("Failed to add item to cart: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<CartItem> updateCartItem(
            @Parameter(description = "Cart item ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateCartItemDto updateCartItemDto) {
        UUID userId = getCurrentUserId();
        
        try {
            CartItem cartItem = cartService.updateCartItem(userId, id, updateCartItemDto);
            return ResponseEntity.ok(cartItem);
        } catch (RuntimeException e) {
            log.error("Failed to update cart item: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<Void> removeFromCart(@Parameter(description = "Cart item ID") @PathVariable UUID id) {
        UUID userId = getCurrentUserId();
        
        try {
            cartService.removeCartItem(userId, id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Failed to remove cart item: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping
    @Operation(summary = "Clear cart")
    public ResponseEntity<Void> clearCart() {
        UUID userId = getCurrentUserId();
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/count")
    @Operation(summary = "Get cart item count")
    public ResponseEntity<Long> getCartItemCount() {
        UUID userId = getCurrentUserId();
        long count = cartService.getCartItemCount(userId);
        return ResponseEntity.ok(count);
    }
    
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            
            // Handle both UserDetails and User entity cases
            if (principal instanceof UserDetails) {
                String username = ((UserDetails) principal).getUsername();
                User user = userService.findByUsername(username);
                return user.getId();
            } else if (principal instanceof User) {
                return ((User) principal).getId();
            }
        }
        throw new RuntimeException("User not authenticated");
    }
}