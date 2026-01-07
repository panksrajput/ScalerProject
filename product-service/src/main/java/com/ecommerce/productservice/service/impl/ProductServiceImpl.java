package com.ecommerce.productservice.service.impl;

import com.ecommerce.productservice.dto.ProductRequest;
import com.ecommerce.productservice.dto.ProductResponse;
import com.ecommerce.productservice.dto.StockReduceRequest;
import com.ecommerce.productservice.entity.Category;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.entity.ProductSpecification;
import com.ecommerce.productservice.exception.ResourceNotFoundException;
import com.ecommerce.productservice.repository.CategoryRepository;
import com.ecommerce.productservice.repository.ProductRepository;
import com.ecommerce.productservice.service.ProductSearchService;
import com.ecommerce.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductSearchService productSearchService;
    private final ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        logger.debug("Fetching all products");
        List<ProductResponse> products = productRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        logger.info("Successfully retrieved {} products", products.size());
        return products;
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        logger.debug("Fetching product with id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Product not found with id: {}", id);
                    return new ResourceNotFoundException("Product not found with id: " + id);
                });
        logger.debug("Successfully retrieved product with id: {}", id);
        return mapToDto(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategoryId(Long categoryId) {
        logger.debug("Fetching products for category id: {}", categoryId);

        List<Category> categories = categoryRepository.findCategoryAndChildren(categoryId);
        List<Long> categoryIds = categories.stream()
                .map(Category::getId)
                .collect(Collectors.toList());
        logger.debug("Found {} categories including children", categoryIds.size());

        List<Product> products = productRepository.findByCategoryIdIn(categoryIds);
        logger.info("Found {} products in category id: {}", products.size(), categoryId);

        return products.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest) {
        logger.info("Starting product creation for: {}", productRequest.getName());

        Product product = new Product();
        product.setName(productRequest.getName());
        product.setDescription(productRequest.getDescription());
        product.setPrice(productRequest.getPrice());
        product.setStockQuantity(productRequest.getStockQuantity());
        product.setSku(productRequest.getSku());
        product.setImageUrls(productRequest.getImageUrls());
        product.setIsActive(true);
        logger.debug("Created product entity with SKU: {}", productRequest.getSku());

        if (productRequest.getCategoryName() != null && !productRequest.getCategoryName().isBlank()) {
            logger.debug("Looking up category: {}", productRequest.getCategoryName());
            try {
                Category category = categoryRepository.findByName(productRequest.getCategoryName())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Category not found with name: " + productRequest.getCategoryName()));
                product.setCategory(category);
                logger.debug("Category set to: {}", category.getName());
            } catch (ResourceNotFoundException e) {
                logger.error("Failed to find category: {}", productRequest.getCategoryName(), e);
                throw e;
            }
        }

        logger.debug("Creating product specification");
        ProductSpecification specification = new ProductSpecification();
        specification.setBrand(productRequest.getBrand());
        specification.setModel(productRequest.getModel());
        specification.setColor(productRequest.getColor());
        specification.setSize(productRequest.getSize());
        specification.setWeight(productRequest.getWeight());
        specification.setDimensions(productRequest.getDimensions());
        specification.setMaterial(productRequest.getMaterial());
        specification.setWarranty(productRequest.getWarranty());
        specification.setCountryOfOrigin(productRequest.getCountryOfOrigin());
        specification.setShippingWeight(productRequest.getShippingWeight());
        specification.setManufacturer(productRequest.getManufacturer());
        specification.setCareInstructions(productRequest.getCareInstructions());
        specification.setIncludedComponents(productRequest.getIncludedComponents());
        specification.setRecommendedAgeRange(productRequest.getRecommendedAgeRange());

        product.setSpecification(specification);
        logger.debug("Product specification created successfully");

        logger.debug("Saving product to database");
        Product savedProduct = productRepository.save(product);
        logger.info("Successfully saved product with ID: {}", savedProduct.getId());

        try {
            logger.debug("Indexing product in Elasticsearch");
            productSearchService.indexProduct(savedProduct);
            logger.debug("Successfully indexed product in Elasticsearch");
        } catch (Exception e) {
            logger.error("Failed to index product with ID: " + savedProduct.getId(), e);
        }

        logger.debug("Mapping product to DTO");
        ProductResponse response = mapToDto(savedProduct);
        if (savedProduct.getCategory() != null) {
            response.setCategoryName(savedProduct.getCategory().getName());
            logger.debug("Set category name to: {}", response.getCategoryName());
        }

        logger.info("Successfully created product with ID: {}", savedProduct.getId());
        return response;
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest productRequest) {
        logger.info("Updating product with ID: {}", id);
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Product not found with ID: {}", id);
                    return new ResourceNotFoundException("Product not found with id: " + id);
                });
        logger.debug("Found existing product: {}", existingProduct.getName());

        logger.debug("Updating product fields");
        if (!existingProduct.getName().equals(productRequest.getName())) {
            logger.debug("Updating name from '{}' to '{}'", existingProduct.getName(), productRequest.getName());
            existingProduct.setName(productRequest.getName());
        }
        existingProduct.setDescription(productRequest.getDescription());
        existingProduct.setPrice(productRequest.getPrice());
        existingProduct.setStockQuantity(productRequest.getStockQuantity());
        existingProduct.setSku(productRequest.getSku());
        existingProduct.setImageUrls(productRequest.getImageUrls());
        existingProduct.setIsActive(true);

        if (productRequest.getCategoryName() != null) {
            if (productRequest.getCategoryName().isBlank()) {
                logger.debug("Removing category from product");
                existingProduct.setCategory(null);
            } else {
                logger.debug("Updating category to: {}", productRequest.getCategoryName());
                try {
                    Category category = categoryRepository.findByName(productRequest.getCategoryName())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Category not found with name: " + productRequest.getCategoryName()));
                    existingProduct.setCategory(category);
                } catch (ResourceNotFoundException e) {
                    logger.error("Failed to find category: {}", productRequest.getCategoryName(), e);
                    throw e;
                }
            }
        }

        logger.debug("Updating product specification");
        ProductSpecification specification = existingProduct.getSpecification();
        if (specification == null) {
            logger.debug("Creating new specification as none existed");
            specification = new ProductSpecification();
            existingProduct.setSpecification(specification);
        }

        logger.debug("Updating specification fields");
        if (productRequest.getBrand() != null) specification.setBrand(productRequest.getBrand());
        if (productRequest.getModel() != null) specification.setModel(productRequest.getModel());
        if (productRequest.getColor() != null) specification.setColor(productRequest.getColor());
        if (productRequest.getSize() != null) specification.setSize(productRequest.getSize());
        if (productRequest.getWeight() != null) specification.setWeight(productRequest.getWeight());
        if (productRequest.getDimensions() != null) specification.setDimensions(productRequest.getDimensions());
        if (productRequest.getMaterial() != null) specification.setMaterial(productRequest.getMaterial());
        if (productRequest.getWarranty() != null) specification.setWarranty(productRequest.getWarranty());
        if (productRequest.getCountryOfOrigin() != null) specification.setCountryOfOrigin(productRequest.getCountryOfOrigin());
        if (productRequest.getShippingWeight() != null) specification.setShippingWeight(productRequest.getShippingWeight());
        if (productRequest.getManufacturer() != null) specification.setManufacturer(productRequest.getManufacturer());
        if (productRequest.getCareInstructions() != null) specification.setCareInstructions(productRequest.getCareInstructions());
        if (productRequest.getIncludedComponents() != null) specification.setIncludedComponents(productRequest.getIncludedComponents());
        if (productRequest.getRecommendedAgeRange() != null) specification.setRecommendedAgeRange(productRequest.getRecommendedAgeRange());

        logger.debug("Saving updated product to database");
        Product updatedProduct = productRepository.save(existingProduct);
        logger.info("Successfully updated product with ID: {}", updatedProduct.getId());

        try {
            logger.debug("Updating product in Elasticsearch index");
            productSearchService.indexProduct(updatedProduct);
            logger.debug("Successfully updated product in Elasticsearch index");
        } catch (Exception e) {
            logger.error("Failed to update product in search index with ID: " + id, e);
        }

        logger.debug("Mapping updated product to DTO");
        ProductResponse response = mapToDto(updatedProduct);
        if (updatedProduct.getCategory() != null) {
            response.setCategoryName(updatedProduct.getCategory().getName());
            logger.debug("Set category name to: {}", response.getCategoryName());
        }

        logger.info("Successfully completed product update for ID: {}", id);
        return response;
    }

    @Transactional
    public void reduceStockBatch(List<StockReduceRequest> requests) {
        for (StockReduceRequest req : requests) {
            logger.info("Reducing stock quantity for product with ID: {}", req.getProductId());
            Product product = productRepository.findById(req.getProductId())
                    .orElseThrow();
            if (product.getStockQuantity() < req.getQuantity()) {
                throw new IllegalStateException("Insufficient stock");
            }
            product.setStockQuantity(product.getStockQuantity() - req.getQuantity());
        }
    }

    @Transactional
    public void deleteProduct(Long id) {
        logger.info("Deleting product with ID: {}", id);
        
        if (!productRepository.existsById(id)) {
            logger.warn("Product not found for deletion with ID: {}", id);
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        
        logger.debug("Deleting product from database");
        productRepository.deleteById(id);
        logger.info("Successfully deleted product with ID: {}", id);
        
        try {
            logger.debug("Deleting product from Elasticsearch index");
            productSearchService.deleteProductFromIndex(id);
            logger.debug("Successfully deleted product from Elasticsearch index");
        } catch (Exception e) {
            logger.error("Failed to delete product from search index with ID: " + id, e);
        }
    }
    
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String query, Pageable pageable) {
        return productSearchService.searchProducts(query, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProductsByCategory(Long categoryId, String query, Pageable pageable) {
        if (categoryId == null) {
            return searchProducts(query, pageable);
        }
        return productSearchService.searchByCategory(categoryId, query, pageable);
    }
    
    @Transactional(readOnly = true)
    public List<String> autocompleteSearch(String query) {
        return productSearchService.autocomplete(query);
    }

    private ProductResponse mapToDto(Product product) {
        ProductResponse response = modelMapper.map(product, ProductResponse.class);
        if (product.getCategory() != null) {
            response.setCategoryName(product.getCategory().getName());
        }

        // Map specification fields
        if (product.getSpecification() != null) {
            ProductSpecification spec = product.getSpecification();
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