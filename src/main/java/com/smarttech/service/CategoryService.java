package com.smarttech.service;

import com.smarttech.dto.request.CategoryCreateRequest;
import com.smarttech.dto.response.CategoryResponse;
import com.smarttech.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CategoryCreateRequest request);
    CategoryResponse getCategoryById(Long id);
    CategoryResponse getCategoryByName(String name);
    List<CategoryResponse> getAllActiveCategories();
    Page<CategoryResponse> getAllCategories(Pageable pageable);
    CategoryResponse updateCategory(Long id, CategoryCreateRequest request);
    void deleteCategory(Long id);
    Category findEntityById(Long id);
}
