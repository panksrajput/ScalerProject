// config.js
const API_CONFIG = {
    AUTH_SERVICE: 'http://localhost:8082',
    PRODUCT_SERVICE: 'http://localhost:8083',
    CART_SERVICE: 'http://localhost:8084',
    ORDER_SERVICE: 'http://localhost:8085',
    PAYMENT_SERVICE: 'http://localhost:8086',
    ENDPOINTS: {
        LOGIN: '/api/auth/signin',
        SIGNUP: '/api/auth/signup',
        FORGOT_PASSWORD: '/api/auth/forgot-password',
        PRODUCTS: '/api/products',
        SEARCH: '/api/products/search',
        CART: '/api/cart',
        CART_ITEMS: '/api/cart/items',
        CART_COUNT: '/api/cart/count',
        ORDERS: '/api/orders',
        PAYMENT: '/api/payment/create',
        PAYMENT_INIT: '/api/orders/payment/init',
        PAYMENT_STATUS: '/api/orders/payment/status'
    },
    getFullUrl: function(service, endpoint) {
        const baseUrl = this[`${service}_SERVICE`];
        return baseUrl ? `${baseUrl}${endpoint}` : endpoint;
    }
};

// Export for ES modules
export { API_CONFIG };

// Make it globally available for non-module scripts
window.API_CONFIG = API_CONFIG;