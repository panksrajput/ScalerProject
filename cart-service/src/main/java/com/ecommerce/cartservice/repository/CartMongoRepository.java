package com.ecommerce.cartservice.repository;

import com.ecommerce.cartservice.entity.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CartMongoRepository extends MongoRepository<Cart, String> {
    Optional<Cart> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
