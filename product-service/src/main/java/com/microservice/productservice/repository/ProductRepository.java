package com.microservice.productservice.repository;

import com.microservice.productservice.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Product findByProductName(String name);
    boolean existsByProductName(String productName);
    
    // Category-based searches
    List<Product> findByCategory(String category);
    Page<Product> findByCategory(String category, Pageable pageable);
    
    // Brand-based searches
    List<Product> findByBrandName(String brandName);
    Page<Product> findByBrandName(String brandName, Pageable pageable);
    
    // Designer-based searches
    List<Product> findByDesigner(String designer);
    Page<Product> findByDesigner(String designer, Pageable pageable);
    
    // Price range search
    List<Product> findByProductPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    Page<Product> findByProductPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    
    // Inventory search
    List<Product> findByQuantityLessThanEqual(int threshold);
    List<Product> findByQuantityGreaterThan(int threshold);
    
    // Combined searches
    Page<Product> findByCategoryAndBrandName(String category, String brandName, Pageable pageable);
    
    // Full-text search using LIKE
    @Query("SELECT p FROM Product p WHERE " +
           "LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.category) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.brandName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.designer) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    // Find products that need reordering (stock <= reorder level)
    List<Product> findByQuantityLessThanEqualAndQuantityGreaterThan(int reorderLevel, int zero);
}