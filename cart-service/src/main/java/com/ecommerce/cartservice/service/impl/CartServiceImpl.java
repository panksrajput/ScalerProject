package com.ecommerce.cartservice.service.impl;

import com.ecommerce.cartservice.client.ProductClient;
import com.ecommerce.cartservice.dto.AddToCartRequest;
import com.ecommerce.cartservice.dto.CartDto;
import com.ecommerce.cartservice.dto.CartItemDto;
import com.ecommerce.cartservice.dto.ProductDto;
import com.ecommerce.cartservice.entity.Cart;
import com.ecommerce.cartservice.entity.CartItem;
import com.ecommerce.cartservice.exception.CartNotFoundException;
import com.ecommerce.cartservice.repository.CartMongoRepository;
import com.ecommerce.cartservice.repository.CartRedisRepository;
import com.ecommerce.cartservice.service.CartService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {
    private static final Logger logger = LoggerFactory.getLogger(CartServiceImpl.class);

    private final CartMongoRepository cartMongoRepository;
    private final CartRedisRepository cartRedisRepository;
    private final ProductClient productClient;

    @Override
    public CartDto getCart(Long userId) {
        logger.debug("Fetching cart for user ID: {}", userId);
        Cart cart = getCartInternal(userId);
        logger.debug("Successfully fetched cart for user ID: {} with {} items", 
                   userId, cart.getItems().size());
        return toDto(cart);
    }

    @Override
    public CartDto addToCart(Long userId, AddToCartRequest request) {
        logger.info("Adding to cart - User ID: {}, Request: {}", 
                  userId, request);
        
        Cart cart = getCartInternal(userId);
        logger.debug("Retrieved cart with ID: {} for user ID: {}", 
                   cart.getId(), userId);

        ProductDto product = fetchProduct(request.getProductId());
        logger.debug("Fetched product details - ID: {}, Name: {}", 
                   product.getId(), product.getName());

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(product.getId()))
                .findFirst()
                .orElseGet(() -> createCartItem(cart, product));

        logger.debug("Updating cart item - Product ID: {}, Current Qty: {}, Adding: {}", 
                   product.getId(), item.getQuantity(), request.getQuantity());
        
        item.incrementQuantity(request.getQuantity());
        cart.updateTotals();
        save(cart);

        logger.info("Successfully added to cart - User ID: {}, Product ID: {}, New Qty: {}", 
                  userId, product.getId(), item.getQuantity());
        
        return toDto(cart);
    }

    @Override
    public CartDto updateCartItem(Long userId, Long productId, int quantity) {
        logger.info("Updating cart item - User ID: {}, Product ID: {}, New Qty: {}", 
                  userId, productId, quantity);
        
        Cart cart = getCartInternal(userId);
        
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> {
                    logger.warn("Item not found in cart - User ID: {}, Product ID: {}", 
                              userId, productId);
                    return new CartNotFoundException("Item not found in cart");
                });

        if (quantity <= 0) {
            logger.debug("Quantity is 0 or negative, removing item from cart");
            return removeFromCart(userId, productId);
        }

        logger.debug("Updating quantity from {} to {}", item.getQuantity(), quantity);
        item.updateQuantity(quantity);
        cart.updateTotals();
        save(cart);

        logger.info("Successfully updated cart item - User ID: {}, Product ID: {}, Qty: {}", 
                  userId, productId, quantity);
        
        return toDto(cart);
    }

    @Override
    public CartDto removeFromCart(Long userId, Long productId) {
        logger.info("Removing item from cart - User ID: {}, Product ID: {}", 
                  userId, productId);
        
        Cart cart = getCartInternal(userId);
        boolean removed = cart.getItems().removeIf(i -> i.getProductId().equals(productId));
        
        if (!removed) {
            logger.warn("Item not found in cart for removal - User ID: {}, Product ID: {}", 
                      userId, productId);
            throw new CartNotFoundException("Item not found in cart");
        }

        cart.updateTotals();
        
        if (cart.getItems().isEmpty()) {
            logger.debug("Cart is empty after removal, deleting cart for user ID: {}", 
                       userId);
            deleteCart(userId);
            return emptyCartDto(userId);
        }

        save(cart);
        logger.info("Successfully removed item from cart - User ID: {}, Product ID: {}", 
                  userId, productId);
        
        return toDto(cart);
    }

    @Override
    public void clearCart(Long userId) {
        logger.info("Clearing cart for user ID: {}", userId);
        deleteCart(userId);
        logger.info("Successfully cleared cart for user ID: {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public CartDto getCartByCartId(Long cartId) {
        logger.debug("Fetching cart by ID: {}", cartId);
        Cart cart = cartMongoRepository.findById(String.valueOf(cartId))
                .orElseThrow(() -> {
                    logger.error("Cart not found with ID: {}", cartId);
                    return new CartNotFoundException("Cart not found");
                });
        logger.debug("Successfully fetched cart by ID: {}", cartId);
        return toDto(cart);
    }

    /* ------------------------- INTERNAL METHODS ------------------------- */

    private Cart getCartInternal(Long userId) {
        logger.debug("Getting cart from cache for user ID: {}", userId);
        Optional<Cart> redisCart = cartRedisRepository.findById(userId);
        if (redisCart.isPresent()) {
            logger.debug("Cache hit for user ID: {}", userId);
            return redisCart.get();
        }

        logger.debug("Cache miss for user ID: {}, checking database", userId);
        Cart mongoCart = cartMongoRepository.findByUserId(userId)
                .orElseGet(() -> {
                    logger.debug("No cart found in database for user ID: {}, creating new cart", 
                              userId);
                    return createCart(userId);
                });

        logger.debug("Updating cache for user ID: {}", userId);
        cartRedisRepository.save(mongoCart);
        return mongoCart;
    }

    private Cart createCart(Long userId) {
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setCreatedAt(LocalDateTime.now());
        cart.setUpdatedAt(LocalDateTime.now());
        return cartMongoRepository.save(cart);
    }

    private void save(Cart cart) {
        cartMongoRepository.save(cart);
        cartRedisRepository.save(cart);
    }

    @Override
    public void deleteCart(Long userId) {
        cartMongoRepository.deleteByUserId(userId);
        cartRedisRepository.deleteById(userId);
    }


    private ProductDto fetchProduct(Long productId) {
        logger.debug("Fetching product details for ID: {}", productId);
        try {
            ProductDto product = productClient.getProduct(productId);
            logger.debug("Successfully fetched product - ID: {}, Name: {}", 
                      productId, product.getName());
            return product;
        } catch (FeignException.NotFound e) {
            logger.error("Product not found - ID: {}", productId, e);
            throw new CartNotFoundException("Product not found: " + productId);
        } catch (Exception e) {
            logger.error("Error fetching product - ID: {}", productId, e);
            throw new RuntimeException("Failed to fetch product: " + productId, e);
        }
    }

    private CartItem createCartItem(Cart cart, ProductDto product) {
        CartItem item = new CartItem();
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setProductPrice(
                product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO
        );
        item.setImageUrl(product.getImageUrl());
        item.setQuantity(0);
        item.setTotalPrice(BigDecimal.ZERO);

        cart.getItems().add(item);
        return item;
    }

    private CartDto toDto(Cart cart) {
        CartDto dto = new CartDto();
        dto.setUserId(cart.getUserId());
        dto.setItems(toItemDtos(cart.getItems()));
        dto.setTotalItems(cart.getTotalItems());
        dto.setTotalPrice(cart.getTotalPrice());
        dto.setEmpty(cart.getItems().isEmpty());
        return dto;
    }

    private CartDto emptyCartDto(Long userId) {
        CartDto dto = new CartDto();
        dto.setUserId(userId);
        dto.setTotalItems(0);
        dto.setTotalPrice(BigDecimal.ZERO);
        dto.setEmpty(true);
        return dto;
    }

    private List<CartItemDto> toItemDtos(List<CartItem> items) {
        return items.stream().map(item -> {
            CartItemDto dto = new CartItemDto();
            dto.setProductId(item.getProductId());
            dto.setProductName(item.getProductName());
            dto.setProductPrice(item.getProductPrice());
            dto.setQuantity(item.getQuantity());
            dto.setImageUrl(item.getImageUrl());
            dto.setSku(item.getSku());
            dto.setTotalPrice(item.getTotalPrice());
            return dto;
        }).toList();
    }

}
