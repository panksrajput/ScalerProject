package com.ecommerce.productservice.repository.elasticsearch;

import com.ecommerce.productservice.document.ProductDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, Long> {
    
    // Full-text search with fuzzy matching
    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"name^3\", \"description^2\", \"specification.brand^2\", \"specification.features\"], \"fuzziness\": \"AUTO\", \"prefix_length\": 2, \"max_expansions\": 10 }}")
    SearchHits<ProductDocument> searchWithFuzzy(String query);
    
    // Autocomplete search
    @Query("{\"bool\": {\"should\": [{\"match_phrase_prefix\": {\"name\": \"?0\"}}, {\"match_phrase_prefix\": {\"specification.brand\": \"?0\"}}, {\"match_phrase_prefix\": {\"description\": \"?0\"}}]}}")
    List<ProductDocument> autocompleteSearch(String query, Pageable pageable);
    
    // Search by category with filters
    Page<ProductDocument> findByCategoryIdAndIsActiveTrue(Long categoryId, Pageable pageable);
    
    // Search by multiple categories
    Page<ProductDocument> findByCategoryIdInAndIsActiveTrue(List<Long> categoryIds, Pageable pageable);
    
    // Find active products
    Page<ProductDocument> findByIsActiveTrue(Pageable pageable);
}
