package com.ecommerce.productservice.repository;

import com.ecommerce.productservice.entity.Category;
import com.ecommerce.productservice.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    Optional<Category> findByName(String name);
    
    List<Category> findByParentIsNull();
    
    List<Category> findByParentId(Long parentId);
    
    boolean existsByName(String name);
    
    @Query("SELECT c FROM Category c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Category> search(@Param("query") String query);
    
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.children WHERE c.parent IS NULL")
    List<Category> findAllWithChildren();

    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId")
    List<Category> findChildCategories(@Param("parentId") Long parentId);

    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId OR c.id = :parentId")
    List<Category> findCategoryAndChildren(@Param("parentId") Long parentId);
    
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.products p WHERE c.id = :categoryId")
    Optional<Category> findByIdWithProducts(@Param("categoryId") Long categoryId);
    
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL")
    List<Category> findRootCategories();
}
