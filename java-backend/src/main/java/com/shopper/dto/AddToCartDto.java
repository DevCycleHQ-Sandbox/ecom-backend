package com.shopper.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartDto {
    
    @NotNull(message = "Product ID is required")
    @JsonAlias("product_id")
    private UUID productId;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}