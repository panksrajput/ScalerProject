package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.CategoryRequest;
import com.ecommerce.productservice.dto.CategoryResponse;

import javax.validation.Valid;
import java.util.List;

public interface CategoryService {
    public List<CategoryResponse> getAllCategories();
    public CategoryResponse getCategoryById(Long id);
    public CategoryResponse createCategory(@Valid CategoryRequest categoryRequest);
    public CategoryResponse updateCategory(Long id, @Valid CategoryRequest categoryRequest);
    public void deleteCategory(Long id);
}
