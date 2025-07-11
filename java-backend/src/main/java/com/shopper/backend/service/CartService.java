package com.shopper.backend.service;

import com.shopper.backend.dto.AddToCartDto;
import com.shopper.backend.dto.UpdateCartItemDto;
import com.shopper.backend.entity.CartItem;
import com.shopper.backend.entity.Product;
import com.shopper.backend.entity.User;
import com.shopper.backend.repository.CartItemRepository;
import com.shopper.backend.repository.ProductRepository;
import com.shopper.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class CartService {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeatureFlagService featureFlagService;

    public List<CartItem> getCartItems(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check feature flag for cart improvements
        boolean cartImprovements = featureFlagService.getBooleanFeatureFlag(
                username, "cart-improvements", false);

        List<CartItem> cartItems = cartItemRepository.findByUserIdWithProduct(user.getId());

        if (cartImprovements) {
            System.out.println("✅ Cart improvements enabled for user: " + username);
        }

        return cartItems;
    }

    public CartItem addToCart(String username, AddToCartDto addToCartDto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepository.findById(addToCartDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Check if product is in stock
        if (product.getStockQuantity() < addToCartDto.getQuantity()) {
            throw new RuntimeException("Insufficient stock");
        }

        // Check if item already exists in cart
        Optional<CartItem> existingCartItem = cartItemRepository.findByUserIdAndProductId(
                user.getId(), addToCartDto.getProductId());

        if (existingCartItem.isPresent()) {
            // Update existing item
            CartItem cartItem = existingCartItem.get();
            int newQuantity = cartItem.getQuantity() + addToCartDto.getQuantity();
            
            if (product.getStockQuantity() < newQuantity) {
                throw new RuntimeException("Insufficient stock for requested quantity");
            }
            
            cartItem.setQuantity(newQuantity);
            return cartItemRepository.save(cartItem);
        } else {
            // Create new cart item
            CartItem cartItem = new CartItem();
            cartItem.setUserId(user.getId());
            cartItem.setProductId(addToCartDto.getProductId());
            cartItem.setQuantity(addToCartDto.getQuantity());
            return cartItemRepository.save(cartItem);
        }
    }

    public CartItem updateCartItem(String username, UUID cartItemId, UpdateCartItemDto updateCartItemDto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CartItem cartItem = cartItemRepository.findByIdAndUserId(cartItemId, user.getId())
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        Product product = productRepository.findById(cartItem.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Check if product is in stock
        if (product.getStockQuantity() < updateCartItemDto.getQuantity()) {
            throw new RuntimeException("Insufficient stock");
        }

        cartItem.setQuantity(updateCartItemDto.getQuantity());
        return cartItemRepository.save(cartItem);
    }

    public void removeCartItem(String username, UUID cartItemId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CartItem cartItem = cartItemRepository.findByIdAndUserId(cartItemId, user.getId())
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        cartItemRepository.delete(cartItem);
    }

    public void clearCart(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        cartItemRepository.deleteByUserId(user.getId());
    }

    public Long getCartItemCount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return cartItemRepository.countByUserId(user.getId());
    }
}