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
public class OrderEvent extends BaseEvent {
    private Long invoiceId;
    private Long userId;
    private String status;
    private List<OrderItemDto> items;
}

