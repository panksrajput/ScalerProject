import { API_CONFIG } from './config.js';
import { showMessage } from './utils.js';
import { Auth } from './auth.js';
import { Cart } from './cart.js';

/* ==========================
   AUTH TOKEN HELPER
========================== */
function getAuthToken() {
    return localStorage.getItem('authToken');
}

/* ==========================
   CREATE ORDER FROM CART
========================== */
const createOrderFromCart = async (orderDetails) => {
    try {
        const token = getAuthToken();
        if (!token) {
            throw new Error('User not authenticated');
        }

        if (
            !orderDetails ||
            !orderDetails.shippingAddress ||
            !orderDetails.billingAddress ||
            !orderDetails.paymentMethod
        ) {
            throw new Error('Missing required order details');
        }

        const requestBody = {
            shippingAddress: {
                ...orderDetails.shippingAddress,
                isDefault: orderDetails.shippingAddress.isDefault ?? true,
                addressType: 'SHIPPING'
            },
            billingAddress: {
                ...orderDetails.billingAddress,
                isDefault: orderDetails.billingAddress.isDefault ?? true,
                addressType: 'BILLING'
            },
            paymentMethod: orderDetails.paymentMethod
        };

        const response = await fetch(
            `${API_CONFIG.getFullUrl('ORDER', API_CONFIG.ENDPOINTS.ORDERS)}/from-cart`,
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(requestBody)
            }
        );

        if (!response.ok) {
            const err = await response.json().catch(() => ({}));
            throw new Error(err.message || 'Failed to create order');
        }

        const orderData = await response.json();
        showMessage('Order created successfully!', 'success');

        // Clear cart safely
        try {
            await Cart.clearCart();
            Cart.updateCartCount();
        } catch (e) {
            console.warn('Cart clear failed (non-blocking)', e);
        }

        return orderData;

    } catch (error) {
        console.error('Create order error:', error);
        showMessage(error.message || 'Order creation failed', 'error');
        throw error;
    }
};

/* ==========================
   INITIALIZE ORDERS PAGE
========================== */
function initializeOrdersPage() {
    if (!Auth.isAuthenticated()) {
        return;
    }

    const ordersList = document.getElementById('orders-list');
    if (!ordersList) return;

    Cart.loadCart()
        .then(() => Cart.updateCartCount())
        .catch(console.error);

    loadOrders();
    setupEventListeners();
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeOrdersPage);
} else {
    initializeOrdersPage();
}

/* ==========================
   LOAD ORDERS
========================== */
async function loadOrders(page = 0, size = 10) {
    try {
        const token = getAuthToken();
        if (!token) {
            Auth.logout();
            return;
        }

        const response = await fetch(
            `${API_CONFIG.getFullUrl('ORDER', API_CONFIG.ENDPOINTS.ORDERS)}?page=${page}&size=${size}&sort=createdAt,desc`,
            {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Accept': 'application/json'
                }
            }
        );

        if (!response.ok) {
            if (response.status === 401) {
                Auth.logout();
                return;
            }
            throw new Error('Failed to fetch orders');
        }

        const data = await response.json();
        renderOrders(data.content || []);

    } catch (error) {
        console.error('Load orders error:', error);
        showMessage(error.message || 'Failed to load orders', 'error');
    }
}

/* ==========================
   RENDER ORDERS
========================== */
export function renderOrders(orders) {
    const ordersList = document.getElementById('orders-list');
    if (!ordersList) return;

    if (!orders || !orders.length) {
        ordersList.innerHTML = `
            <div class="empty-orders">
                <i class="fas fa-box-open"></i>
                <h3>No orders found</h3>
                <p>You haven't placed any orders yet.</p>
                <a href="index.html" class="btn">Start Shopping</a>
            </div>
        `;
        return;
    }

    const ordersHtml = orders.map(order => {
        const orderDate = new Date(order.createdAt).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });

        const total = order.totalAmount || order.items?.reduce((sum, item) =>
            sum + (item.price * item.quantity), 0
        ) || 0;

        return `
            <div class="order-card" onclick="showOrderDetails(${JSON.stringify(order).replace(/"/g, '&quot;')})">
                <div class="order-card-header">
                    <div class="order-id">Order #${order.orderNumber || order.id}</div>
                    <div class="order-status status-${order.status}">${order.status}</div>
                </div>
                <div class="order-card-body">
                    <div class="order-date">${orderDate}</div>
                    <div>${order.items?.length || 0} items</div>
                </div>
                <div class="order-card-footer">
                    <div class="order-total">₹${total.toFixed(2)}</div>
                    <button class="btn btn-outline" onclick="event.stopPropagation(); showOrderDetails(${JSON.stringify(order).replace(/"/g, '&quot;')})">
                        View Details
                    </button>
                </div>
            </div>
        `;
    }).join('');

    ordersList.innerHTML = ordersHtml;
}

/* ==========================
   EVENTS
========================== */
function setupEventListeners() {
    const logoutLink = document.getElementById('logout-link');
    if (logoutLink) {
        logoutLink.addEventListener('click', (e) => {
            e.preventDefault();
            Auth.logout();
        });
    }
}

/* ==========================
   EXPORTS
========================== */
export {
    createOrderFromCart,
    loadOrders
};
