package com.labMetricas.LabMetricas.product.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "product_discounts",
        indexes = {
                @Index(name = "idx_product_discounts_product_id", columnList = "product_id"),
                @Index(name = "idx_product_discounts_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDiscountLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(name = "product_nombre", columnDefinition = "VARCHAR(200)")
    private String productNombre;

    @Column(name = "product_lote", columnDefinition = "VARCHAR(100)")
    private String productLote;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "quantity_before", nullable = false)
    private Integer quantityBefore;

    @Column(name = "quantity_after", nullable = false)
    private Integer quantityAfter;

    @Column(name = "created_by_user_id", columnDefinition = "UUID")
    private UUID createdByUserId;

    @Column(name = "created_by_user_name", columnDefinition = "VARCHAR(50)")
    private String createdByUserName;

    @Column(name = "created_by_user_email", columnDefinition = "VARCHAR(50)")
    private String createdByUserEmail;

    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}
