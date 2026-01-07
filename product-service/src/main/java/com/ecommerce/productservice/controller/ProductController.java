package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.dto.ProductRequest;
import com.ecommerce.productservice.dto.ProductResponse;
import com.ecommerce.productservice.dto.StockReduceRequest;
import com.ecommerce.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        logger.info("Received request to get all products");
        List<ProductResponse> products = productService.getAllProducts();
        logger.debug("Retrieved {} products", products.size());
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        logger.info("Received request to get product by id: {}", id);
        ProductResponse product = productService.getProductById(id);
        logger.debug("Retrieved product with id {}: {}", id, product);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(@PathVariable Long categoryId) {
        logger.info("Received request to get products by category id: {}", categoryId);
        List<ProductResponse> products = productService.getProductsByCategoryId(categoryId);
        logger.debug("Retrieved {} products for category id: {}", products.size(), categoryId);
        return ResponseEntity.ok(products);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest productRequest) {
        logger.info("Received request to create product: {}", productRequest.getName());
        ProductResponse createdProduct = productService.createProduct(productRequest);
        logger.info("Successfully created product with id: {}", createdProduct.getId());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdProduct.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdProduct);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest productRequest) {
        logger.info("Received request to update product with id: {}", id);
        ProductResponse updatedProduct = productService.updateProduct(id, productRequest);
        logger.info("Successfully updated product with id: {}", id);
        return ResponseEntity.ok(updatedProduct);
    }

    @PostMapping("/decrement-stock/batch")
    public ResponseEntity<Void> reduceStockBatch(
            @RequestBody List<StockReduceRequest> requests) {

        productService.reduceStockBatch(requests);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        logger.info("Received request to delete product with id: {}", id);
        productService.deleteProduct(id);
        logger.info("Successfully deleted product with id: {}", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @RequestParam String query,
            @RequestParam(required = false) Long categoryId,
            Pageable pageable) {
        logger.info("Received search request - query: {}, categoryId: {}, pageable: {}", 
                   query, categoryId, pageable);

        Page<ProductResponse> results;
        if (categoryId != null) {
            results = productService.searchProductsByCategory(categoryId, query, pageable);
        } else {
            results = productService.searchProducts(query, pageable);
        }

        logger.debug("Search returned {} results", results.getTotalElements());
        return ResponseEntity.ok(results);
    }

    @GetMapping("/search/autocomplete")
    public ResponseEntity<List<String>> autocompleteSearch(@RequestParam String query) {
        logger.debug("Received autocomplete request for query: {}", query);
        List<String> suggestions = productService.autocompleteSearch(query);
        logger.debug("Returning {} suggestions for query: {}", suggestions.size(), query);
        return ResponseEntity.ok(suggestions);
    }
}