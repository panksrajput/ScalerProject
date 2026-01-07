package com.ecommerce.cartservice.repository;

import com.ecommerce.cartservice.entity.Cart;
import org.springframework.data.repository.CrudRepository;

public interface CartRedisRepository extends CrudRepository<Cart, Long> {
}
