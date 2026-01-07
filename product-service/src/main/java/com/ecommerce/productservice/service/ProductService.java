package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.ProductRequest;
import com.ecommerce.productservice.dto.ProductResponse;
import com.ecommerce.productservice.dto.StockReduceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.validation.Valid;
import java.util.List;

public interface ProductService {
    List<ProductResponse> getAllProducts();
    ProductResponse getProductById(Long id);
    List<ProductResponse> getProductsByCategoryId(Long categoryId);
    ProductResponse createProduct(@Valid ProductRequest productRequest);
    ProductResponse updateProduct(Long id, @Valid ProductRequest productRequest);
    void deleteProduct(Long id);
    Page<ProductResponse> searchProductsByCategory(Long categoryId, String query, Pageable pageable);
    Page<ProductResponse> searchProducts(String query, Pageable pageable);
    List<String> autocompleteSearch(String query);
    void reduceStockBatch(List<StockReduceRequest> requests);
}
