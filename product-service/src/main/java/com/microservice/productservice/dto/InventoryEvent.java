package com.microservice.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryEvent extends BaseEvent {
    private Long orderId;
    private Long invoiceId;
    private List<ProductInventoryChangeDto> productChanges;
}
