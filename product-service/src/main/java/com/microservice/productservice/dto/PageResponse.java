package com.microservice.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse {
    private Optional<Integer> page;
    private Integer per_page;
    private long total;
    private Integer total_pages;

    private Object data;
}
