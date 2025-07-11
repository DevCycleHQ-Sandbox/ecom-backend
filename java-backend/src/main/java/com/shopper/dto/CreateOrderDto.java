package com.shopper.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderDto {
    
    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;
    
    @NotBlank(message = "Card number is required")
    private String cardNumber;
}