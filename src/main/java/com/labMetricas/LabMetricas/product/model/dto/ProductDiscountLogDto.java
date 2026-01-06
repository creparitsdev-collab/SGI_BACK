package com.labMetricas.LabMetricas.product.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDiscountLogDto {
    private Integer id;
    private Integer productId;
    private String productNombre;
    private String productLote;
    private Integer amount;
    private String description;
    private Integer quantityBefore;
    private Integer quantityAfter;
    private UUID createdByUserId;
    private String createdByUserName;
    private String createdByUserEmail;
    private LocalDateTime createdAt;
}
