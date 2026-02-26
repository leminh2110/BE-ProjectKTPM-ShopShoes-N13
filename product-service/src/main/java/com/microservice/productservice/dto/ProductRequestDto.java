package com.microservice.productservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequestDto {
    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 100, message = "Tên sản phẩm không được vượt quá 100 ký tự")
    private String productName;

    @NotBlank(message = "Mô tả sản phẩm không được để trống")
    private String description;

    @NotBlank(message = "Danh mục sản phẩm không được để trống")
    private String category;

    @NotNull(message = "Giá sản phẩm không được để trống")
    @Min(value = 0, message = "Giá sản phẩm phải lớn hơn hoặc bằng 0")
    private BigDecimal productPrice;

    @NotBlank(message = "URL hình ảnh không được để trống")
    private String imgUrl;

    @NotNull(message = "Số lượng sản phẩm không được để trống")
    @Min(value = 1, message = "Số lượng sản phẩm phải ít nhất là 1")
    private Integer quantity;

    @NotBlank(message = "Thương hiệu sản phẩm không được để trống")
    private String brandName;

    @NotBlank(message = "Tên nhà thiết kế không được để trống")
    private String designer;
    
    @Min(value = 0, message = "Reorder level phải lớn hơn hoặc bằng 0")
    private Integer reorderLevel = 5; // Default reorder level
}