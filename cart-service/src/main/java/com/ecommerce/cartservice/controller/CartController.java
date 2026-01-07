package com.ecommerce.cartservice.controller;

import com.ecommerce.cartservice.dto.AddToCartRequest;
import com.ecommerce.cartservice.dto.CartDto;
import com.ecommerce.cartservice.security.CurrentUserResolver;
import com.ecommerce.cartservice.service.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;
    private final CurrentUserResolver currentUserResolver;

    public CartController(CartService cartService,
                          CurrentUserResolver currentUserResolver) {
        this.cartService = cartService;
        this.currentUserResolver = currentUserResolver;
    }

    private Long getCurrentUserId() {
        return currentUserResolver.resolve().getUserId();
    }

    @GetMapping
    public ResponseEntity<CartDto> getCart() {
        Long userId = getCurrentUserId();
        logger.info("Fetching cart for user ID: {}", userId);
        CartDto cart = cartService.getCart(userId);
        logger.debug("Retrieved cart for user ID: {}, item count: {}", userId,
                 cart.getItems() != null ? cart.getItems().size() : 0);
        return ResponseEntity.ok(cart);
    }

    @GetMapping("/{cartId}")
    public ResponseEntity<CartDto> getCartById(@PathVariable Long cartId) {
        logger.info("Fetching cart by ID: {}", cartId);
        CartDto cart = cartService.getCartByCartId(cartId);
        logger.debug("Retrieved cart by ID: {}, user ID: {}, item count: {}",
                cartId, cart.getUserId(),
                cart.getItems() != null ? cart.getItems().size() : 0);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/items")
    public ResponseEntity<CartDto> addToCart(@RequestBody AddToCartRequest request) {
        Long userId = getCurrentUserId();
        logger.info("Adding item to cart - User ID: {}, Product ID: {}, Quantity: {}", userId, request.getProductId(), request.getQuantity());
        CartDto updatedCart = cartService.addToCart(userId, request);
        logger.debug("Item added to cart successfully. User ID: {}, Product ID: {}", userId, request.getProductId());
        return ResponseEntity.ok(updatedCart);
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<CartDto> updateCartItem(@PathVariable Long productId,
                                                 @RequestParam int quantity) {
        Long userId = getCurrentUserId();
        logger.info("Updating cart item - User ID: {}, Product ID: {}, New Quantity: {}", userId, productId, quantity);
        CartDto updatedCart = cartService.updateCartItem(userId, productId, quantity);
        logger.debug("Cart item updated successfully. User ID: {}, Product ID: {}",
                 userId, productId);
        return ResponseEntity.ok(updatedCart);
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartDto> removeFromCart(@PathVariable Long productId) {
        Long userId = getCurrentUserId();
        logger.info("Removing item from cart - User ID: {}, Product ID: {}",
                userId, productId);
        CartDto updatedCart = cartService.removeFromCart(userId, productId);
        logger.info("Item removed from cart. User ID: {}, Product ID: {}",
                userId, productId);
        return ResponseEntity.ok(updatedCart);
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart() {
        Long userId = getCurrentUserId();
        logger.info("Clearing cart for user ID: {}", userId);
        cartService.clearCart(userId);
        logger.info("Cart cleared successfully for user ID: {}", userId);
        return ResponseEntity.noContent().build();
    }
}
