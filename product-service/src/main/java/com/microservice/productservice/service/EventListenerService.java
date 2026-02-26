package com.microservice.productservice.service;

import com.microservice.productservice.dto.OrderEvent;
import com.microservice.productservice.dto.ProductInventoryChangeDto;
import com.microservice.productservice.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventListenerService {
    
    private final ProductService productService;
    private final InventoryEventPublisher inventoryEventPublisher;
    
    /**
     * Handles order events received from the message queue.
     * Processes inventory updates for order creation and placement.
     * 
     * @param event The order event containing order details
     * @throws AmqpRejectAndDontRequeueException if the event is malformed and cannot be processed
     */
    @RabbitListener(queues = "${product.queue.order}")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(value = {OptimisticLockingFailureException.class}, 
               maxAttempts = 3, 
               backoff = @Backoff(delay = 1000, multiplier = 2))
    public void handleOrderEvent(OrderEvent event) {
        if (event == null || event.getEventType() == null) {
            log.error("Received null or invalid order event");
            throw new AmqpRejectAndDontRequeueException("Invalid event received");
        }
        
        log.info("Received order event: type={}, id={}", event.getEventType(), event.getEventId());
        
        try {
            switch (event.getEventType()) {
                case "ORDER_CREATED":
                case "ORDER_PLACED":
                    updateInventoryForOrder(event);
                    break;
                case "ORDER_CANCELLED":
                    restoreInventoryForCancelledOrder(event);
                    break;
                default:
                    log.warn("Unhandled order event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing order event: {}", e.getMessage(), e);
            // If this is a temporary failure that could succeed on retry, rethrow
            if (isRetryableException(e)) {
                throw e;
            }
            // Otherwise, acknowledge the message but log the error
        }
    }
    
    private boolean isRetryableException(Exception e) {
        return e instanceof OptimisticLockingFailureException;
    }
    
    /**
     * Updates product inventory based on an order event.
     */
    private void updateInventoryForOrder(OrderEvent event) {
        log.info("Updating inventory for order event: {}", event.getEventId());
        
        if (event.getItems() == null || event.getItems().isEmpty()) {
            log.warn("Order event contains no items, skipping inventory update");
            return;
        }
        
        // Collect all inventory updates
        Map<Long, Integer> inventoryUpdates = event.getItems().stream()
                .filter(item -> item.getProductId() != null && item.getQuantity() != null && item.getQuantity() > 0)
                .collect(Collectors.toMap(
                    item -> item.getProductId(),
                    item -> -item.getQuantity(),  // Negate quantity because we're reducing inventory
                    (existing, replacement) -> existing + replacement  // If same product appears multiple times, sum the quantities
                ));
        
        if (inventoryUpdates.isEmpty()) {
            log.warn("No valid items to process in order event: {}", event.getEventId());
            return;
        }
        
        // Process inventory changes atomically
        List<ProductInventoryChangeDto> inventoryChanges = new ArrayList<>();
        
        inventoryUpdates.forEach((productId, changeAmount) -> {
            try {
                // Get current product
                Product product = productService.getProductById(productId);
                if (product == null) {
                    log.warn("Product not found: {}", productId);
                    return;
                }
                
                // Record the current quantity
                int currentStock = product.getQuantity();
                
                // Validate the inventory change
                int newStock = Math.max(0, currentStock + changeAmount);
                
                // Update the product quantity
                product.setQuantity(newStock);
                productService.updateProduct(product);
                
                // Add to inventory changes for event publishing
                inventoryChanges.add(ProductInventoryChangeDto.builder()
                        .productId(productId)
                        .previousQuantity(currentStock)
                        .newQuantity(newStock)
                        .changeAmount(changeAmount)
                        .build());
                
                log.info("Updated inventory for product {}: {} -> {}", productId, currentStock, newStock);
                
                // Check if low stock
                if (newStock <= product.getReorderLevel()) {
                    log.warn("Product {} is running low on stock: {}", productId, newStock);
                    // You could trigger a separate low stock event here
                }
            } catch (Exception e) {
                log.error("Error updating inventory for product {}: {}", productId, e.getMessage(), e);
            }
        });
        
        // Publish inventory update event if there were any changes
        if (!inventoryChanges.isEmpty()) {
            inventoryEventPublisher.publishInventoryChangedEvent(
                    event.getInvoiceId(),
                    null, // orderId might not be available
                    inventoryChanges
            );
        }
    }
    
    /**
     * Restores product inventory when an order is cancelled.
     */
    private void restoreInventoryForCancelledOrder(OrderEvent event) {
        log.info("Restoring inventory for cancelled order: {}", event.getEventId());
        
        if (event.getItems() == null || event.getItems().isEmpty()) {
            log.warn("Cancelled order event contains no items, skipping inventory restore");
            return;
        }
        
        // For order cancellation, we restore inventory by adding back the quantities
        Map<Long, Integer> inventoryUpdates = event.getItems().stream()
                .filter(item -> item.getProductId() != null && item.getQuantity() != null && item.getQuantity() > 0)
                .collect(Collectors.toMap(
                    item -> item.getProductId(),
                    item -> item.getQuantity(),  // Positive quantity because we're restoring inventory
                    (existing, replacement) -> existing + replacement
                ));
        
        if (inventoryUpdates.isEmpty()) {
            log.warn("No valid items to restore in cancelled order: {}", event.getEventId());
            return;
        }
        
        // Process inventory changes and collect results
        List<ProductInventoryChangeDto> inventoryChanges = new ArrayList<>();
        
        inventoryUpdates.forEach((productId, changeAmount) -> {
            try {
                Product product = productService.getProductById(productId);
                if (product == null) {
                    log.warn("Product not found for restoration: {}", productId);
                    return;
                }
                
                int currentStock = product.getQuantity();
                int newStock = currentStock + changeAmount;
                
                product.setQuantity(newStock);
                productService.updateProduct(product);
                
                inventoryChanges.add(ProductInventoryChangeDto.builder()
                        .productId(productId)
                        .previousQuantity(currentStock)
                        .newQuantity(newStock)
                        .changeAmount(changeAmount)
                        .build());
                
                log.info("Restored inventory for product {}: {} -> {}", productId, currentStock, newStock);
            } catch (Exception e) {
                log.error("Error restoring inventory for product {}: {}", productId, e.getMessage(), e);
            }
        });
        
        // Publish inventory restoration event
        if (!inventoryChanges.isEmpty()) {
            inventoryEventPublisher.publishInventoryChangedEvent(
                    event.getInvoiceId(),
                    null,
                    inventoryChanges
            );
        }
    }
}