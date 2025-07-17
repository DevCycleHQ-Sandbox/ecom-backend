package com.shopper.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductDto {
    
    private String name;
    
    private String description;
    
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;
    
    private String imageUrl;
    
    private String category;
    
    @Min(value = 0, message = "Stock quantity must be non-negative")
    private Integer stockQuantity;
}