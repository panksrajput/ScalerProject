package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.dto.CategoryRequest;
import com.ecommerce.productservice.dto.CategoryResponse;
import com.ecommerce.productservice.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        logger.info("Received request to get all categories");
        List<CategoryResponse> categories = categoryService.getAllCategories();
        logger.debug("Retrieved {} categories", categories.size());
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        logger.info("Received request to get category by id: {}", id);
        CategoryResponse category = categoryService.getCategoryById(id);
        logger.debug("Retrieved category with id {}: {}", id, category);
        return ResponseEntity.ok(category);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest categoryRequest) {
        logger.info("Received request to create category: {}", categoryRequest.getName());
        CategoryResponse createdCategory = categoryService.createCategory(categoryRequest);
        logger.info("Successfully created category with id: {}", createdCategory.getId());
        
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdCategory.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdCategory);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest categoryRequest) {
        logger.info("Received request to update category with id: {}", id);
        CategoryResponse updatedCategory = categoryService.updateCategory(id, categoryRequest);
        logger.info("Successfully updated category with id: {}", id);
        return ResponseEntity.ok(updatedCategory);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        logger.info("Received request to delete category with id: {}", id);
        categoryService.deleteCategory(id);
        logger.info("Successfully deleted category with id: {}", id);
        return ResponseEntity.noContent().build();
    }
}
