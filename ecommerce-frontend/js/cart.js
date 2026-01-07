import { showMessage } from './utils.js';
import { API_CONFIG } from './config.js';
import { createOrderFromCart } from './orders.js';

class Cart {
    static currentCart = null;

    /* ===============================
       AUTH TOKEN
       =============================== */
    static getAuthToken() {
        return localStorage.getItem('authToken');
    }

    /* ===============================
       ADD TO CART
       =============================== */
    static async addToCart(productId, quantity = 1) {
        try {
            const token = this.getAuthToken();
            if (!token) {
                showMessage('Please login to add items to cart', 'error');
                return false;
            }

            const response = await fetch(
                API_CONFIG.getFullUrl('CART', API_CONFIG.ENDPOINTS.CART_ITEMS),
                {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    },
                    body: JSON.stringify({ productId, quantity })
                }
            );

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Failed to add item to cart');
            }

            await this.loadCart();
            this.updateCartCount();
            showMessage('Item added to cart', 'success');
            return true;

        } catch (error) {
            console.error('Add to cart error:', error);
            showMessage(error.message || 'Failed to add item to cart', 'error');
            return false;
        }
    }

    /* ===============================
       LOAD CART
       =============================== */
    static async loadCart() {
        const token = this.getAuthToken();
        if (!token) return null;

        const response = await fetch(
            API_CONFIG.getFullUrl('CART', API_CONFIG.ENDPOINTS.CART),
            {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Accept': 'application/json'
                }
            }
        );

        if (!response.ok) {
            throw new Error('Failed to load cart');
        }

        this.currentCart = await response.json();
        return this.currentCart;
    }

    /* ===============================
       UPDATE CART ITEM
       =============================== */
    static async updateCartItem(itemId, quantity) {
        if (quantity < 1) return;

        const token = this.getAuthToken();
        if (!token) return;

        const response = await fetch(
            `${API_CONFIG.getFullUrl('CART', API_CONFIG.ENDPOINTS.CART_ITEMS)}/${itemId}?quantity=${encodeURIComponent(quantity)}`,
            {
                method: 'PUT',
                headers: { 'Authorization': `Bearer ${token}` }
            }
        );

        if (!response.ok) {
            throw new Error('Failed to update cart item');
        }

        this.currentCart = await response.json();
        return this.currentCart;
    }

    /* ===============================
       REMOVE CART ITEM
       =============================== */
    static async removeItem(itemId) {
        const token = this.getAuthToken();
        if (!token) return;

        const response = await fetch(
            `${API_CONFIG.getFullUrl('CART', API_CONFIG.ENDPOINTS.CART_ITEMS)}/${itemId}`,
            {
                method: 'DELETE',
                headers: { 'Authorization': `Bearer ${token}` }
            }
        );

        if (!response.ok) {
            throw new Error('Failed to remove item');
        }

        await this.loadCart();
    }

    /* ===============================
       RENDER CART
       =============================== */
    static async renderCart() {
        const cartItemsEl = document.getElementById('cart-items');
        const summaryEl = document.querySelector('.cart-summary');

        if (!cartItemsEl) return;

        const cart = this.currentCart || await this.loadCart();

        if (!cart || !cart.items || cart.items.length === 0) {
            cartItemsEl.innerHTML = `<div class="empty-cart"><p>Your cart is empty</p></div>`;
            if (summaryEl) summaryEl.style.display = 'none';
            return;
        }

        let subtotal = 0;

        cartItemsEl.innerHTML = cart.items.map(item => {
            const total = item.productPrice * item.quantity;
            subtotal += total;

            return `
                <div class="cart-item" data-item-id="${item.productId}">
                    <div class="cart-item-details">
                        <h4>${item.productName}</h4>
                        <p>₹${item.productPrice}</p>
                        <div class="quantity-controls">
                            <button class="qty-btn" data-action="decrease">−</button>
                            <span class="quantity">${item.quantity}</span>
                            <button class="qty-btn" data-action="increase">+</button>
                        </div>
                        <button class="remove-item">Remove</button>
                    </div>
                    <div class="cart-item-total">₹${total}</div>
                </div>
            `;
        }).join('');

        document.getElementById('subtotal').textContent = `₹${subtotal}`;
        document.getElementById('total').textContent = `₹${subtotal}`;
        summaryEl.style.display = 'block';

        this.attachEventHandlers();
        this.updateCartCount();

        document.getElementById('checkout-btn').onclick = () =>
            Cart.toggleCheckoutPanel();

        document.getElementById('pay-btn').onclick = () =>
            Cart.startPayment();
    }

    /* ===============================
       EVENTS
       =============================== */
    static attachEventHandlers() {
        document.querySelectorAll('.qty-btn').forEach(btn => {
            btn.onclick = async (e) => {
                const itemEl = e.target.closest('.cart-item');
                const itemId = itemEl.dataset.itemId;
                let qty = parseInt(itemEl.querySelector('.quantity').textContent, 10);
                if (e.target.dataset.action === 'increase') qty++;
                if (e.target.dataset.action === 'decrease' && qty > 1) qty--;
                await this.updateCartItem(itemId, qty);
                await this.renderCart();
            };
        });

        document.querySelectorAll('.remove-item').forEach(btn => {
            btn.onclick = async (e) => {
                const itemId = e.target.closest('.cart-item').dataset.itemId;
                await this.removeItem(itemId);
                await this.renderCart();
            };
        });
    }

    /* ===============================
       CLEAR CART
       =============================== */
    static async clearCart() {
        try {
            const token = this.getAuthToken();
            if (!token) return;

            const response = await fetch(
                API_CONFIG.getFullUrl('CART', API_CONFIG.ENDPOINTS.CART),
                {
                    method: 'DELETE',
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                }
            );

            // Some backends return 204 No Content
            if (!response.ok && response.status !== 204) {
                throw new Error('Failed to clear cart');
            }

            // Reset local cart state
            this.currentCart = {
                items: []
            };

            this.updateCartCount();

            console.log('Cart cleared successfully');

        } catch (error) {
            // NON-BLOCKING by design (as you wanted)
            console.warn('Cart clear failed (non-blocking)', error);
        }
    }

    /* ===============================
       CART COUNT
       =============================== */
    static updateCartCount() {
        const countEl = document.querySelector('.cart-count');
        if (!countEl || !this.currentCart) return;

        const count = this.currentCart.items.reduce((s, i) => s + i.quantity, 0);
        countEl.textContent = count;
        countEl.style.display = count > 0 ? 'inline-flex' : 'none';
    }

    /* ===============================
       CHECKOUT PANEL
       =============================== */
    static toggleCheckoutPanel() {
        const panel = document.getElementById('checkout-panel');
        if (panel) panel.style.display = 'block';
    }

/* ---------------- BUILD PAYLOAD ---------------- */
    static buildCheckoutPayload() {
        return {
            shippingAddress: {
                firstName: document.getElementById('ship-firstName').value,
                lastName: document.getElementById('ship-lastName').value,
                email: document.getElementById('ship-email').value,
                phone: document.getElementById('ship-phone').value,
                addressLine1: document.getElementById('ship-address1').value,
                addressLine2: document.getElementById('ship-address2').value,
                city: document.getElementById('ship-city').value,
                state: document.getElementById('ship-state').value,
                postalCode: document.getElementById('ship-postalCode').value,
                country: document.getElementById('ship-country').value,
                addressType: "SHIPPING"
            },
            billingAddress: {
                firstName: document.getElementById('bill-firstName').value,
                lastName: document.getElementById('bill-lastName').value,
                email: document.getElementById('bill-email').value,
                phone: document.getElementById('bill-phone').value,
                addressLine1: document.getElementById('bill-address1').value,
                city: document.getElementById('bill-city').value,
                state: document.getElementById('bill-state').value,
                postalCode: document.getElementById('bill-postalCode').value,
                country: document.getElementById('bill-country').value,
                addressType: "BILLING"
            },
            paymentMethod: document.getElementById('payment-method').value
        };
    }

    /* ===============================
       PAYU PAYMENT
       =============================== */
    static async startPayment() {
        try {
            const payload = this.buildCheckoutPayload();

            const order = await createOrderFromCart(payload);

            const totalText = document.getElementById("total").textContent;
            const amount = totalText.replace(/[₹$,]/g, "").trim();

            const token = this.getAuthToken();
            if (!token) return;

            const response = await fetch(API_CONFIG.getFullUrl('PAYMENT', API_CONFIG.ENDPOINTS.PAYMENT), {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({
                    orderId: order.id,
                    firstname: document.getElementById("bill-firstName").value,
                    email: document.getElementById("bill-email").value,
                    amount: amount
                })
            });

            if (!response.ok) {
                const text = await response.text();
                console.error("Payment create failed:", text);
                throw new Error("Payment initiation failed");
            }

            const data = await response.json();

            await fetch(API_CONFIG.getFullUrl('ORDER', API_CONFIG.ENDPOINTS.PAYMENT_INIT), {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`
                },
                body: JSON.stringify({
                    orderId: order.id,
                    orderNumber: order.orderNumber,
                    paymentId: data.paymentId,
                    status: "INITIATED",
                    amount: data.amount
                })
            });

            const form = document.getElementById("payuForm");
            form.action = data.payuUrl;

            form.innerHTML = `
                <input type="hidden" name="key" value="${data.key}">
                <input type="hidden" name="txnid" value="${data.txnId}">
                <input type="hidden" name="amount" value="${data.amount}">
                <input type="hidden" name="productinfo" value="${data.productInfo}">
                <input type="hidden" name="firstname" value="${data.firstname}">
                <input type="hidden" name="email" value="${data.email}">
                <input type="hidden" name="hash" value="${data.hash}">
                <input type="hidden" name="surl" value="${data.surl}">
                <input type="hidden" name="furl" value="${data.furl}">
                <input type="hidden" name="udf1" value="${order.id}">
                <input type="hidden" name="udf2" value="${data.paymentId}">
            `;

            form.submit();

        } catch (e) {
            console.error(e);
            showMessage('Payment initiation failed', 'error');
        }
    }
}

export { Cart };
