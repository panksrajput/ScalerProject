package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.dto.AddToCartRequest;
import com.ecommerce.cartservice.dto.CartDto;

public interface CartService {
    CartDto getCart(Long userId);
    CartDto addToCart(Long userId, AddToCartRequest request);
    CartDto updateCartItem(Long userId, Long productId, int quantity);
    CartDto removeFromCart(Long userId, Long productId);
    void clearCart(Long userId);
    void deleteCart(Long userId);
    CartDto getCartByCartId(Long cartId);
}
