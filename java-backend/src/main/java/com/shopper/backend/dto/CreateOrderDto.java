package com.shopper.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateOrderDto {
    
    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;
    
    @NotBlank(message = "Card number is required")
    private String cardNumber;
    
    // Constructors
    public CreateOrderDto() {}
    
    public CreateOrderDto(String shippingAddress, String cardNumber) {
        this.shippingAddress = shippingAddress;
        this.cardNumber = cardNumber;
    }
    
    // Getters and Setters
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
}