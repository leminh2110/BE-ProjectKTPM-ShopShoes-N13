package com.microservice.productservice.service;

import com.microservice.productservice.dto.BaseEvent;
import com.microservice.productservice.dto.InventoryEvent;
import com.microservice.productservice.dto.ProductInventoryChangeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${product.exchange.name}")
    private String productExchange;

    @Value("${product.inventory.routing-key}")
    private String inventoryRoutingKey;

    /**
     * Publishes an event when inventory changes occur
     * 
     * @param invoiceId The ID of the invoice associated with the inventory change (can be null)
     * @param orderId The ID of the order associated with the inventory change (can be null)
     * @param changes The list of inventory changes to publish
     * @return true if the event was published successfully, false otherwise
     */
    @Retryable(value = {AmqpException.class}, maxAttempts = 3, 
              backoff = @Backoff(delay = 1000, multiplier = 2))
    public boolean publishInventoryChangedEvent(Long invoiceId, Long orderId, 
                                             List<ProductInventoryChangeDto> changes) {
        if (changes == null || changes.isEmpty()) {
            log.warn("Attempted to publish empty inventory changes - skipping");
            return false;
        }

        try {
            InventoryEvent event = InventoryEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("INVENTORY_CHANGED")
                    .eventTime(LocalDateTime.now())
                    .invoiceId(invoiceId)
                    .orderId(orderId)
                    .productChanges(changes)
                    .build();

            log.info("Publishing inventory event: type={}, id={}, changesCount={}", 
                    event.getEventType(), event.getEventId(), changes.size());

            rabbitTemplate.convertAndSend(productExchange, inventoryRoutingKey, event);
            return true;
        } catch (AmqpException ex) {
            log.error("Failed to publish inventory event: {}", ex.getMessage(), ex);
            throw ex; // Retryable annotation will handle retry
        } catch (Exception ex) {
            log.error("Unexpected error publishing inventory event: {}", ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Publishes a low stock alert event
     * 
     * @param productId The ID of the product with low stock
     * @param currentQuantity The current quantity of the product
     * @param reorderLevel The reorder level of the product
     * @return true if the event was published successfully, false otherwise
     */
    @Retryable(value = {AmqpException.class}, maxAttempts = 2, 
              backoff = @Backoff(delay = 500, multiplier = 2))
    public boolean publishLowStockEvent(Long productId, int currentQuantity, int reorderLevel) {
        if (productId == null) {
            log.warn("Attempted to publish low stock event with null productId - skipping");
            return false;
        }

        try {
            ProductInventoryChangeDto change = ProductInventoryChangeDto.builder()
                    .productId(productId)
                    .newQuantity(currentQuantity)
                    .previousQuantity(currentQuantity) // Added to ensure we have a previous quantity value
                    .changeAmount(0) // No change amount for a notification event
                    .build();

            InventoryEvent event = InventoryEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("LOW_STOCK_ALERT")
                    .eventTime(LocalDateTime.now())
                    .productChanges(List.of(change))
                    .build();

            log.warn("Publishing low stock alert: productId={}, quantity={}, reorderLevel={}", 
                    productId, currentQuantity, reorderLevel);

            rabbitTemplate.convertAndSend(productExchange, inventoryRoutingKey, event);
            return true;
        } catch (AmqpException ex) {
            log.error("Failed to publish low stock alert: {}", ex.getMessage(), ex);
            throw ex; // Retryable annotation will handle retry
        } catch (Exception ex) {
            log.error("Unexpected error publishing low stock alert: {}", ex.getMessage(), ex);
            return false;
        }
    }
} 