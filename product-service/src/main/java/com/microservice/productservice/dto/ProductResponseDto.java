package com.microservice.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {
    private Long id;
    private String productName;
    private String description;
    private String category;
    private BigDecimal productPrice;
    private String imgUrl;
    private int quantity;
    private String brandName;
    private String designer;
    private int reorderLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean inStock;
    private boolean lowStock;
    
    // Helper methods
    public boolean isInStock() {
        return quantity > 0;
    }
    
    public boolean isLowStock() {
        return quantity <= reorderLevel;
    }
}