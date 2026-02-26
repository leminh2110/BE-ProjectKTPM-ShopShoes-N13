package com.microservice.productservice.controller;

import com.microservice.productservice.dto.PageResponse;
import com.microservice.productservice.dto.ProductMapperDto;
import com.microservice.productservice.dto.ProductRequestDto;
import com.microservice.productservice.dto.ProductResponseDto;
import com.microservice.productservice.dto.ResponseObject;
import com.microservice.productservice.entity.Product;
import com.microservice.productservice.repository.ProductRepository;
import com.microservice.productservice.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private static final Logger log = LoggerFactory.getLogger(ProductController.class);
    private final ProductRepository productRepository;
    private final ProductService productService;

    @Autowired
    public ProductController(ProductRepository productRepository, ProductService productService) {
        this.productRepository = productRepository;
        this.productService = productService;
    }

    @GetMapping("/all")
    ResponseEntity<ResponseObject> GetProducts(){
        List<ProductResponseDto> products = productService.getAllProductResponsesDto();

        if(products != null){
            return ResponseEntity.status(HttpStatus.OK).body(
                    new ResponseObject("ok","get all products successfully", products)
            );
        }
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(
                new ResponseObject("ok", "cannot find product", "")
        );
    }

    @GetMapping("/all-products")
    ResponseEntity<PageResponse> getAllProductPage(@RequestParam Optional<Integer> page){
        Page<ProductResponseDto> pageProducts = productService.getAllProductResponsesDtoPaged(
                PageRequest.of(page.orElse(0), 6));
        
        return ResponseEntity.status(HttpStatus.OK).body(
                new PageResponse(page, pageProducts.getSize(), pageProducts.getTotalElements(),
                        pageProducts.getTotalPages(),
                        pageProducts.getContent()
                )
        );
    }

    @GetMapping("/shop-products")
    ResponseEntity<PageResponse> getShopProductPage(@RequestParam Optional<Integer> page){
        Page<ProductResponseDto> pageProducts = productService.getAllProductResponsesDtoPaged(
                PageRequest.of(page.orElse(0), 10));
        
        return ResponseEntity.status(HttpStatus.OK).body(
                new PageResponse(page, pageProducts.getSize(), pageProducts.getTotalElements(),
                        pageProducts.getTotalPages(),
                        pageProducts.getContent()
                )
        );
    }

    @GetMapping("/sort-products")
    ResponseEntity<PageResponse> getShopProductPageSort(
            @RequestParam Optional<Integer> page,
            @RequestParam Optional<String> sortBy,
            @RequestParam Integer asc
    ){
        Sort.Direction direction = asc == 1 ? Sort.Direction.ASC : Sort.Direction.DESC;
        Page<ProductResponseDto> pageProducts = productService.getAllProductResponsesDtoPaged(
                PageRequest.of(page.orElse(0), 10, direction, sortBy.orElse("productPrice"))
        );
        
        return ResponseEntity.status(HttpStatus.OK).body(
                new PageResponse(page, pageProducts.getSize(), pageProducts.getTotalElements(),
                        pageProducts.getTotalPages(),
                        pageProducts.getContent()
                )
        );
    }

    // Add a GET endpoint for add product page
    @GetMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ResponseObject> getAddProductPage() {
        return ResponseEntity.status(HttpStatus.OK).body(
                new ResponseObject("ok", "Add product page", "")
        );
    }

    // Add product with ProductRequestDto
    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ResponseObject> AddProduct(@Valid @RequestBody ProductRequestDto productRequest, BindingResult bindingResult){
        if (bindingResult.hasErrors()) {
            // Create a custom error message based on validation errors
            StringBuilder errorMessages = new StringBuilder("Validation errors: ");
            bindingResult.getAllErrors().forEach(error -> {
                errorMessages.append(error.getDefaultMessage()).append(" ");
            });

            // Return a simplified error response
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ResponseObject("failed", errorMessages.toString().trim(), "")
            );
        }

        ProductResponseDto product = productService.addNewProductDto(productRequest);
        
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(
                    new ResponseObject("failed", "product name has already been taken", "")
            );
        }
        
        return ResponseEntity.status(HttpStatus.OK).body(
                new ResponseObject("ok", "product added successfully", product)
        );
    }

    // Get 1 product
    @GetMapping("/{id:[\\d]+}")
    ResponseEntity<ResponseObject> GetDetailProduct(@PathVariable Long id)  {
        log.debug("Getting product details for ID: {}", id);
        try {
            ProductResponseDto product = productService.getProductResponseDtoById(id);
            return ResponseEntity.status(HttpStatus.OK).body(
                    new ResponseObject("ok", "get success", product)
            );
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ResponseObject("failed", "can not find product with "+id, "")
            );
        }
    }

    // Internal endpoint for direct access by other services (cart, invoice)
    @GetMapping("/internal/{id}")
    ResponseEntity<Product> getProductInternal(@PathVariable Long id) {
        log.debug("Internal request for product ID: {}", id);
        try {
            Product product = productService.getProductById(id);
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            log.error("Error retrieving product for internal use: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // Update product with ProductRequestDto
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ResponseObject> UpdateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequestDto productRequest){
        log.debug("Updating product ID: {}", id);
        try {
            ProductResponseDto updatedProduct = productService.updateProductDto(id, productRequest);
            
            if (updatedProduct == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        new ResponseObject("failed", "Product name already exists", null)
                );
            }
            
            return ResponseEntity.status(HttpStatus.OK).body(
                    new ResponseObject("ok", "updated product success", updatedProduct)
            );
        } catch (Exception e) {
            log.error("Error updating product: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ResponseObject("error", "Failed to update product: " + e.getMessage(), null)
            );
        }
    }

    // Update product inventory only (used by cart service during checkout)
    @PatchMapping("/{id}/inventory")
    ResponseEntity<ResponseObject> updateInventory(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        log.debug("Updating inventory for product ID: {} with quantity: {}", id, request.get("quantity"));
        
        try {
            Integer newQuantity = request.get("quantity");
            if (newQuantity == null) {
                return ResponseEntity.badRequest().body(
                        new ResponseObject("error", "Quantity is required", null)
                );
            }
            
            Product updatedProduct = productService.updateProductQuantity(id, newQuantity);
            ProductResponseDto response = ProductMapperDto.toProductResponseDto(updatedProduct);
            
            return ResponseEntity.ok(
                    new ResponseObject("ok", "Inventory updated successfully", response)
            );
        } catch (Exception e) {
            log.error("Error updating inventory: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ResponseObject("error", "Failed to update inventory: " + e.getMessage(), null)
            );
        }
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ResponseObject> DeleteProduct(@PathVariable Long id){
        try {
            productService.deleteProduct(id);
            return ResponseEntity.status(HttpStatus.OK).body(
                    new ResponseObject("ok", "deleted product successfully", "")
            );
        } catch (Exception e) {
            log.error("Error deleting product: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ResponseObject("error", "Failed to delete product: " + e.getMessage(), null)
            );
        }
    }
    
    // New endpoints for enhanced search features
    
    @GetMapping("/search")
    ResponseEntity<PageResponse> searchProducts(
            @RequestParam String keyword,
            @RequestParam Optional<Integer> page) {
        log.debug("Searching for products with keyword: {}", keyword);
        
        Page<ProductResponseDto> results = productService.searchByKeywordDto(
                keyword, PageRequest.of(page.orElse(0), 10));
        
        return ResponseEntity.ok(
                new PageResponse(page, results.getSize(), results.getTotalElements(),
                        results.getTotalPages(), results.getContent())
        );
    }
    
    @GetMapping("/category/{category}")
    ResponseEntity<PageResponse> getProductsByCategory(
            @PathVariable String category,
            @RequestParam Optional<Integer> page) {
        log.debug("Getting products by category: {}", category);
        
        Page<ProductResponseDto> results = productService.searchByCategoryDto(
                category, PageRequest.of(page.orElse(0), 10));
        
        return ResponseEntity.ok(
                new PageResponse(page, results.getSize(), results.getTotalElements(),
                        results.getTotalPages(), results.getContent())
        );
    }
    
    @GetMapping("/brand/{brand}")
    ResponseEntity<PageResponse> getProductsByBrand(
            @PathVariable String brand,
            @RequestParam Optional<Integer> page) {
        log.debug("Getting products by brand: {}", brand);
        
        Page<ProductResponseDto> results = productService.searchByBrandDto(
                brand, PageRequest.of(page.orElse(0), 10));
        
        return ResponseEntity.ok(
                new PageResponse(page, results.getSize(), results.getTotalElements(),
                        results.getTotalPages(), results.getContent())
        );
    }
    
    @GetMapping("/designer/{designer}")
    ResponseEntity<PageResponse> getProductsByDesigner(
            @PathVariable String designer,
            @RequestParam Optional<Integer> page) {
        log.debug("Getting products by designer: {}", designer);
        
        Page<ProductResponseDto> results = productService.searchByDesignerDto(
                designer, PageRequest.of(page.orElse(0), 10));
        
        return ResponseEntity.ok(
                new PageResponse(page, results.getSize(), results.getTotalElements(),
                        results.getTotalPages(), results.getContent())
        );
    }
    
    @GetMapping("/price-range")
    ResponseEntity<PageResponse> getProductsByPriceRange(
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max,
            @RequestParam Optional<Integer> page) {
        log.debug("Getting products by price range: {} - {}", min, max);
        
        Page<ProductResponseDto> results = productService.searchByPriceRangeDto(
                min, max, PageRequest.of(page.orElse(0), 10));
        
        return ResponseEntity.ok(
                new PageResponse(page, results.getSize(), results.getTotalElements(),
                        results.getTotalPages(), results.getContent())
        );
    }
    
    // New endpoints for inventory management
    
    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ResponseObject> getProductsNeedingReorder() {
        log.debug("Getting products needing reorder");
        
        List<ProductResponseDto> lowStockProducts = productService.getProductsNeedingReorderDto();
        
        return ResponseEntity.ok(
                new ResponseObject("ok", "Low stock products retrieved successfully", lowStockProducts)
        );
    }
    
    @PatchMapping("/{id}/reorder-level")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ResponseObject> updateReorderLevel(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> request) {
        
        Integer reorderLevel = request.get("reorderLevel");
        if (reorderLevel == null || reorderLevel < 0) {
            return ResponseEntity.badRequest().body(
                    new ResponseObject("error", "Valid reorder level is required", null)
            );
        }
        
        log.debug("Updating reorder level for product ID: {} to {}", id, reorderLevel);
        
        try {
            productService.updateReorderLevel(id, reorderLevel);
            ProductResponseDto product = productService.getProductResponseDtoById(id);
            
            return ResponseEntity.ok(
                    new ResponseObject("ok", "Reorder level updated successfully", product)
            );
        } catch (Exception e) {
            log.error("Error updating reorder level: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ResponseObject("error", "Failed to update reorder level: " + e.getMessage(), null)
            );
        }
    }
    
    @PostMapping("/batch-inventory-update")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ResponseObject> batchInventoryUpdate(@RequestBody Map<Long, Integer> productQuantityChanges) {
        log.debug("Batch updating inventory for {} products", productQuantityChanges.size());
        
        try {
            boolean success = productService.processInventoryChanges(productQuantityChanges);
            
            if (success) {
                return ResponseEntity.ok(
                        new ResponseObject("ok", "Inventory updated successfully for all products", null)
                );
            } else {
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(
                        new ResponseObject("partial", "Some inventory updates failed", null)
                );
            }
        } catch (Exception e) {
            log.error("Error during batch inventory update: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ResponseObject("error", "Failed to update inventory: " + e.getMessage(), null)
            );
        }
    }

    @PostMapping("/import-excel")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ResponseObject> importProductsFromExcel(@RequestParam("file") MultipartFile file) {
        log.debug("Importing products from Excel file: {}", file.getOriginalFilename());
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ResponseObject("error", "Please select a file to upload", null)
                );
            }

            if (!file.getOriginalFilename().endsWith(".xlsx")) {
                return ResponseEntity.badRequest().body(
                    new ResponseObject("error", "Only Excel (.xlsx) files are allowed", null)
                );
            }

            List<ProductResponseDto> importedProducts = productService.importProductsFromExcel(file);
            
            return ResponseEntity.ok(
                new ResponseObject("ok", "Successfully imported " + importedProducts.size() + " products", importedProducts)
            );
        } catch (Exception e) {
            log.error("Error importing products from Excel: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ResponseObject("error", "Failed to import products: " + e.getMessage(), null)
            );
        }
    }

}
