package com.microservice.productservice.dto;

import com.microservice.productservice.entity.Product;

import java.util.List;
import java.util.stream.Collectors;

public class ProductMapperDto {
    
    public static ProductResponseDto toProductResponseDto(Product product) {
        if (product == null) {
            return null;
        }
        
        return ProductResponseDto.builder()
                .id(product.getId())
                .productName(product.getProductName())
                .description(product.getDescription())
                .category(product.getCategory())
                .productPrice(product.getProductPrice())
                .imgUrl(product.getImgUrl())
                .quantity(product.getQuantity())
                .brandName(product.getBrandName())
                .designer(product.getDesigner())
                .reorderLevel(product.getReorderLevel())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .inStock(product.getQuantity() > 0)
                .lowStock(product.getQuantity() <= product.getReorderLevel())
                .build();
    }
    
    public static List<ProductResponseDto> toProductResponseDtoList(List<Product> products) {
        if (products == null) {
            return List.of();
        }
        
        return products.stream()
                .map(ProductMapperDto::toProductResponseDto)
                .collect(Collectors.toList());
    }
    
    public static Product toProduct(ProductRequestDto request) {
        if (request == null) {
            return null;
        }
        
        Product product = new Product();
        product.setProductName(request.getProductName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setProductPrice(request.getProductPrice());
        product.setImgUrl(request.getImgUrl());
        product.setQuantity(request.getQuantity());
        product.setBrandName(request.getBrandName());
        product.setDesigner(request.getDesigner());
        
        if (request.getReorderLevel() != null) {
            product.setReorderLevel(request.getReorderLevel());
        }
        
        return product;
    }
    
    public static void updateProductFromRequest(Product product, ProductRequestDto request) {
        if (product == null || request == null) {
            return;
        }
        
        product.setProductName(request.getProductName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setProductPrice(request.getProductPrice());
        product.setImgUrl(request.getImgUrl());
        product.setQuantity(request.getQuantity());
        product.setBrandName(request.getBrandName());
        product.setDesigner(request.getDesigner());
        
        if (request.getReorderLevel() != null) {
            product.setReorderLevel(request.getReorderLevel());
        }
    }
}