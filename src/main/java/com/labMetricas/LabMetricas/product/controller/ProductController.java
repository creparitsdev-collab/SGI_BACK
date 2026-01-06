package com.labMetricas.LabMetricas.product.controller;

import com.labMetricas.LabMetricas.product.model.dto.CreateProductDto;
import com.labMetricas.LabMetricas.product.model.dto.CreateProductDiscountDto;
import com.labMetricas.LabMetricas.product.model.dto.UpdateProductDto;
import com.labMetricas.LabMetricas.product.service.ProductService;
import com.labMetricas.LabMetricas.util.ResponseObject;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProductController {
    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ResponseObject> createProduct(@Valid @RequestBody CreateProductDto createProductDto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Log the attempt
        logger.info("User {} attempting to create a new product with lote: {}", 
            auth.getName(), createProductDto.getLote());
        
        return productService.createProduct(createProductDto);
    }

    @PutMapping
    public ResponseEntity<ResponseObject> updateProduct(@Valid @RequestBody UpdateProductDto updateProductDto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Log the attempt
        logger.info("User {} attempting to update product with id {}", 
            auth.getName(), updateProductDto.getId());
        
        return productService.updateProduct(updateProductDto);
    }

    @PostMapping("/{id}/discounts")
    public ResponseEntity<ResponseObject> createProductDiscount(
            @PathVariable Integer id,
            @Valid @RequestBody CreateProductDiscountDto dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        logger.info("User {} attempting to create discount for product id {}", auth.getName(), id);

        return productService.createProductDiscount(id, dto);
    }

    @GetMapping("/{id}/discounts")
    public ResponseEntity<ResponseObject> getProductDiscounts(@PathVariable Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        logger.info("User {} attempting to retrieve discounts for product id {}", auth.getName(), id);

        return productService.getProductDiscounts(id);
    }

    @GetMapping("/qr/{hash}")
    public ResponseEntity<ResponseObject> getProductByQrHash(@PathVariable String hash) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Log the attempt
        logger.info("User {} attempting to retrieve product by QR hash", auth.getName());
        
        return productService.getProductByQrHash(hash);
    }

    @GetMapping("/qr/{hash}/image")
    public ResponseEntity<byte[]> getQrCodeImage(@PathVariable String hash) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Log the attempt
        logger.info("User {} attempting to retrieve QR code image for hash: {}", auth.getName(), hash);
        
        try {
            byte[] qrImage = productService.generateQrCodeImage(hash);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(qrImage.length);
            headers.setCacheControl("public, max-age=31536000"); // Cache por 1 año (QR es inmutable)
            
            return new ResponseEntity<>(qrImage, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error generating QR code image for hash: {}", hash, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping
    public ResponseEntity<ResponseObject> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer stockCatalogueId,
            @RequestParam(required = false) Integer statusId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Log the attempt
        logger.info("User {} attempting to retrieve products - page: {}, size: {}, stockCatalogueId: {}, statusId: {}", 
            auth.getName(), page, size, stockCatalogueId, statusId);
        
        return productService.getAllProducts(page, size, stockCatalogueId, statusId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseObject> deleteProduct(@PathVariable Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        logger.info("User {} attempting to delete product with id {}", auth.getName(), id);

        return productService.deleteProduct(id);
    }
}

