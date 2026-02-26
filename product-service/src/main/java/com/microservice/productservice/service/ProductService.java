package com.microservice.productservice.service;

import com.microservice.productservice.dto.ProductRequestDto;
import com.microservice.productservice.dto.ProductResponseDto;
import com.microservice.productservice.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ProductService {
    // Basic entity operations for internal use
    List<Product> getAllProducts();
    Product addNewProduct(Product product);
    Optional<Product> getProduct(Long id);
    Product getProductById(Long id);
    boolean existsByProductName(String productName);
    Product updateProduct(Product product);
    void deleteProduct(Long id);
    
    // DTO-based operations for API endpoints
    ProductResponseDto getProductResponseDtoById(Long id);
    List<ProductResponseDto> getAllProductResponsesDto();
    Page<ProductResponseDto> getAllProductResponsesDtoPaged(Pageable pageable);
    ProductResponseDto addNewProductDto(ProductRequestDto productRequest);
    ProductResponseDto updateProductDto(Long id, ProductRequestDto productRequest);
    
    // Inventory management
    Product updateProductQuantity(Long id, int quantity);
    boolean hasInStock(Long productId, int quantity);
    List<ProductResponseDto> getProductsNeedingReorderDto();
    void updateReorderLevel(Long id, int reorderLevel);
    boolean processInventoryChanges(Map<Long, Integer> productQuantityChanges);
    
    // Search operations
    Page<ProductResponseDto> searchByKeywordDto(String keyword, Pageable pageable);
    Page<ProductResponseDto> searchByCategoryDto(String category, Pageable pageable);
    Page<ProductResponseDto> searchByBrandDto(String brand, Pageable pageable);
    Page<ProductResponseDto> searchByDesignerDto(String designer, Pageable pageable);
    Page<ProductResponseDto> searchByPriceRangeDto(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    Page<ProductResponseDto> searchByCategoryAndBrandDto(String category, String brand, Pageable pageable);
    
    // Import products from Excel
    List<ProductResponseDto> importProductsFromExcel(MultipartFile file) throws IOException;
}
