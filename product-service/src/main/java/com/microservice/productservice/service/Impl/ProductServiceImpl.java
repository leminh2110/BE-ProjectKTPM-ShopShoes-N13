package com.microservice.productservice.service.Impl;

import com.microservice.productservice.dto.ProductMapperDto;
import com.microservice.productservice.dto.ProductRequestDto;
import com.microservice.productservice.dto.ProductResponseDto;
import com.microservice.productservice.entity.Product;
import com.microservice.productservice.repository.ProductRepository;
import com.microservice.productservice.service.InventoryEventPublisher;
import com.microservice.productservice.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);
    private final ProductRepository productRepository;
    private final InventoryEventPublisher inventoryEventPublisher;

    @Autowired
    public ProductServiceImpl(ProductRepository productRepository, InventoryEventPublisher inventoryEventPublisher) {
        this.productRepository = productRepository;
        this.inventoryEventPublisher = inventoryEventPublisher;
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    @Override
    public List<ProductResponseDto> getAllProductResponsesDto() {
        List<Product> products = productRepository.findAll();
        return ProductMapperDto.toProductResponseDtoList(products);
    }

    @Override
    public Page<ProductResponseDto> getAllProductResponsesDtoPaged(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(ProductMapperDto::toProductResponseDto);
    }

    @Override
    @Transactional
    public Product addNewProduct(Product product) {
        Product existProduct = productRepository.findByProductName(product.getProductName().trim());
        if(existProduct != null){
            return null;
        }

        log.info("Adding new product: {}", product.getProductName());
        return productRepository.save(product);
    }
    
    @Override
    @Transactional
    public ProductResponseDto addNewProductDto(ProductRequestDto productRequest) {
        try {
            log.info("[ADD] Bắt đầu thêm sản phẩm mới: {}", productRequest.getProductName());
            
            // Validate dữ liệu đầu vào
            if (productRequest.getProductName() == null || productRequest.getProductName().trim().isEmpty()) {
                log.error("[ADD] Tên sản phẩm không được để trống");
                throw new IllegalArgumentException("Tên sản phẩm không được để trống");
            }
            
            if (productRequest.getProductPrice() == null || productRequest.getProductPrice().compareTo(BigDecimal.ZERO) <= 0) {
                log.error("[ADD] Giá sản phẩm phải lớn hơn 0");
                throw new IllegalArgumentException("Giá sản phẩm phải lớn hơn 0");
            }
            
            if (productRequest.getQuantity() < 0) {
                log.error("[ADD] Số lượng sản phẩm không được âm");
                throw new IllegalArgumentException("Số lượng sản phẩm không được âm");
            }
            
            if (productRequest.getReorderLevel() < 0) {
                log.error("[ADD] Mức đặt hàng lại không được âm");
                throw new IllegalArgumentException("Mức đặt hàng lại không được âm");
            }
            
            // Kiểm tra sản phẩm đã tồn tại
            if (existsByProductName(productRequest.getProductName().trim())) {
                log.warn("[ADD] Sản phẩm đã tồn tại: {}", productRequest.getProductName());
                return null;
            }
            
            // Chuyển đổi và lưu sản phẩm
            Product product = ProductMapperDto.toProduct(productRequest);
            product = productRepository.save(product);
            
            log.info("[ADD] Thêm sản phẩm thành công. ID: {}, Tên: {}", 
                    product.getId(), product.getProductName());
            
            return ProductMapperDto.toProductResponseDto(product);
            
        } catch (Exception e) {
            log.error("[ADD] Lỗi khi thêm sản phẩm '{}': {}", 
                    productRequest.getProductName(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Optional<Product> getProduct(Long id) {
        return productRepository.findById(id);
    }
    
    @Override
    public ProductResponseDto getProductResponseDtoById(Long id) {
        Product product = getProductById(id);
        return ProductMapperDto.toProductResponseDto(product);
    }

    @Override
    public Product getProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + productId));
    }

    @Override
    public boolean existsByProductName(String productName) {
        return productRepository.existsByProductName(productName);
    }
    
    @Override
    @Transactional
    public Product updateProductQuantity(Long id, int quantity) {
        log.info("Updating product quantity: productId={}, quantity={}", id, quantity);
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));
        
        int oldQuantity = product.getQuantity();
        product.setQuantity(quantity);
        Product saved = productRepository.save(product);
        
        // Check if we need to notify about low stock
        if (saved.isLowStock() && oldQuantity > saved.getReorderLevel()) {
            log.warn("Product {} is now below reorder level: {}/{}", 
                    id, saved.getQuantity(), saved.getReorderLevel());
            
            try {
                inventoryEventPublisher.publishLowStockEvent(
                        saved.getId(), saved.getQuantity(), saved.getReorderLevel());
            } catch (Exception e) {
                log.error("Failed to publish low stock event: {}", e.getMessage(), e);
            }
        }
        
        return saved;
    }
    
    @Override
    @Transactional
    public Product updateProduct(Product product) {
        log.info("Updating product: productId={}", product.getId());
        
        if (product.getId() == null) {
            throw new IllegalArgumentException("Product ID cannot be null for update operation");
        }
        
        // Verify product exists
        if (!productRepository.existsById(product.getId())) {
            throw new EntityNotFoundException("Product not found with id: " + product.getId());
        }
        
        return productRepository.save(product);
    }
    
    @Override
    @Transactional
    public ProductResponseDto updateProductDto(Long id, ProductRequestDto productRequest) {
        log.info("Updating product: productId={}", id);
        
        Product existingProduct = getProductById(id);
        
        // Check if product name is being changed and if the new name already exists
        if (!existingProduct.getProductName().equals(productRequest.getProductName()) && 
                existsByProductName(productRequest.getProductName())) {
            log.warn("Cannot update product: name {} is already taken", productRequest.getProductName());
            return null;
        }
        
        // Update product with request data
        ProductMapperDto.updateProductFromRequest(existingProduct, productRequest);
        Product updatedProduct = productRepository.save(existingProduct);
        
        return ProductMapperDto.toProductResponseDto(updatedProduct);
    }
    
    @Override
    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new EntityNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
        log.info("Deleted product with ID: {}", id);
    }
    
    @Override
    public boolean hasInStock(Long productId, int quantity) {
        Product product = getProductById(productId);
        boolean hasStock = product.getQuantity() >= quantity;
        
        if (!hasStock) {
            log.warn("Insufficient stock for product {}: requested={}, available={}", 
                    productId, quantity, product.getQuantity());
        }
        
        return hasStock;
    }
    
    @Override
    public List<ProductResponseDto> getProductsNeedingReorderDto() {
        List<Product> lowStockProducts = productRepository.findByQuantityLessThanEqualAndQuantityGreaterThan(
                5, 0); // Default reorder level and greater than zero
        
        return ProductMapperDto.toProductResponseDtoList(lowStockProducts);
    }
    
    @Override
    @Transactional
    public void updateReorderLevel(Long id, int reorderLevel) {
        if (reorderLevel < 0) {
            throw new IllegalArgumentException("Reorder level cannot be negative");
        }
        
        Product product = getProductById(id);
        product.setReorderLevel(reorderLevel);
        productRepository.save(product);
        
        log.info("Updated reorder level for product {}: {}", id, reorderLevel);
        
        // Check if product is now below reorder level
        if (product.isLowStock()) {
            try {
                inventoryEventPublisher.publishLowStockEvent(
                        product.getId(), product.getQuantity(), product.getReorderLevel());
            } catch (Exception e) {
                log.error("Failed to publish low stock event: {}", e.getMessage(), e);
            }
        }
    }
    
    @Override
    @Transactional
    public boolean processInventoryChanges(Map<Long, Integer> productQuantityChanges) {
        List<Long> successfulUpdates = new ArrayList<>();
        List<Long> failedUpdates = new ArrayList<>();
        
        for (Map.Entry<Long, Integer> entry : productQuantityChanges.entrySet()) {
            Long productId = entry.getKey();
            Integer newQuantity = entry.getValue();
            
            try {
                Product product = getProductById(productId);
                
                // Skip invalid quantities
                if (newQuantity < 0) {
                    log.warn("Skipping invalid quantity ({}) for product {}", newQuantity, productId);
                    failedUpdates.add(productId);
                    continue;
                }
                
                int oldQuantity = product.getQuantity();
                product.setQuantity(newQuantity);
                productRepository.save(product);
                
                // Check if we need to notify about low stock
                if (product.isLowStock() && oldQuantity > product.getReorderLevel()) {
                    try {
                        inventoryEventPublisher.publishLowStockEvent(
                                product.getId(), product.getQuantity(), product.getReorderLevel());
                    } catch (Exception e) {
                        log.error("Failed to publish low stock event: {}", e.getMessage(), e);
                    }
                }
                
                successfulUpdates.add(productId);
                log.info("Updated inventory for product {}: {} -> {}", 
                        productId, oldQuantity, newQuantity);
                
            } catch (Exception e) {
                log.error("Failed to update inventory for product {}: {}", 
                        productId, e.getMessage());
                failedUpdates.add(productId);
            }
        }
        
        log.info("Batch inventory update: {}/{} successful", 
                successfulUpdates.size(), productQuantityChanges.size());
        
        return failedUpdates.isEmpty();
    }
    
    @Override
    public Page<ProductResponseDto> searchByKeywordDto(String keyword, Pageable pageable) {
        return productRepository.searchByKeyword(keyword, pageable)
                .map(ProductMapperDto::toProductResponseDto);
    }
    
    @Override
    public Page<ProductResponseDto> searchByCategoryDto(String category, Pageable pageable) {
        return productRepository.findByCategory(category, pageable)
                .map(ProductMapperDto::toProductResponseDto);
    }
    
    @Override
    public Page<ProductResponseDto> searchByBrandDto(String brand, Pageable pageable) {
        return productRepository.findByBrandName(brand, pageable)
                .map(ProductMapperDto::toProductResponseDto);
    }
    
    @Override
    public Page<ProductResponseDto> searchByDesignerDto(String designer, Pageable pageable) {
        return productRepository.findByDesigner(designer, pageable)
                .map(ProductMapperDto::toProductResponseDto);
    }
    
    @Override
    public Page<ProductResponseDto> searchByPriceRangeDto(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return productRepository.findByProductPriceBetween(minPrice, maxPrice, pageable)
                .map(ProductMapperDto::toProductResponseDto);
    }
    
    @Override
    public Page<ProductResponseDto> searchByCategoryAndBrandDto(String category, String brand, Pageable pageable) {
        return productRepository.findByCategoryAndBrandName(category, brand, pageable)
                .map(ProductMapperDto::toProductResponseDto);
    }

    @Override
    @Transactional
    public List<ProductResponseDto> importProductsFromExcel(MultipartFile file) throws IOException {
        log.info("Starting import of products from Excel file: {}", file.getOriginalFilename());
        
        List<ProductResponseDto> importedProducts = new ArrayList<>();
        List<String> skippedProducts = new ArrayList<>();
        
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // Skip header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                try {
                    Product product = new Product();
                    
                    // Read data from Excel cells
                    product.setProductName(getCellValueAsString(row.getCell(0)));
                    product.setDescription(getCellValueAsString(row.getCell(1)));
                    product.setCategory(getCellValueAsString(row.getCell(2)));
                    product.setProductPrice(new BigDecimal(getCellValueAsString(row.getCell(3))));
                    product.setImgUrl(getCellValueAsString(row.getCell(4)));
                    product.setQuantity(Integer.parseInt(getCellValueAsString(row.getCell(5))));
                    product.setBrandName(getCellValueAsString(row.getCell(6)));
                    product.setDesigner(getCellValueAsString(row.getCell(7)));
                    
                    // Check if product name already exists
                    if (existsByProductName(product.getProductName())) {
                        log.warn("Skipping duplicate product: {}", product.getProductName());
                        skippedProducts.add(product.getProductName());
                        continue;
                    }
                    
                    // Save product
                    Product savedProduct = productRepository.save(product);
                    if (savedProduct != null) {
                        importedProducts.add(ProductMapperDto.toProductResponseDto(savedProduct));
                        log.info("Successfully imported product: {}", savedProduct.getProductName());
                    }
                    
                } catch (Exception e) {
                    log.error("Error processing row {}: {}", i, e.getMessage());
                }
            }
        }
        
        log.info("Completed import. Successfully imported {} products", importedProducts.size());
        if (!skippedProducts.isEmpty()) {
            log.warn("Skipped {} duplicate products: {}", skippedProducts.size(), String.join(", ", skippedProducts));
        }
        
        return importedProducts;
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }
}

