package com.ecommerce.productservice.service.impl;

import com.ecommerce.productservice.dto.CategoryRequest;
import com.ecommerce.productservice.dto.CategoryResponse;
import com.ecommerce.productservice.entity.Category;
import com.ecommerce.productservice.exception.ResourceNotFoundException;
import com.ecommerce.productservice.repository.CategoryRepository;
import com.ecommerce.productservice.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private static final Logger logger = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        logger.debug("Fetching all categories");
        List<Category> categories = categoryRepository.findAll();
        logger.info("Successfully retrieved {} categories", categories.size());
        return categories.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        logger.debug("Fetching category with id: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Category not found with id: {}", id);
                    return new ResourceNotFoundException("Category not found with id: " + id);
                });
        logger.debug("Successfully retrieved category: {}", category.getName());
        return mapToDto(category);
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest categoryRequest) {
        logger.info("Creating new category: {}", categoryRequest.getName());
        
        // Check if category with same name already exists
        if (categoryRepository.existsByName(categoryRequest.getName())) {
            logger.warn("Category with name '{}' already exists", categoryRequest.getName());
            throw new IllegalArgumentException("Category with name '" + categoryRequest.getName() + "' already exists");
        }
        
        Category category = modelMapper.map(categoryRequest, Category.class);
        logger.debug("Mapped request to category entity");

        // Set parent category if parentId is provided
        if (categoryRequest.getParentId() != null) {
            logger.debug("Setting parent category with id: {}", categoryRequest.getParentId());
            try {
                Category parent = categoryRepository.findById(categoryRequest.getParentId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Parent category not found with id: " + categoryRequest.getParentId()));
                category.setParent(parent);
                logger.debug("Set parent category to: {}", parent.getName());
            } catch (ResourceNotFoundException e) {
                logger.error("Failed to find parent category with id: {}", categoryRequest.getParentId(), e);
                throw e;
            }
        }

        logger.debug("Saving category to database");
        Category savedCategory = categoryRepository.save(category);
        logger.info("Successfully created category with ID: {}", savedCategory.getId());
        
        return mapToDto(savedCategory);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest categoryRequest) {
        logger.info("Updating category with ID: {}", id);
        
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Category not found with id: {}", id);
                    return new ResourceNotFoundException("Category not found with id: " + id);
                });
        
        logger.debug("Updating category fields for: {}", category.getName());
        
        // Check if name is being changed and if the new name already exists
        if (!category.getName().equals(categoryRequest.getName()) && 
            categoryRepository.existsByName(categoryRequest.getName())) {
            logger.warn("Category with name '{}' already exists", categoryRequest.getName());
            throw new IllegalArgumentException("Category with name '" + categoryRequest.getName() + "' already exists");
        }
        
        // Update fields
        category.setName(categoryRequest.getName());
        category.setDescription(categoryRequest.getDescription());
        category.setImageUrl(categoryRequest.getImageUrl());
        
        // Update parent if needed
        if (categoryRequest.getParentId() != null) {
            if (!categoryRequest.getParentId().equals(category.getParent() != null ? category.getParent().getId() : null)) {
                logger.debug("Updating parent category to ID: {}", categoryRequest.getParentId());
                try {
                    Category parent = categoryRepository.findById(categoryRequest.getParentId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Parent category not found with id: " + categoryRequest.getParentId()));
                    
                    // Prevent circular reference
                    if (isCircularReference(category, parent)) {
                        logger.error("Circular reference detected when setting parent category");
                        throw new IllegalArgumentException("Circular reference detected in category hierarchy");
                    }
                    
                    category.setParent(parent);
                    logger.debug("Updated parent category to: {}", parent.getName());
                } catch (ResourceNotFoundException e) {
                    logger.error("Failed to find parent category with id: {}", categoryRequest.getParentId(), e);
                    throw e;
                }
            }
        } else {
            logger.debug("Removing parent category");
            category.setParent(null);
        }
        
        logger.debug("Saving updated category");
        Category updatedCategory = categoryRepository.save(category);
        logger.info("Successfully updated category with ID: {}", id);
        
        return mapToDto(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long id) {
        logger.info("Deleting category with ID: {}", id);
        
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Category not found with id: {}", id);
                    return new ResourceNotFoundException("Category not found with id: " + id);
                });
        
        logger.debug("Found category: {}", category.getName());
        
        // Check if category has products
        if (!category.getProducts().isEmpty()) {
            logger.warn("Cannot delete category '{}' as it has {} associated products", 
                       category.getName(), category.getProducts().size());
            throw new IllegalStateException("Cannot delete category with " + category.getProducts().size() + " associated products");
        }
        
        // Check for child categories
        if (!category.getChildren().isEmpty()) {
            logger.warn("Cannot delete category '{}' as it has {} child categories", 
                       category.getName(), category.getChildren().size());
            throw new IllegalStateException("Cannot delete category with " + category.getChildren().size() + " child categories");
        }
        
        logger.debug("Deleting category from database");
        categoryRepository.delete(category);
        logger.info("Successfully deleted category with ID: {}", id);
    }

    private CategoryResponse mapToDto(Category category) {
        logger.trace("Mapping category to DTO: {}", category.getName());
        
        CategoryResponse response = modelMapper.map(category, CategoryResponse.class);
        
        if (category.getParent() != null) {
            response.setParentId(category.getParent().getId());
            logger.trace("Set parent ID to: {}", category.getParent().getId());
        }
        
        // Map children recursively if needed
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            logger.trace("Mapping {} child categories", category.getChildren().size());
            response.setChildren(category.getChildren().stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toSet()));
        }
        
        return response;
    }

    private boolean isCircularReference(Category category, Category potentialParent) {
        if (category.getId().equals(potentialParent.getId())) {
            return true;
        }

        Category current = potentialParent.getParent();
        while (current != null) {
            if (current.getId().equals(category.getId())) {
                return true;
            }
            current = current.getParent();
        }
        
        return false;
    }
}
