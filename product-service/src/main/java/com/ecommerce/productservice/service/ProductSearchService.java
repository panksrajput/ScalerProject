package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.ProductResponse;
import com.ecommerce.productservice.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductSearchService {
    void indexProduct(Product savedProduct);
    void deleteProductFromIndex(Long id);
    Page<ProductResponse> searchProducts(String query, Pageable pageable);
    Page<ProductResponse> searchByCategory(Long categoryId, String query, Pageable pageable);
    List<String> autocomplete(String query);
}
