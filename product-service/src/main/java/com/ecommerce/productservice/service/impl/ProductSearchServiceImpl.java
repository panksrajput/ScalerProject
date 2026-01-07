package com.ecommerce.productservice.service.impl;

import com.ecommerce.productservice.document.ProductDocument;
import com.ecommerce.productservice.dto.ProductResponse;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.entity.ProductSpecification;
import com.ecommerce.productservice.repository.elasticsearch.ProductSearchRepository;
import com.ecommerce.productservice.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchServiceImpl implements ProductSearchService {

    private final ProductSearchRepository productSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ModelMapper modelMapper;
    
    /**
     * Search products with fuzzy matching and typo tolerance
     */
    public Page<ProductResponse> searchProducts(String query, Pageable pageable) {
        // Create a more sophisticated search query
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.boolQuery()
                        .should(QueryBuilders.multiMatchQuery(query)
                                .field("name", 3.0f)
                                .field("description", 2.0f)
                                .field("specification.brand", 2.0f)
                                .field("specification.features")
                                .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
                                .fuzziness(Fuzziness.AUTO)
                                .prefixLength(2)
                                .fuzzyTranspositions(true)
                                .minimumShouldMatch("75%"))
                        .should(QueryBuilders.wildcardQuery("name", "*" + query.toLowerCase() + "*"))
                        .minimumShouldMatch(1))
                .withPageable(pageable)
                .build();

        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(searchQuery, ProductDocument.class);

        List<ProductResponse> results = searchHits.getSearchHits().stream()
                .map(hit -> mapToProductResponse(hit.getContent(), hit.getScore()))
                .collect(Collectors.toList());

        return new PageImpl<>(results, pageable, searchHits.getTotalHits());
    }
    
    /**
     * Search products by category with fuzzy matching
     */
    public Page<ProductResponse> searchByCategory(Long categoryId, String query, Pageable pageable) {
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery("categoryId", categoryId))
                        .must(QueryBuilders.multiMatchQuery(query)
                                .field("name", 3.0f)
                                .field("description", 2.0f)
                                .field("specification.brand", 2.0f)
                                .fuzziness(Fuzziness.AUTO)
                                .prefixLength(2)))
                .withPageable(pageable)
                .build();
                
        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(searchQuery, ProductDocument.class);
        
        List<ProductResponse> results = searchHits.getSearchHits().stream()
                .map(hit -> {
                    ProductResponse response = modelMapper.map(hit.getContent(), ProductResponse.class);
                    response.setScore(hit.getScore()); // Include relevance score
                    return response;
                })
                .collect(Collectors.toList());
                
        return new PageImpl<>(results, pageable, searchHits.getTotalHits());
    }
    
    /**
     * Autocomplete search for product names and brands
     */
    public List<String> autocomplete(String query) {
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery(query)
                        .field("name")
                        .field("specification.brand")
                        .type(MultiMatchQueryBuilder.Type.PHRASE_PREFIX))
                .withPageable(Pageable.ofSize(5)) // Limit to 5 suggestions
                .build();

        return elasticsearchOperations.search(searchQuery, ProductDocument.class)
                .getSearchHits()
                .stream()
                .map(hit -> {
                    ProductDocument product = hit.getContent();
                    return product.getName() != null ? product.getName() :
                            (product.getSpecification() != null && product.getSpecification().getBrand() != null ?
                                    product.getSpecification().getBrand() : "");
                })
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
    
    /**
     * Index a single product in Elasticsearch
     */
    public void indexProduct(Product product) {
        try {
            ProductDocument document = convertToDocument(product);
            log.debug("Indexing product document: {}", document);
            productSearchRepository.save(document);
            log.info("Successfully indexed product with ID: {}", product.getId());
        } catch (Exception e) {
            log.error("Error indexing product {}: {}", product.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to index product", e);
        }
    }

    /**
     * Delete a product from the index
     */
    public void deleteProductFromIndex(Long productId) {
        try {
            productSearchRepository.deleteById(productId);
        } catch (Exception e) {
            log.error("Error deleting product {} from index: {}", productId, e.getMessage(), e);
        }
    }
    
    /**
     * Convert Product entity to ProductDocument for indexing
     */
    private ProductDocument convertToDocument(Product product) {
        if (product == null) return null;

        return ProductDocument.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())  // Make sure this is included
                .sku(product.getSku())  // Make sure this is included
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .isActive(product.getIsActive())
                .specification(product.getSpecification())
                .build();
    }

    private ProductResponse mapToProductResponse(ProductDocument document, float score) {
        ProductResponse response = new ProductResponse();
        response.setId(document.getId());
        response.setName(document.getName());
        response.setDescription(document.getDescription());
        response.setPrice(document.getPrice());
        response.setStockQuantity(document.getStockQuantity());
        response.setSku(document.getSku());
        response.setCategoryName(document.getCategoryName());
        response.setScore(score);

        // Map specification fields if they exist
        if (document.getSpecification() != null) {
            ProductSpecification spec = document.getSpecification();
            response.setBrand(spec.getBrand());
            response.setModel(spec.getModel());
            response.setColor(spec.getColor());
            response.setSize(spec.getSize());
            response.setWeight(spec.getWeight());
            response.setDimensions(spec.getDimensions());
            response.setMaterial(spec.getMaterial());
            response.setWarranty(spec.getWarranty());
            response.setCountryOfOrigin(spec.getCountryOfOrigin());
            response.setShippingWeight(spec.getShippingWeight());
            response.setManufacturer(spec.getManufacturer());
            response.setCareInstructions(spec.getCareInstructions());
            response.setIncludedComponents(spec.getIncludedComponents());
            response.setRecommendedAgeRange(spec.getRecommendedAgeRange());
        }

        return response;
    }
}
