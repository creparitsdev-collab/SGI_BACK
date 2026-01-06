package com.labMetricas.LabMetricas.product.repository;

import com.labMetricas.LabMetricas.product.model.ProductDiscountLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductDiscountLogRepository extends JpaRepository<ProductDiscountLog, Integer> {
    List<ProductDiscountLog> findByProductIdOrderByCreatedAtDesc(Integer productId);
}
