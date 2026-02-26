package com.microservice.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductInventoryChangeDto implements Serializable {
    private Long productId;
    private Integer previousQuantity;
    private Integer newQuantity;
    private Integer changeAmount;
}