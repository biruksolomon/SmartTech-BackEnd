package com.smarttech.repository;

import com.smarttech.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
    List<Category> findByIsActiveTrue();
    
    @Query("SELECT c FROM Category c WHERE c.isActive = true ORDER BY c.name ASC")
    List<Category> findActiveCategoriesOrderByName();
}
