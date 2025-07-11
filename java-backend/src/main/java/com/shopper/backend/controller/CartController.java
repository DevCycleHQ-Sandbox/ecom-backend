package com.shopper.backend.controller;

import com.shopper.backend.dto.AddToCartDto;
import com.shopper.backend.dto.UpdateCartItemDto;
import com.shopper.backend.entity.CartItem;
import com.shopper.backend.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/cart")
@Tag(name = "Cart", description = "Shopping cart management APIs")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping
    @Operation(summary = "Get user's cart items")
    public ResponseEntity<List<CartItem>> getCartItems() {
        String username = getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(401).body(null);
        }

        try {
            List<CartItem> cartItems = cartService.getCartItems(username);
            return ResponseEntity.ok(cartItems);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping
    @Operation(summary = "Add item to cart")
    public ResponseEntity<CartItem> addToCart(@Valid @RequestBody AddToCartDto addToCartDto) {
        String username = getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(401).body(null);
        }

        try {
            CartItem cartItem = cartService.addToCart(username, addToCartDto);
            return ResponseEntity.ok(cartItem);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update cart item")
    public ResponseEntity<CartItem> updateCartItem(@PathVariable UUID id, 
                                                  @Valid @RequestBody UpdateCartItemDto updateCartItemDto) {
        String username = getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(401).body(null);
        }

        try {
            CartItem cartItem = cartService.updateCartItem(username, id, updateCartItemDto);
            return ResponseEntity.ok(cartItem);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<Map<String, String>> removeFromCart(@PathVariable UUID id) {
        String username = getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(401).body(null);
        }

        try {
            cartService.removeCartItem(username, id);
            return ResponseEntity.ok(Map.of("message", "Item removed from cart"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping
    @Operation(summary = "Clear cart")
    public ResponseEntity<Map<String, String>> clearCart() {
        String username = getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(401).body(null);
        }

        try {
            cartService.clearCart(username);
            return ResponseEntity.ok(Map.of("message", "Cart cleared"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/count")
    @Operation(summary = "Get cart item count")
    public ResponseEntity<Map<String, Long>> getCartItemCount() {
        String username = getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(401).body(null);
        }

        try {
            Long count = cartService.getCartItemCount(username);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }
}